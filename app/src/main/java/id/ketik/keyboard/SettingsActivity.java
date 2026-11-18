package id.ketik.keyboard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Typeface;
import android.app.NotificationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Setup steps and all keyboard settings, built in code (no XML layouts). */
public final class SettingsActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private Button enableBtn, pickBtn;
    private Switch shotSwitch, smsSwitch;
    private RadioGroup fontGroup;
    private LinearLayout toolbarList;

    private java.util.List<String> toolbarOrder() {
        String saved = Prefs.get(this).getString(Prefs.TOOLBAR, ToolbarView.DEFAULT_ORDER);
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String p : (saved + "," + ToolbarView.DEFAULT_ORDER).split(",")) {
            String name = p.startsWith("!") ? p.substring(1) : p;
            if (name.isEmpty() || !java.util.Arrays.asList(ToolbarView.TOOL_NAMES).contains(name) || !seen.add(name)) continue;
            out.add(p);
        }
        return out;
    }

    private void saveToolbarOrder(java.util.List<String> order) {
        Prefs.get(this).edit().putString(Prefs.TOOLBAR, android.text.TextUtils.join(",", order)).apply();
        buildToolbarList();
    }

    private void buildToolbarList() {
        toolbarList.removeAllViews();
        final java.util.List<String> order = toolbarOrder();
        for (int i = 0; i < order.size(); i++) {
            final int idx = i;
            final String entry = order.get(i);
            final boolean hidden = entry.startsWith("!");
            final String name = hidden ? entry.substring(1) : entry;
            String label = name;
            for (int k = 0; k < ToolbarView.TOOL_NAMES.length; k++) {
                if (ToolbarView.TOOL_NAMES[k].equals(name)) label = ToolbarView.TOOL_LABELS[k];
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            Switch sw = new Switch(this);
            sw.setText((i + 1) + ".  " + label);
            sw.setTextSize(15);
            sw.setChecked(!hidden);
            sw.setOnCheckedChangeListener((v, on) -> {
                order.set(idx, on ? name : "!" + name);
                saveToolbarOrder(order);
            });
            row.addView(sw, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button up = new Button(this);
            up.setText("↑");
            up.setEnabled(i > 0);
            up.setOnClickListener(v -> {
                java.util.Collections.swap(order, idx, idx - 1);
                saveToolbarOrder(order);
            });
            Button down = new Button(this);
            down.setText("↓");
            down.setEnabled(i < order.size() - 1);
            down.setOnClickListener(v -> {
                java.util.Collections.swap(order, idx, idx + 1);
                saveToolbarOrder(order);
            });
            row.addView(up, new LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(down, new LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT));
            toolbarList.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
    private RadioButton customFontButton;

    private static final int REQ_EXPORT = 10;

    private void exportTo(android.net.Uri uri) {
        new Thread(() -> {
            String msg;
            try (java.io.OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new java.io.IOException("no output");
                DataTransfer.export(this, out);
                msg = "Exported. Open the new Ketik app and choose \"Import from a backup file\".";
            } catch (Exception e) {
                msg = "Export failed: " + e.getMessage();
            }
            final String m = msg;
            runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) exportTo(data.getData());
            return;
        }
        if (requestCode != 9 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        java.io.File tmp = new java.io.File(getFilesDir(), "font_import.tmp");
        try (java.io.InputStream in = getContentResolver().openInputStream(data.getData());
             java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) {
            if (in == null) throw new IllegalStateException();
            byte[] buf = new byte[64 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 30L * 1024 * 1024) throw new IllegalStateException("too big");
                out.write(buf, 0, n);
            }
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            Toast.makeText(this, "Couldn't read that file", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Fonts.isValidFont(tmp)) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            Toast.makeText(this, "That file isn't a font Android can use (.ttf or .otf)", Toast.LENGTH_LONG).show();
            return;
        }
        java.io.File dest = Fonts.customFile(this);
        //noinspection ResultOfMethodCallIgnored
        dest.delete();
        if (!tmp.renameTo(dest)) {
            Toast.makeText(this, "Couldn't save the font", Toast.LENGTH_LONG).show();
            return;
        }
        Prefs.get(this).edit().putString(Prefs.FONT, Fonts.CUSTOM).apply();
        customFontButton.setEnabled(true);
        customFontButton.setChecked(true);
        Toast.makeText(this, "Font imported. Tap the test box to see it.", Toast.LENGTH_SHORT).show();
    }
    private TextView otpStatus;
    private Button otpBtn;

    private String photoPermission() {
        return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasPhotoAccess() {
        return checkSelfPermission(photoPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(24), dp(20), dp(40));
        scroll.addView(list);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("Ketik Keyboard");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        list.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Works fully offline. This app has no internet permission, so nothing you type can leave your phone.");
        sub.setTextSize(14);
        sub.setPadding(0, dp(6), 0, dp(10));
        list.addView(sub);

        section("Moving to the new Ketik");
        TextView moveInfo = new TextView(this);
        moveInfo.setText("Ketik Keyboard now continues as a new app. Install it, open its settings and tap "
                + "\"Import from old Ketik\" to bring over your settings, learned words, clipboard and emoji. "
                + "If that doesn't work, export a file here and import it there.");
        moveInfo.setTextSize(14);
        moveInfo.setPadding(0, 0, 0, dp(4));
        list.addView(moveInfo);
        button("Export my data to a file", v -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, DataTransfer.FILE_NAME);
            startActivityForResult(i, REQ_EXPORT);
        });

        section("Setup");
        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, 0, 0, dp(8));
        list.addView(status);
        enableBtn = button("1. Turn on Ketik Keyboard", v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        pickBtn = button("2. Choose Ketik as your keyboard", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });
        EditText test = new EditText(this);
        test.setHint("Tap here to try the keyboard");
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        list.addView(test, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        testBox("Numbers only (number pad)", InputType.TYPE_CLASS_NUMBER);
        testBox("Phone number (phone pad)", InputType.TYPE_CLASS_PHONE);
        testBox("Email address (@ and .com keys)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        testBox("Website address (/ key, long-press . for .com)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        testBox("Password (no suggestions, nothing learned)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        section("Layout");
        toggle("Number row", "Show a row of numbers above the letters", Prefs.NUMBER_ROW);
        toggle("Symbol hints", "Show symbols on keys; long-press to type them", Prefs.HINTS);
        toggle("Key preview", "Show a bubble when you press a key", Prefs.KEY_PREVIEW);

        section("Smart typing");
        toggle("Predictive text", "Word suggestions in the toolbar", Prefs.PREDICTIONS);
        toggle("Auto-correct", "Fix typos when you press space (backspace undoes it)", Prefs.AUTO_CORRECT);
        toggle("Next-word suggestions", "Learn which words you type after each other", Prefs.NEXT_WORD);
        toggle("Auto-capitalise", "Capital letter at the start of sentences", Prefs.AUTO_CAPS);
        toggle("Double-space full stop", "Tap space twice to type \". \"", Prefs.DOUBLE_SPACE);
        toggle("Swipe to type", "Slide your finger across letters to type a word", Prefs.SWIPE);
        toggle("Spacebar cursor control", "Slide along the spacebar to move the cursor", Prefs.SPACE_CURSOR);

        section("Feedback");
        toggle("Vibrate on keypress", null, Prefs.VIBRATE);
        toggle("Stronger vibration", null, Prefs.VIBRATE_STRONG);
        toggle("Sound on keypress", null, Prefs.SOUND);

        section("Top row");
        TextView trInfo = new TextView(this);
        trInfo.setText("Order of the icons above the keys (left to right). The hide-keyboard arrow always stays on the right.");
        trInfo.setTextSize(13);
        list.addView(trInfo);
        toolbarList = new LinearLayout(this);
        toolbarList.setOrientation(LinearLayout.VERTICAL);
        list.addView(toolbarList);
        buildToolbarList();

        section("Look");
        toggle("Animations", "Smooth transitions when opening panels and switching one-handed mode", Prefs.ANIMATIONS);

        TextView fontLabel = new TextView(this);
        fontLabel.setText("Key font");
        fontLabel.setTextSize(15);
        fontLabel.setPadding(0, dp(12), 0, 0);
        list.addView(fontLabel);
        fontGroup = new RadioGroup(this);
        String[][] fonts = {
                {Fonts.INTER, "Inter (closest to the Galaxy look)"},
                {Fonts.SYSTEM, "Phone default"},
                {Fonts.NUNITO, "Nunito Sans (softer, rounder)"},
                {Fonts.CUSTOM, "My own font file"}};
        for (int i = 0; i < fonts.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(fonts[i][1]);
            rb.setId(2000 + i);
            rb.setTag(fonts[i][0]);
            fontGroup.addView(rb);
            if (fonts[i][0].equals(Fonts.choice(this))) rb.setChecked(true);
        }
        customFontButton = (RadioButton) fontGroup.getChildAt(3);
        customFontButton.setEnabled(Fonts.customFile(this).isFile());
        fontGroup.setOnCheckedChangeListener((g, id) -> {
            RadioButton rb = g.findViewById(id);
            if (rb != null) Prefs.get(this).edit().putString(Prefs.FONT, (String) rb.getTag()).apply();
        });
        list.addView(fontGroup);
        button("Import a font file (.ttf / .otf)", v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, 9);
        });
        RadioGroup rg = new RadioGroup(this);
        String[] names = {"Follow system", "Light", "Dark"};
        int cur = Prefs.integer(this, Prefs.THEME);
        for (int i = 0; i < 3; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(names[i]);
            rb.setId(1000 + i);
            rg.addView(rb);
        }
        rg.check(1000 + cur);
        rg.setOnCheckedChangeListener((g, id) -> Prefs.put(this, Prefs.THEME, id - 1000));
        list.addView(rg);

        final TextView hLabel = new TextView(this);
        hLabel.setTextSize(15);
        hLabel.setPadding(0, dp(12), 0, 0);
        list.addView(hLabel);
        SeekBar height = new SeekBar(this);
        height.setMax(70); // 70%..140%
        int hp = Prefs.integer(this, Prefs.HEIGHT);
        height.setProgress(hp - 70);
        hLabel.setText("Keyboard height: " + hp + "%");
        height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                hLabel.setText("Keyboard height: " + (p + 70) + "%");
                if (user) Prefs.put(SettingsActivity.this, Prefs.HEIGHT, p + 70);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        list.addView(height);

        final TextView pLabel = new TextView(this);
        pLabel.setTextSize(15);
        pLabel.setPadding(0, dp(12), 0, 0);
        list.addView(pLabel);
        SeekBar prev = new SeekBar(this);
        prev.setMax(100); // 80%..180% of a key's height
        int pp = Prefs.integer(this, Prefs.PREVIEW_HEIGHT);
        prev.setProgress(pp - 80);
        pLabel.setText("Key preview height (how far it rises): " + pp + "%");
        prev.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                pLabel.setText("Key preview height (how far it rises): " + (p + 80) + "%");
                if (user) Prefs.put(SettingsActivity.this, Prefs.PREVIEW_HEIGHT, p + 80);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        list.addView(prev);

        final TextView wLabel = new TextView(this);
        wLabel.setTextSize(15);
        wLabel.setPadding(0, dp(12), 0, 0);
        list.addView(wLabel);
        SeekBar prevW = new SeekBar(this);
        prevW.setMax(70); // 100%..170% of the key's width
        int pw = Prefs.integer(this, Prefs.PREVIEW_WIDTH);
        prevW.setProgress(pw - 100);
        wLabel.setText("Key preview width: " + pw + "%");
        prevW.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                wLabel.setText("Key preview width: " + (p + 100) + "%");
                if (user) Prefs.put(SettingsActivity.this, Prefs.PREVIEW_WIDTH, p + 100);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        list.addView(prevW);

        final TextView kLabel = new TextView(this);
        kLabel.setTextSize(15);
        kLabel.setPadding(0, dp(12), 0, 0);
        list.addView(kLabel);
        SeekBar keySize = new SeekBar(this);
        keySize.setMax(40); // 60%..100% of the row
        int kp = Prefs.integer(this, Prefs.KEY_SIZE);
        keySize.setProgress(kp - 60);
        kLabel.setText("Key height: " + kp + "%");
        keySize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                kLabel.setText("Key height: " + (p + 60) + "%");
                if (user) Prefs.put(SettingsActivity.this, Prefs.KEY_SIZE, p + 60);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        list.addView(keySize);

        final TextView fLabel = new TextView(this);
        fLabel.setTextSize(15);
        fLabel.setPadding(0, dp(12), 0, 0);
        list.addView(fLabel);
        SeekBar font = new SeekBar(this);
        font.setMax(60); // 70%..130%
        int fp = Prefs.integer(this, Prefs.FONT_SIZE);
        font.setProgress(fp - 70);
        fLabel.setText("Key text size: " + fp + "%");
        font.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                fLabel.setText("Key text size: " + (p + 70) + "%");
                if (user) Prefs.put(SettingsActivity.this, Prefs.FONT_SIZE, p + 70);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        list.addView(font);
        TextView sizeTip = new TextView(this);
        sizeTip.setText("Keyboard height = the whole keyboard. Key height = how tall each key is inside it "
                + "(the gaps between rows get bigger). Try them in the test box at the top.");
        sizeTip.setTextSize(13);
        list.addView(sizeTip);

        section("Clipboard");
        toggle("Suggest recently copied text", "Show a paste chip in the toolbar after you copy", Prefs.CLIP_SUGGEST);
        shotSwitch = new Switch(this);
        shotSwitch.setText("Add screenshots to clipboard\nNeeds photo access: choose \"Allow all\". Images stay on this phone.");
        shotSwitch.setTextSize(15);
        shotSwitch.setPadding(0, dp(8), 0, dp(8));
        shotSwitch.setChecked(Prefs.bool(this, Prefs.SCREENSHOTS) && hasPhotoAccess());
        shotSwitch.setOnCheckedChangeListener((v, on) -> {
            Prefs.put(this, Prefs.SCREENSHOTS, on);
            if (on && !hasPhotoAccess()) requestPermissions(new String[]{photoPermission()}, 7);
        });
        list.addView(shotSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView cLabel = new TextView(this);
        cLabel.setTextSize(15);
        list.addView(cLabel);
        SeekBar clipMax = new SeekBar(this);
        clipMax.setMax(90); // 10..100
        int cm = Prefs.integer(this, Prefs.CLIP_MAX);
        clipMax.setProgress(cm - 10);
        cLabel.setText("Keep last " + cm + " items (pinned items are always kept)");
        clipMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                cLabel.setText("Keep last " + (p + 10) + " items (pinned items are always kept)");
                if (user) Prefs.put(SettingsActivity.this, Prefs.CLIP_MAX, p + 10);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        list.addView(clipMax);
        button("Clear clipboard history", v -> confirm("Delete all clipboard items, including pinned ones?", () -> {
            ClipboardStore.get(this).clearAll();
            Toast.makeText(this, "Clipboard history cleared", Toast.LENGTH_SHORT).show();
        }));

        if (hasOtpFeature()) addOtpSection();

        section("Problem report");
        final TextView crash = new TextView(this);
        crash.setTextSize(13);
        crash.setTextIsSelectable(true);
        java.io.File cf = CrashLog.file(this);
        String report = null;
        if (cf.isFile()) {
            try {
                byte[] all = java.nio.file.Files.readAllBytes(cf.toPath());
                report = new String(all, java.nio.charset.StandardCharsets.UTF_8);
                if (report.length() > 6000) report = report.substring(report.length() - 6000);
            } catch (Exception ignored) {
            }
        }
        final String finalReport = report;
        crash.setText(report == null || report.trim().isEmpty()
                ? "No problems recorded."
                : "The keyboard recorded an error. Tap Copy and send it so it can be fixed "
                + "(it contains only the error, never anything you typed).");
        list.addView(crash);
        if (finalReport != null && !finalReport.trim().isEmpty()) {
            button("Copy problem report", v -> {
                android.content.ClipboardManager clipMgr = getSystemService(android.content.ClipboardManager.class);
                if (clipMgr != null) clipMgr.setPrimaryClip(android.content.ClipData.newPlainText("Ketik problem report", finalReport));
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
            });
            button("Clear problem report", v -> {
                //noinspection ResultOfMethodCallIgnored
                CrashLog.file(this).delete();
                crash.setText("No problems recorded.");
            });
        }

        section("Privacy");
        button("Clear learned words", v -> confirm("Forget all words and word pairs the keyboard has learned?", () -> {
            Dictionary.get(this).clearLearned();
            Toast.makeText(this, "Learned words cleared", Toast.LENGTH_SHORT).show();
        }));
        TextView note = new TextView(this);
        note.setText("Nothing is learned or saved in password fields. Text that apps mark as sensitive "
                + "(for example passwords copied from a password manager) is never kept in clipboard history. "
                + "Clipboard history is never deleted by time: only the oldest unpinned items go once you pass "
                + "the limit above. The app has no internet permission, so none of this can leave your phone.");
        note.setTextSize(13);
        note.setPadding(0, dp(8), 0, 0);
        list.addView(note);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        if (shotSwitch != null && shotSwitch.isChecked() && !hasPhotoAccess()) shotSwitch.setChecked(false);
        if (smsSwitch != null && smsSwitch.isChecked() && !hasSms()) smsSwitch.setChecked(false);
        updateOtpStatus();
    }

    private boolean hasSms() {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private ComponentName listenerComponent() {
        return new ComponentName(this, OtpListenerService.class);
    }

    private boolean listenerEnabled() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        return nm != null && nm.isNotificationListenerAccessGranted(listenerComponent());
    }

    private void updateOtpStatus() {
        if (otpStatus == null) return;
        boolean on = listenerEnabled();
        otpStatus.setText(on ? "✓ Notification access is on." : "Notification access is off.");
        otpBtn.setText(on ? "Notification access settings" : "Allow notification access");
    }

    private void openListenerSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            i.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listenerComponent().flattenToString());
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 8) {
            if (!hasSms()) {
                Prefs.put(this, Prefs.OTP_SMS, false);
                smsSwitch.setChecked(false);
                Toast.makeText(this, "SMS permission was not granted", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode != 7) return;
        if (!hasPhotoAccess()) {
            Prefs.put(this, Prefs.SCREENSHOTS, false);
            shotSwitch.setChecked(false);
            Toast.makeText(this, "Screenshots need photo access set to \"Allow all\"", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "New screenshots will appear in your clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) updateStatus();
    }

    private void updateStatus() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        boolean enabled = false;
        String myId = null;
        if (imm != null) {
            for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
                if (info.getPackageName().equals(getPackageName())) {
                    enabled = true;
                    myId = info.getId();
                }
            }
        }
        String current = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = myId != null && myId.equals(current);
        if (selected) status.setText("✓ Ketik is on and is your current keyboard.");
        else if (enabled) status.setText("Step 1 done. Now do step 2.");
        else status.setText("Do these two steps once:");
        enableBtn.setEnabled(!enabled);
        pickBtn.setEnabled(enabled && !selected);
    }

    private void confirm(String msg, Runnable action) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("Delete", (d, w) -> action.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void section(String name) {
        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFF2F6BE4);
        t.setPadding(0, dp(24), 0, dp(6));
        list.addView(t);
    }

    private Button button(String text, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        btn.setOnClickListener(l);
        list.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return btn;
    }

    private void toggle(String title, String summary, final String key) {
        Switch s = new Switch(this);
        s.setText(summary == null ? title : title + "\n" + summary);
        s.setTextSize(15);
        s.setPadding(0, dp(8), 0, dp(8));
        s.setChecked(Prefs.bool(this, key));
        s.setOnCheckedChangeListener((v, on) -> Prefs.put(this, key, on));
        list.addView(s, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private boolean hasOtpFeature() {
        try {
            getPackageManager().getServiceInfo(listenerComponent(), 0);
            return true;
        } catch (Exception e) {
            return false; // lite edition
        }
    }

    private void addOtpSection() {
        section("One-time codes (OTP)");
        TextView otpInfo = new TextView(this);
        otpInfo.setText("When a code arrives by SMS, WhatsApp, email or a bank app, it appears in the "
                + "keyboard's top bar. Tap it to type it. The code is kept in memory only and is gone "
                + "after 5 minutes. Nothing is saved or sent.");
        otpInfo.setTextSize(14);
        list.addView(otpInfo);
        otpStatus = new TextView(this);
        otpStatus.setTextSize(14);
        otpStatus.setPadding(0, dp(8), 0, dp(4));
        list.addView(otpStatus);
        otpBtn = button("Allow notification access", v -> openListenerSettings());
        smsSwitch = new Switch(this);
        smsSwitch.setText("Also read codes from SMS directly\nBackup for when Android hides codes in notifications");
        smsSwitch.setTextSize(15);
        smsSwitch.setPadding(0, dp(8), 0, dp(8));
        smsSwitch.setChecked(Prefs.bool(this, Prefs.OTP_SMS) && hasSms());
        smsSwitch.setOnCheckedChangeListener((v, on) -> {
            Prefs.put(this, Prefs.OTP_SMS, on);
            if (on && !hasSms()) requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 8);
        });
        list.addView(smsSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        button("Test: send a sample code (482913)", v -> {
            OtpBus.post("482913", "Test");
            Toast.makeText(this, "Tap the test box at the top to see it", Toast.LENGTH_SHORT).show();
        });
        TextView otpNote = new TextView(this);
        otpNote.setText("If a switch is greyed out or blocked: open App info for Ketik Keyboard → ⋮ menu → "
                + "\"Allow restricted settings\", then try again. On HyperOS also turn on Autostart and set "
                + "Battery saver to \"No restrictions\" so codes keep arriving in the background.");
        otpNote.setTextSize(13);
        otpNote.setPadding(0, dp(6), 0, 0);
        list.addView(otpNote);

    }

    private void testBox(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(true);
        list.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
