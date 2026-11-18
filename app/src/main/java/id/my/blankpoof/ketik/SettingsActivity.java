package id.my.blankpoof.ketik;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Setup steps and all keyboard settings, built in code (no XML layouts).
 * Grouped into cards that follow the keyboard's own light/dark palette.
 */
public final class SettingsActivity extends Activity {
    private static final int REQ_PHOTOS = 7, REQ_SMS = 8, REQ_FONT = 9, REQ_IMPORT = 10;
    private static final int SCROLL_ID = 0x00ab0001;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    /** One line in a card: title, optional summary, and an optional control on the right. */
    private static final class Row {
        LinearLayout view;
        TextView title, summary, badge;
        View divider;
        Switch sw;
        RadioButton radio;
        String key;
    }

    private static final class Slider {
        SeekBar bar;
        String key;
        int min;
    }

    // Colours, taken from the keyboard theme so the app and keyboard match.
    private Theme theme;
    private int cBg, cSurface, cField, cText, cText2, cAccent, cAccentFill, cOnAccent;
    private int cDivider, cDanger, cSuccess, cRipple;

    private LinearLayout list;   // the page column
    private LinearLayout card;   // the card rows are currently added to

    private TextView status;
    private View statusDot;
    private Row step1, step2;
    private Row shotRow, smsRow, otpBtnRow, customFontRow;
    private TextView otpStatus;
    private View otpDot;
    private final List<Row> fontRows = new ArrayList<>();
    private final List<Slider> sizeSliders = new ArrayList<>();
    private LinearLayout toolbarList;

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // the page has its own header
        initColors();
        styleWindow();

        ScrollView scroll = new ScrollView(this);
        scroll.setId(SCROLL_ID);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(cBg);
        FrameLayout frame = new FrameLayout(this);
        list = new MaxWidthColumn(this, dp(640));
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(24), dp(16), dp(32));
        frame.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame, new ScrollView.LayoutParams(MATCH, WRAP));
        setContentView(scroll);

        header();
        setupSection();
        trySection();
        typingSection();
        keysSection();
        feedbackSection();
        appearanceSection();
        sizeSection();
        toolbarSection();
        clipboardSection();
        if (hasOtpFeature()) otpSection();
        importSection();
        privacySection();
        problemSection();
        footer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        if (shotRow != null && shotRow.sw.isChecked() && !hasPhotoAccess()) shotRow.sw.setChecked(false);
        if (smsRow != null && smsRow.sw.isChecked() && !hasSms()) smsRow.sw.setChecked(false);
        updateOtpStatus();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) updateStatus();
    }

    // ---------------------------------------------------------------- sections

    private void header() {
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        IconView logo = new IconView(this, Icons.KEYBOARD, cOnAccent, dp(26));
        logo.setBackground(oval(cAccentFill));
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        top.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout names = vbox();
        TextView title = text("Ketik Keyboard", 26, cText, true);
        title.setAccessibilityHeading(true);
        names.addView(title);
        String ver = versionName();
        if (ver != null) names.addView(text("Version " + ver, 14, cText2, false));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, WRAP, 1f);
        np.leftMargin = dp(16);
        top.addView(names, np);
        list.addView(top);

        TextView chip = text("Offline · no internet permission", 13, cSuccess, true);
        chip.setBackground(rounded((cSuccess & 0x00FFFFFF) | 0x24000000, dp(999), 0, 0));
        chip.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(WRAP, WRAP);
        cp.topMargin = dp(18);
        list.addView(chip, cp);
        TextView sub = text("Nothing you type, copy or screenshot can leave your phone.", 14, cText2, false);
        sub.setPadding(dp(2), dp(8), 0, 0);
        list.addView(sub);
    }

    private void setupSection() {
        section("Setup");
        LinearLayout sr = hbox();
        sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.setPadding(dp(16), dp(16), dp(16), dp(16));
        statusDot = new View(this);
        statusDot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams dp10 = new LinearLayout.LayoutParams(dp(10), dp(10));
        dp10.rightMargin = dp(12);
        sr.addView(statusDot, dp10);
        status = text("", 16, cText, true);
        sr.addView(status, new LinearLayout.LayoutParams(0, WRAP, 1f));
        addToCard(sr);

        step1 = stepRow(1, "Turn on Ketik Keyboard", "Opens Android's keyboard list",
                v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        step2 = stepRow(2, "Choose Ketik as your keyboard", "Switch from your current keyboard", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });
    }

    private void trySection() {
        section("Try it");
        LinearLayout box = vbox();
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.addView(field("Tap here to try the keyboard", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE, true));
        addToCard(box);

        final LinearLayout extra = vbox();
        extra.setPadding(dp(16), 0, dp(16), dp(16));
        extra.setVisibility(View.GONE);
        String[] hints = {
                "Numbers only (number pad)",
                "Phone number (phone pad)",
                "Email address (@ and .com keys)",
                "Website address (/ key, long-press . for .com)",
                "Password (no suggestions, nothing learned)"};
        int[] types = {
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_PHONE,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD};
        for (int i = 0; i < hints.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            if (i > 0) lp.topMargin = dp(8);
            extra.addView(field(hints[i], types[i], false), lp);
        }

        final Row more = row("More test fields", "Number pad, phone, email, web address, password");
        final IconView chev = new IconView(this, Icons.CHEVRON_UP, cText2, dp(18));
        chev.setRotation(180);
        chev.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        more.view.addView(chev, new LinearLayout.LayoutParams(dp(32), dp(32)));
        clickable(more, v -> {
            boolean open = extra.getVisibility() != View.VISIBLE;
            extra.setVisibility(open ? View.VISIBLE : View.GONE);
            chev.setRotation(open ? 0 : 180);
        });
        more.view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
                if (Build.VERSION.SDK_INT >= 30) {
                    info.setStateDescription(extra.getVisibility() == View.VISIBLE ? "Expanded" : "Collapsed");
                }
            }
        });
        card.addView(extra, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private void typingSection() {
        section("Smart typing");
        toggle("Predictive text", "Word suggestions in the toolbar", Prefs.PREDICTIONS);
        toggle("Auto-correct", "Fix typos when you press space. Backspace undoes it.", Prefs.AUTO_CORRECT);
        toggle("Next-word suggestions", "Learn which words you type after each other", Prefs.NEXT_WORD);
        toggle("Auto-capitalise", "Capital letter at the start of sentences", Prefs.AUTO_CAPS);
        toggle("Double-space full stop", "Tap space twice to type \". \"", Prefs.DOUBLE_SPACE);
        toggle("Swipe to type", "Slide your finger across letters to type a word", Prefs.SWIPE);
        toggle("Spacebar cursor control", "Slide along the spacebar to move the cursor", Prefs.SPACE_CURSOR);
    }

    private void keysSection() {
        section("Keys");
        toggle("Number row", "Show a row of numbers above the letters", Prefs.NUMBER_ROW);
        toggle("Symbol hints", "Show symbols on keys; long-press to type them", Prefs.HINTS);
        toggle("Key preview", "Show a bubble when you press a key", Prefs.KEY_PREVIEW);
    }

    private void feedbackSection() {
        section("Sound & vibration");
        final Row[] strong = new Row[1];
        Row vib = toggle("Vibrate on keypress", null, Prefs.bool(this, Prefs.VIBRATE), (b, on) -> {
            Prefs.put(this, Prefs.VIBRATE, on);
            if (strong[0] != null) setRowEnabled(strong[0], on);
        });
        strong[0] = toggle("Stronger vibration", "A firmer buzz for each key", Prefs.VIBRATE_STRONG);
        setRowEnabled(strong[0], vib.sw.isChecked());
        toggle("Sound on keypress", null, Prefs.SOUND);
    }

    private void appearanceSection() {
        section("Appearance");
        themePicker();
        toggle("Animations", "Smooth transitions when opening panels and switching one-handed mode", Prefs.ANIMATIONS);

        TextView fontLabel = text("Key font", 14, cText2, true);
        fontLabel.setPadding(dp(16), dp(16), dp(16), dp(4));
        fontLabel.setAccessibilityHeading(true);
        addToCard(fontLabel);
        String[][] fonts = {
                {Fonts.INTER, "Inter", "Closest to the Galaxy look"},
                {Fonts.SYSTEM, "Phone default", "Your phone's own system font"},
                {Fonts.NUNITO, "Nunito Sans", "Softer and rounder"},
                {Fonts.CUSTOM, "My own font", null}};
        String chosen = Fonts.choice(this);
        boolean hasCustom = Fonts.customFile(this).isFile();
        for (String[] f : fonts) {
            final String key = f[0];
            String summary = f[2] != null ? f[2] : (hasCustom ? "Imported font file" : "Import a font file first");
            Row r = row(f[1], summary);
            r.key = key;
            r.title.setTypeface(previewFace(key));
            RadioButton rb = new RadioButton(this);
            rb.setChecked(key.equals(chosen));
            rb.setClickable(false);
            rb.setFocusable(false);
            rb.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            rb.setButtonTintList(checkedTint(cAccent, cText2));
            r.view.addView(rb, new LinearLayout.LayoutParams(WRAP, WRAP));
            r.radio = rb;
            clickable(r, v -> selectFont(key));
            checkableA11y(r.view, RadioButton.class, rb);
            fontRows.add(r);
            if (Fonts.CUSTOM.equals(key)) customFontRow = r;
        }
        setRowEnabled(customFontRow, hasCustom);
        action("Import a font file", ".ttf or .otf, stays on this phone", cAccent, v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_FONT);
        });
    }

    private void sizeSection() {
        section("Size");
        sizeSliders.add(slider("Keyboard height", "The whole keyboard", Prefs.HEIGHT, 70, 140, "%"));
        sizeSliders.add(slider("Key height", "How tall each key is inside its row. Smaller keys leave bigger gaps.",
                Prefs.KEY_SIZE, 60, 100, "%"));
        sizeSliders.add(slider("Key text size", null, Prefs.FONT_SIZE, 70, 130, "%"));
        sizeSliders.add(slider("Key preview height", "How far the bubble rises above the key",
                Prefs.PREVIEW_HEIGHT, 80, 180, "%"));
        sizeSliders.add(slider("Key preview width", null, Prefs.PREVIEW_WIDTH, 100, 170, "%"));
        action("Reset sizes", "Back to the default sizes", cAccent, v -> {
            for (Slider s : sizeSliders) {
                int def = Prefs.defaultInt(s.key);
                s.bar.setProgress(def - s.min);
                Prefs.put(this, s.key, def);
            }
            Toast.makeText(this, "Sizes reset", Toast.LENGTH_SHORT).show();
        });
    }

    private void toolbarSection() {
        section("Top row");
        infoText("Icons above the keys, left to right. The hide-keyboard arrow always stays on the right.");
        toolbarList = vbox();
        addToCard(toolbarList);
        buildToolbarList();
    }

    private void clipboardSection() {
        section("Clipboard");
        toggle("Suggest recently copied text", "Show a paste chip in the toolbar after you copy", Prefs.CLIP_SUGGEST);
        shotRow = toggle("Add screenshots to clipboard",
                "Needs photo access set to \"Allow all\". Images stay on this phone.",
                Prefs.bool(this, Prefs.SCREENSHOTS) && hasPhotoAccess(), (v, on) -> {
                    Prefs.put(this, Prefs.SCREENSHOTS, on);
                    if (on && !hasPhotoAccess()) requestPermissions(new String[]{photoPermission()}, REQ_PHOTOS);
                });
        slider("History size", "Pinned items are always kept", Prefs.CLIP_MAX, 10, 100, " items");
        action("Clear clipboard history", "Deletes every item, including pinned ones", cDanger, v ->
                confirm("Clear clipboard history?", "All clipboard items will be deleted, including pinned ones.", () -> {
                    ClipboardStore.get(this).clearAll();
                    Toast.makeText(this, "Clipboard history cleared", Toast.LENGTH_SHORT).show();
                }));
    }

    private void otpSection() {
        section("One-time codes (OTP)");
        infoText("When a code arrives by SMS, WhatsApp, email or a bank app, it appears in the keyboard's "
                + "top bar. Tap it to type it. The code is kept in memory only and is gone after 5 minutes.");
        LinearLayout sr = hbox();
        sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.setPadding(dp(16), dp(14), dp(16), dp(14));
        otpDot = new View(this);
        otpDot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(10), dp(10));
        dl.rightMargin = dp(12);
        sr.addView(otpDot, dl);
        otpStatus = text("", 15, cText, true);
        sr.addView(otpStatus, new LinearLayout.LayoutParams(0, WRAP, 1f));
        addToCard(sr);
        otpBtnRow = action("Allow notification access", "Lets Ketik see codes in your notifications", cAccent,
                v -> openListenerSettings());
        smsRow = toggle("Also read codes from SMS", "Backup for when Android hides codes in notifications",
                Prefs.bool(this, Prefs.OTP_SMS) && hasSms(), (v, on) -> {
                    Prefs.put(this, Prefs.OTP_SMS, on);
                    if (on && !hasSms()) requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, REQ_SMS);
                });
        toggle("Mark as read after filling",
                "Press the message's \"Mark as read\" button (if it has one) and clear the notification",
                Prefs.OTP_MARK_READ);
        action("Send a test code", "Shows 482913 in the keyboard's top bar", cAccent, v -> {
            OtpBus.post("482913", "Test");
            Toast.makeText(this, "Tap the test box at the top to see it", Toast.LENGTH_SHORT).show();
        });
        infoText("Switch greyed out or blocked? Open App info for Ketik Keyboard → ⋮ menu → "
                + "\"Allow restricted settings\", then try again. On HyperOS also turn on Autostart and set "
                + "Battery saver to \"No restrictions\" so codes keep arriving in the background.");
    }

    private void importSection() {
        section("Move from the old Ketik");
        final boolean oldInstalled = isInstalled(DataTransfer.OLD_PACKAGE);
        Row fromApp = action("Import from old Ketik",
                oldInstalled ? "Copies settings, learned words, clipboard and emoji from the old app"
                        : "The old Ketik app isn't installed on this phone",
                cAccent, v -> confirmImport(() -> runImport(null)));
        setRowEnabled(fromApp, oldInstalled);
        action("Import from a backup file", "A " + DataTransfer.FILE_NAME + " exported from the old app",
                cAccent, v -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, REQ_IMPORT);
                });
        infoText("Importing replaces this app's settings, learned words, clipboard and emoji history. "
                + "Ketik restarts when it's done. Afterwards you can uninstall the old app.");
    }

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void confirmImport(Runnable go) {
        int style = theme.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        new AlertDialog.Builder(this, style)
                .setTitle("Replace data in this app?")
                .setMessage("Your current settings, learned words, clipboard and emoji history here will be "
                        + "replaced by the imported ones.")
                .setPositiveButton("Import", (d, w) -> go.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** @param file a backup file to read, or null to ask the old app directly. */
    private void runImport(final android.net.Uri file) {
        Toast.makeText(this, "Importing…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String error = null;
            try (java.io.InputStream in = getContentResolver().openInputStream(
                    file != null ? file : DataTransfer.EXPORT_URI)) {
                if (in == null) throw new java.io.FileNotFoundException();
                DataTransfer.importFrom(this, in);
            } catch (SecurityException | java.io.FileNotFoundException e) {
                error = file != null ? "Couldn't open that file."
                        : "The old app can't share its data yet. Update the old Ketik to version 1.31 "
                        + "or newer, or use \"Export my data to a file\" there and import that file here.";
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : "Import failed.";
            }
            final String err = error;
            runOnUiThread(() -> {
                if (err != null) {
                    int style = theme.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                            : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
                    new AlertDialog.Builder(this, style).setTitle("Import didn't work")
                            .setMessage(err).setPositiveButton("OK", null).show();
                } else {
                    Toast.makeText(this, "Imported. Restarting Ketik…", Toast.LENGTH_LONG).show();
                    restartApp();
                }
            });
        }).start();
    }

    /** Settings, words and clipboard are cached in memory by the keyboard, so start fresh. */
    private void restartApp() {
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void privacySection() {
        section("Privacy");
        action("Clear learned words", "Forget every word and word pair the keyboard has learned", cDanger, v ->
                confirm("Clear learned words?", "The keyboard will forget all words and word pairs it has learned.", () -> {
                    Dictionary.get(this).clearLearned();
                    Toast.makeText(this, "Learned words cleared", Toast.LENGTH_SHORT).show();
                }));
        infoText("Nothing is learned or saved in password fields. Text that apps mark as sensitive "
                + "(for example passwords copied from a password manager) is never kept in clipboard history. "
                + "Clipboard history is never deleted by time: only the oldest unpinned items go once you pass "
                + "the history size. The app has no internet permission, so none of this can leave your phone.");
    }

    private void problemSection() {
        section("Problem report");
        String report = null;
        java.io.File cf = CrashLog.file(this);
        if (cf.isFile()) {
            try {
                byte[] all = java.nio.file.Files.readAllBytes(cf.toPath());
                report = new String(all, java.nio.charset.StandardCharsets.UTF_8);
                if (report.length() > 6000) report = report.substring(report.length() - 6000);
            } catch (Exception ignored) {
            }
        }
        final boolean has = report != null && !report.trim().isEmpty();
        final TextView crash = infoText(has
                ? "The keyboard recorded an error. Copy the report and send it so it can be fixed. "
                + "It contains only the error, never anything you typed."
                : "No problems recorded.");
        crash.setTextColor(cText);
        if (!has) return;
        final String finalReport = report;
        final Row copy = action("Copy problem report", null, cAccent, v -> {
            android.content.ClipboardManager cm = getSystemService(android.content.ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("Ketik problem report", finalReport));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
        final Row[] clear = new Row[1];
        clear[0] = action("Clear problem report", null, cDanger, v -> {
            //noinspection ResultOfMethodCallIgnored
            CrashLog.file(this).delete();
            crash.setText("No problems recorded.");
            setVisible(copy, false);
            setVisible(clear[0], false);
        });
    }

    private void footer() {
        String ver = versionName();
        TextView f = text("Ketik Keyboard" + (ver != null ? " " + ver : "") + "\nMade to work fully offline",
                13, cText2, false);
        f.setGravity(Gravity.CENTER);
        f.setPadding(0, dp(32), 0, 0);
        list.addView(f, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    // ---------------------------------------------------------------- setup status

    private void updateStatus() {
        if (status == null) return;
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

        if (selected) {
            status.setText("Ready. Ketik is your current keyboard.");
            statusDot.setBackground(oval(cSuccess));
        } else if (enabled) {
            status.setText("Almost there: do step 2");
            statusDot.setBackground(oval(cAccent));
        } else {
            status.setText("Two quick steps to start typing");
            statusDot.setBackground(oval(cText2));
        }
        setVisible(step1, !selected);
        setVisible(step2, !selected);
        setStepState(step1, 1, enabled, !enabled);
        setStepState(step2, 2, false, enabled);
    }

    private void setStepState(Row r, int n, boolean done, boolean actionable) {
        r.badge.setText(done ? "✓" : String.valueOf(n));
        r.badge.setBackground(oval(done ? cSuccess : (actionable ? cAccentFill : cText2)));
        r.badge.setTextColor(done && theme.dark ? 0xFF0E1A12 : cOnAccent);
        r.summary.setText(done ? "Done" : (n == 1 ? "Opens Android's keyboard list" : "Switch from your current keyboard"));
        r.view.setEnabled(actionable);
        r.view.setAlpha(done || actionable ? 1f : 0.5f);
    }

    // ---------------------------------------------------------------- toolbar order

    private List<String> toolbarOrder() {
        String saved = Prefs.get(this).getString(Prefs.TOOLBAR, ToolbarView.DEFAULT_ORDER);
        List<String> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String p : (saved + "," + ToolbarView.DEFAULT_ORDER).split(",")) {
            String name = p.startsWith("!") ? p.substring(1) : p;
            if (name.isEmpty() || !java.util.Arrays.asList(ToolbarView.TOOL_NAMES).contains(name) || !seen.add(name)) continue;
            out.add(p);
        }
        return out;
    }

    private void saveToolbarOrder(List<String> order) {
        Prefs.get(this).edit().putString(Prefs.TOOLBAR, android.text.TextUtils.join(",", order)).apply();
        buildToolbarList();
    }

    private void buildToolbarList() {
        toolbarList.removeAllViews();
        final List<String> order = toolbarOrder();
        for (int i = 0; i < order.size(); i++) {
            final int idx = i;
            final String entry = order.get(i);
            final boolean hidden = entry.startsWith("!");
            final String name = hidden ? entry.substring(1) : entry;
            String label = name;
            for (int k = 0; k < ToolbarView.TOOL_NAMES.length; k++) {
                if (ToolbarView.TOOL_NAMES[k].equals(name)) label = ToolbarView.TOOL_LABELS[k];
            }
            if (i > 0) toolbarList.addView(divider());

            LinearLayout v = hbox();
            v.setGravity(Gravity.CENTER_VERTICAL);
            v.setMinimumHeight(dp(56));
            v.setPadding(dp(16), dp(4), dp(12), dp(4));
            TextView num = text(String.valueOf(i + 1), 14, cText2, true);
            num.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            v.addView(num, new LinearLayout.LayoutParams(dp(24), WRAP));
            TextView t = text(label, 16, hidden ? cText2 : cText, false);
            v.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));

            v.addView(iconButton(Icons.ARROW_UP, "Move " + label + " earlier", i > 0, x -> {
                java.util.Collections.swap(order, idx, idx - 1);
                saveToolbarOrder(order);
            }));
            v.addView(iconButton(Icons.ARROW_DOWN, "Move " + label + " later", i < order.size() - 1, x -> {
                java.util.Collections.swap(order, idx, idx + 1);
                saveToolbarOrder(order);
            }));

            Switch sw = styledSwitch();
            sw.setChecked(!hidden);
            sw.setContentDescription("Show " + label);
            sw.setOnCheckedChangeListener((b, on) -> {
                order.set(idx, on ? name : "!" + name);
                saveToolbarOrder(order);
            });
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(WRAP, WRAP);
            sp.leftMargin = dp(4);
            v.addView(sw, sp);
            toolbarList.addView(v, new LinearLayout.LayoutParams(MATCH, WRAP));
        }
    }

    // ---------------------------------------------------------------- results

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                final android.net.Uri file = data.getData();
                confirmImport(() -> runImport(file));
            }
            return;
        }
        if (requestCode != REQ_FONT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
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
        setRowEnabled(customFontRow, true);
        customFontRow.summary.setText("Imported font file");
        customFontRow.title.setTypeface(previewFace(Fonts.CUSTOM));
        selectFont(Fonts.CUSTOM);
        Toast.makeText(this, "Font imported. Tap the test box to see it.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_SMS) {
            if (!hasSms()) {
                Prefs.put(this, Prefs.OTP_SMS, false);
                smsRow.sw.setChecked(false);
                Toast.makeText(this, "SMS permission was not granted", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode != REQ_PHOTOS) return;
        if (!hasPhotoAccess()) {
            Prefs.put(this, Prefs.SCREENSHOTS, false);
            shotRow.sw.setChecked(false);
            Toast.makeText(this, "Screenshots need photo access set to \"Allow all\"", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "New screenshots will appear in your clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- permissions / OTP

    private String photoPermission() {
        return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasPhotoAccess() {
        return checkSelfPermission(photoPermission()) == PackageManager.PERMISSION_GRANTED;
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

    private boolean hasOtpFeature() {
        try {
            getPackageManager().getServiceInfo(listenerComponent(), 0);
            return true;
        } catch (Exception e) {
            return false; // lite edition
        }
    }

    private void updateOtpStatus() {
        if (otpStatus == null) return;
        boolean on = listenerEnabled();
        otpStatus.setText(on ? "Notification access is on" : "Notification access is off");
        otpDot.setBackground(oval(on ? cSuccess : cText2));
        otpBtnRow.title.setText(on ? "Notification access settings" : "Allow notification access");
        otpBtnRow.summary.setText(on ? "Change or turn off access" : "Lets Ketik see codes in your notifications");
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

    // ---------------------------------------------------------------- building blocks

    private void initColors() {
        theme = Theme.of(this);
        boolean d = theme.dark;
        cBg = theme.background;
        cSurface = theme.card;
        cField = d ? 0xFF1B1D21 : 0xFFF3F4F6;
        cText = theme.text;
        cText2 = d ? 0xFFA9AEB6 : 0xFF5C626B;      // >= 4.5:1 on the card colour
        cAccent = d ? 0xFF7BA4F5 : 0xFF2F6BE4;     // accent used as text / control colour
        cAccentFill = d ? 0xFF3A6CCB : 0xFF2F6BE4; // accent used behind white text
        cOnAccent = 0xFFFFFFFF;
        cDivider = theme.divider;
        cDanger = d ? 0xFFFF8A80 : 0xFFC62828;
        cSuccess = d ? 0xFF81C995 : 0xFF1E7E34;
        cRipple = d ? 0x33FFFFFF : 0x1F000000;
    }

    /**
     * Targeting API 35+ makes the window edge-to-edge: bar colours are ignored and the page
     * background (drawn by the root view, which pads itself for the bars) shows through.
     * Only the bar icon colour needs setting so it stays readable on that background.
     */
    @SuppressWarnings("deprecation")
    private void styleWindow() {
        Window w = getWindow();
        w.getDecorView().setBackgroundColor(cBg);
        // No grey scrim behind 3-button navigation: the page colour shows through instead.
        w.setNavigationBarContrastEnforced(false);
        w.setStatusBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT < 35) {
            w.setStatusBarColor(cBg);
            w.setNavigationBarColor(cBg);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(theme.dark ? 0 : light, light);
            }
        } else {
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility();
            int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(theme.dark ? flags & ~light : flags | light);
        }
    }

    private void section(String name) {
        TextView h = text(name, 14, cAccent, true);
        h.setLetterSpacing(0.02f);
        h.setPadding(dp(4), dp(28), dp(4), dp(8));
        h.setAccessibilityHeading(true);
        list.addView(h);
        card = vbox();
        card.setBackground(rounded(cSurface, dp(16), 0, 0));
        card.setClipToOutline(true);
        list.addView(card, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    /** Adds a view to the current card, with a divider above it if it isn't the first. */
    private View addToCard(View v) {
        View div = null;
        if (card.getChildCount() > 0) {
            div = divider();
            card.addView(div);
        }
        card.addView(v, new LinearLayout.LayoutParams(MATCH, WRAP));
        return div;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(cDivider);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(0.75f)));
        lp.leftMargin = dp(16);
        d.setLayoutParams(lp);
        return d;
    }

    private Row row(String title, String summary) {
        Row r = new Row();
        LinearLayout v = hbox();
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setMinimumHeight(dp(56));
        v.setPadding(dp(16), dp(12), dp(12), dp(12));
        LinearLayout texts = vbox();
        r.title = text(title, 16, cText, false);
        texts.addView(r.title);
        r.summary = text(summary == null ? "" : summary, 14, cText2, false);
        r.summary.setPadding(0, dp(2), 0, 0);
        r.summary.setLineSpacing(dp(2), 1f);
        if (summary == null) r.summary.setVisibility(View.GONE);
        texts.addView(r.summary);
        v.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        r.view = v;
        r.divider = addToCard(v);
        return r;
    }

    private void clickable(Row r, View.OnClickListener l) {
        r.view.setBackground(ripple(null));
        r.view.setClickable(true);
        r.view.setFocusable(true);
        r.view.setOnClickListener(l);
    }

    private Row toggle(String title, String summary, String key) {
        return toggle(title, summary, Prefs.bool(this, key), (b, on) -> Prefs.put(this, key, on));
    }

    private Row toggle(String title, String summary, boolean checked, CompoundButton.OnCheckedChangeListener l) {
        Row r = row(title, summary);
        final Switch sw = styledSwitch();
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(l);
        sw.setClickable(false);
        sw.setFocusable(false);
        sw.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.leftMargin = dp(12);
        r.view.addView(sw, lp);
        r.sw = sw;
        clickable(r, v -> sw.toggle());
        checkableA11y(r.view, Switch.class, sw);
        return r;
    }

    private Row action(String title, String summary, int color, View.OnClickListener l) {
        Row r = row(title, summary);
        r.title.setTextColor(color);
        r.title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clickable(r, l);
        r.view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
        return r;
    }

    private Row stepRow(int n, String title, String summary, View.OnClickListener l) {
        Row r = row(title, summary);
        TextView badge = text(String.valueOf(n), 14, cOnAccent, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(oval(cAccentFill));
        badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(28), dp(28));
        bp.rightMargin = dp(16);
        r.view.addView(badge, 0, bp);
        r.badge = badge;
        IconView chev = new IconView(this, Icons.CHEVRON_RIGHT, cText2, dp(16));
        chev.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        r.view.addView(chev, new LinearLayout.LayoutParams(dp(28), dp(28)));
        clickable(r, l);
        return r;
    }

    private Slider slider(String title, String summary, final String key, final int min, int max, final String suffix) {
        LinearLayout v = vbox();
        v.setPadding(dp(16), dp(14), dp(16), dp(4));
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(title, 16, cText, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        final TextView val = text("", 15, cAccent, true);
        top.addView(val);
        v.addView(top);
        if (summary != null) {
            TextView s = text(summary, 14, cText2, false);
            s.setPadding(0, dp(2), 0, 0);
            v.addView(s);
        }
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        int cur = Math.max(min, Math.min(max, Prefs.integer(this, key)));
        bar.setProgress(cur - min);
        val.setText(cur + suffix);
        bar.setProgressTintList(ColorStateList.valueOf(cAccent));
        bar.setThumbTintList(ColorStateList.valueOf(cAccent));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(cText2));
        bar.setContentDescription(title);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                val.setText((p + min) + suffix);
                if (user) Prefs.put(SettingsActivity.this, key, p + min);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(MATCH, dp(48));
        bp.leftMargin = -dp(12);   // line the track up with the text above it
        bp.rightMargin = -dp(12);
        v.addView(bar, bp);
        addToCard(v);
        Slider s = new Slider();
        s.bar = bar;
        s.key = key;
        s.min = min;
        return s;
    }

    private void themePicker() {
        LinearLayout v = vbox();
        v.setPadding(dp(16), dp(14), dp(16), dp(16));
        v.addView(text("Theme", 16, cText, false));
        LinearLayout seg = hbox();
        seg.setBackground(rounded(cField, dp(12), Math.max(1, dp(0.75f)), cDivider));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        String[] names = {"System", "Light", "Dark"};
        final int cur = Prefs.integer(this, Prefs.THEME);
        for (int i = 0; i < names.length; i++) {
            final int mode = i;
            final boolean on = i == cur;
            TextView o = text(names[i], 15, on ? cOnAccent : cText, on);
            o.setGravity(Gravity.CENTER);
            o.setMinHeight(dp(44));
            o.setBackground(on ? rounded(cAccentFill, dp(10), 0, 0) : ripple(rounded(Color.WHITE, dp(10), 0, 0)));
            o.setClickable(true);
            o.setFocusable(true);
            o.setContentDescription(names[i] + " theme");
            o.setOnClickListener(x -> {
                if (mode == Prefs.integer(this, Prefs.THEME)) return;
                Prefs.put(this, Prefs.THEME, mode);
                recreate();
            });
            o.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(on);
                }
            });
            seg.addView(o, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(MATCH, WRAP);
        sp.topMargin = dp(10);
        v.addView(seg, sp);
        addToCard(v);
    }

    private void selectFont(String key) {
        Prefs.get(this).edit().putString(Prefs.FONT, key).apply();
        for (Row r : fontRows) r.radio.setChecked(key.equals(r.key));
    }

    private Typeface previewFace(String key) {
        Typeface t = null;
        try {
            switch (key) {
                case Fonts.INTER:
                    t = new Typeface.Builder(getAssets(), "fonts/Inter.ttf")
                            .setFontVariationSettings("'wght' 400").build();
                    break;
                case Fonts.NUNITO:
                    t = new Typeface.Builder(getAssets(), "fonts/NunitoSans.ttf")
                            .setFontVariationSettings("'wght' 400").build();
                    break;
                case Fonts.CUSTOM:
                    java.io.File f = Fonts.customFile(this);
                    if (f.isFile()) t = new Typeface.Builder(f).build();
                    break;
                default:
                    break;
            }
        } catch (Exception ignored) {
        }
        return t != null ? t : Typeface.DEFAULT;
    }

    private TextView infoText(String s) {
        TextView t = text(s, 14, cText2, false);
        t.setLineSpacing(dp(2), 1f);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        addToCard(t);
        return t;
    }

    private EditText field(String hint, int inputType, boolean multiLine) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        if (!multiLine) e.setSingleLine(true);
        e.setTextSize(16);
        e.setTextColor(cText);
        e.setHintTextColor(cText2);
        e.setMinHeight(dp(multiLine ? 56 : 48));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, rounded(cField, dp(12), dp(2), cAccent));
        bg.addState(new int[]{}, rounded(cField, dp(12), Math.max(1, dp(0.75f)), cDivider));
        e.setBackground(bg);
        return e;
    }

    private Switch styledSwitch() {
        Switch sw = new Switch(this);
        int thumbOff = theme.dark ? 0xFFB9BDC4 : 0xFFFAFAFA;
        int trackOff = theme.dark ? 0x4DFFFFFF : 0x42000000;
        sw.setThumbTintList(new ColorStateList(
                new int[][]{{-android.R.attr.state_enabled}, {android.R.attr.state_checked}, {}},
                new int[]{(thumbOff & 0x00FFFFFF) | 0x80000000, cAccent, thumbOff}));
        sw.setTrackTintList(new ColorStateList(
                new int[][]{{-android.R.attr.state_enabled}, {android.R.attr.state_checked}, {}},
                new int[]{(trackOff & 0x00FFFFFF) | 0x1F000000, (cAccent & 0x00FFFFFF) | 0x80000000, trackOff}));
        return sw;
    }

    private View iconButton(int icon, String description, boolean enabled, View.OnClickListener l) {
        IconView b = new IconView(this, icon, cText, dp(18));
        b.setContentDescription(description);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(cRipple), null, null));
        b.setClickable(true);
        b.setFocusable(true);
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : 0.3f);
        b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return b;
    }

    private void checkableA11y(View host, final Class<?> cls, final CompoundButton state) {
        host.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View h, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(h, info);
                info.setClassName(cls.getName());
                info.setCheckable(true);
                info.setChecked(state.isChecked());
            }
        });
    }

    private void setRowEnabled(Row r, boolean enabled) {
        r.view.setEnabled(enabled);
        r.view.setAlpha(enabled ? 1f : 0.45f);
        if (r.sw != null) r.sw.setEnabled(enabled);
        if (r.radio != null) r.radio.setEnabled(enabled);
    }

    private void setVisible(Row r, boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        r.view.setVisibility(vis);
        if (r.divider != null) r.divider.setVisibility(vis);
    }

    private void confirm(String title, String msg, Runnable action) {
        int style = theme.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        new AlertDialog.Builder(this, style)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Delete", (d, w) -> action.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private GradientDrawable rounded(int fill, int radius, int strokeW, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (strokeW > 0) g.setStroke(strokeW, strokeColor);
        return g;
    }

    private GradientDrawable oval(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        return g;
    }

    private Drawable ripple(Drawable mask) {
        return new RippleDrawable(ColorStateList.valueOf(cRipple), null,
                mask != null ? mask : new ColorDrawable(Color.WHITE));
    }

    private static ColorStateList checkedTint(int on, int off) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{on, off});
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Draws one of the keyboard's own vector icons. */
    private static final class IconView extends View {
        private final int icon, color;
        private final float size;

        IconView(Context c, int icon, int color, float size) {
            super(c);
            this.icon = icon;
            this.color = color;
            this.size = size;
        }

        @Override
        protected void onDraw(Canvas c) {
            Icons.draw(c, icon, getWidth() / 2f, getHeight() / 2f, size, color);
        }
    }

    /** A column that stops growing past a maximum width, so text stays readable on tablets. */
    private static final class MaxWidthColumn extends LinearLayout {
        private final int maxWidth;

        MaxWidthColumn(Context c, int maxWidth) {
            super(c);
            this.maxWidth = maxWidth;
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            if (MeasureSpec.getSize(widthSpec) > maxWidth) {
                widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
            }
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
