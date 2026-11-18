package id.my.blankpoof.ketik;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Moves user data between the old app (id.ketik.keyboard) and the new one (id.my.blankpoof.ketik).
 * The same class lives in both apps. Data travels as a zip:
 *   ketik-backup.json           format marker
 *   prefs/&lt;name&gt;.json         one SharedPreferences file, values tagged with their type
 *   files/&lt;name&gt;               learned words, word pairs, imported font
 *   clips/&lt;name&gt;               clipboard images
 * The crash log is never included.
 */
final class DataTransfer {
    static final String OLD_PACKAGE = "id.ketik.keyboard";
    static final String NEW_PACKAGE = "id.my.blankpoof.ketik";
    static final String AUTHORITY = "id.ketik.keyboard.export";
    static final Uri EXPORT_URI = Uri.parse("content://" + AUTHORITY + "/ketik-backup.zip");
    static final String FILE_NAME = "ketik-backup.zip";

    private static final String META = "ketik-backup.json";
    private static final String[] PREFS = {"ketik_settings", "ketik_clipboard", "ketik_emoji"};
    private static final String[] FILES = {"user_words.tsv", "user_bigrams.tsv", "custom_font"};
    private static final String CLIPS = "clips";
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final long MAX_TOTAL = 300L * 1024 * 1024;

    private DataTransfer() {}

    // ------------------------------------------------------------------ export

    static void export(Context c, OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(out));
        try {
            JSONObject meta = new JSONObject();
            meta.put("format", 1);
            meta.put("from", c.getPackageName());
            putText(zip, META, meta.toString());
            for (String name : PREFS) {
                putText(zip, "prefs/" + name + ".json", prefsToJson(c.getSharedPreferences(name, Context.MODE_PRIVATE)).toString());
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
        for (String name : FILES) {
            File f = new File(c.getFilesDir(), name);
            if (f.isFile()) putFile(zip, "files/" + name, f);
        }
        File[] clips = new File(c.getFilesDir(), CLIPS).listFiles();
        if (clips != null) {
            for (File f : clips) {
                if (f.isFile() && SAFE_NAME.matcher(f.getName()).matches() && !f.getName().endsWith(".tmp")) {
                    putFile(zip, CLIPS + "/" + f.getName(), f);
                }
            }
        }
        zip.finish();
        zip.flush();
    }

    private static JSONObject prefsToJson(SharedPreferences sp) throws JSONException {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            JSONObject t = new JSONObject();
            if (v instanceof Boolean) t.put("t", "b");
            else if (v instanceof Integer) t.put("t", "i");
            else if (v instanceof Long) t.put("t", "l");
            else if (v instanceof Float) t.put("t", "f");
            else if (v instanceof String) t.put("t", "s");
            else if (v instanceof Set) {
                t.put("t", "set");
                JSONArray a = new JSONArray();
                for (Object s : (Set<?>) v) a.put(String.valueOf(s));
                t.put("v", a);
                o.put(e.getKey(), t);
                continue;
            } else continue;
            t.put("v", v instanceof Float ? ((Float) v).doubleValue() : v);
            o.put(e.getKey(), t);
        }
        return o;
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void putFile(ZipOutputStream zip, String name, File f) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream in = new FileInputStream(f)) {
            copy(in, zip, Long.MAX_VALUE);
        }
        zip.closeEntry();
    }

    // ------------------------------------------------------------------ import

    /**
     * Replaces this app's data with the backup's. Everything is unpacked and checked first, so a
     * broken or foreign file changes nothing. Afterwards the process must restart, because the
     * keyboard keeps settings, words and clipboard in memory.
     */
    static void importFrom(Context c, InputStream in) throws IOException {
        // Unpack next to the real files (not in the cache dir), so moved files stay owned as app data.
        File stage = new File(c.getFilesDir(), ".import");
        deleteTree(stage);
        if (!stage.mkdirs()) throw new IOException("Couldn't prepare the import");
        try {
            boolean sawMeta = false;
            long total = 0;
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String target = allowedPath(e.getName());
                if (target == null) continue; // ignore anything we don't know
                File out = new File(stage, target);
                //noinspection ResultOfMethodCallIgnored
                out.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    total += copy(zip, os, MAX_TOTAL - total);
                }
                if (total >= MAX_TOTAL) throw new IOException("The backup is too big");
                if (META.equals(target)) sawMeta = true;
            }
            if (!sawMeta) throw new IOException("This isn't a Ketik backup file");
            JSONObject meta = new JSONObject(readText(new File(stage, META)));
            if (meta.optInt("format", 0) != 1) throw new IOException("This backup is from a newer Ketik version");

            // Parse all prefs before touching anything.
            JSONObject[] prefs = new JSONObject[PREFS.length];
            for (int i = 0; i < PREFS.length; i++) {
                File f = new File(stage, "prefs/" + PREFS[i] + ".json");
                if (f.isFile()) prefs[i] = new JSONObject(readText(f));
            }

            for (int i = 0; i < PREFS.length; i++) {
                if (prefs[i] != null) writePrefs(c.getSharedPreferences(PREFS[i], Context.MODE_PRIVATE), prefs[i]);
            }
            for (String name : FILES) {
                File src = new File(stage, "files/" + name);
                File dst = new File(c.getFilesDir(), name);
                if (src.isFile()) moveOver(src, dst);
            }
            File clipSrc = new File(stage, CLIPS);
            File clipDst = new File(c.getFilesDir(), CLIPS);
            if (prefs[1] != null) { // clipboard list came with the backup: its images replace ours
                deleteTree(clipDst);
                //noinspection ResultOfMethodCallIgnored
                clipDst.mkdirs();
                File[] imgs = clipSrc.listFiles();
                if (imgs != null) for (File f : imgs) moveOver(f, new File(clipDst, f.getName()));
            }
        } catch (JSONException ex) {
            throw new IOException("The backup file is damaged", ex);
        } finally {
            deleteTree(stage);
        }
    }

    /** @return where an entry may be unpacked, or null if it isn't part of a Ketik backup. */
    private static String allowedPath(String name) {
        if (META.equals(name)) return name;
        for (String p : PREFS) if (name.equals("prefs/" + p + ".json")) return name;
        for (String f : FILES) if (name.equals("files/" + f)) return name;
        if (name.startsWith(CLIPS + "/")) {
            String rest = name.substring(CLIPS.length() + 1);
            if (SAFE_NAME.matcher(rest).matches() && !rest.startsWith(".")) return name;
        }
        return null;
    }

    private static void writePrefs(SharedPreferences sp, JSONObject o) throws JSONException {
        SharedPreferences.Editor ed = sp.edit().clear();
        Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            JSONObject t = o.getJSONObject(k);
            switch (t.getString("t")) {
                case "b": ed.putBoolean(k, t.getBoolean("v")); break;
                case "i": ed.putInt(k, t.getInt("v")); break;
                case "l": ed.putLong(k, t.getLong("v")); break;
                case "f": ed.putFloat(k, (float) t.getDouble("v")); break;
                case "s": ed.putString(k, t.getString("v")); break;
                case "set": {
                    JSONArray a = t.getJSONArray("v");
                    Set<String> s = new HashSet<>();
                    for (int j = 0; j < a.length(); j++) s.add(a.getString(j));
                    ed.putStringSet(k, s);
                    break;
                }
                default: break;
            }
        }
        ed.commit(); // synchronous: the process restarts right after
    }

    // ------------------------------------------------------------------ helpers

    private static long copy(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buf = new byte[64 * 1024];
        long n = 0;
        int r;
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
            n += r;
            if (n >= limit) break;
        }
        return n;
    }

    private static String readText(File f) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(f)) {
            copy(in, b, Long.MAX_VALUE);
        }
        return b.toString("UTF-8");
    }

    private static void moveOver(File src, File dst) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        dst.delete();
        if (src.renameTo(dst)) return;
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            copy(in, out, Long.MAX_VALUE);
        }
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
