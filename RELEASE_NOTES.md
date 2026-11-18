# Ketik Keyboard — Release Notes

## Version 1.40

### Play Store "What's new" (max 500 characters per language)

**English (en-US)**
```
• Bring your settings, words and clipboard over from the old Ketik app
• Back to the original app icon
• Redesigned settings: clear groups, setup status at a glance, live font previews
• Theme switch (System / Light / Dark) applies instantly
• One tap to reset keyboard sizes
• Extra test fields tucked away until you need them
• Updated for Android 16
• Still no internet permission: nothing you type ever leaves your phone
```

**Bahasa Indonesia (id)**
```
• Pindahkan pengaturan, kata, dan clipboard dari aplikasi Ketik lama
• Kembali ke ikon aplikasi semula
• Pengaturan baru: grup yang jelas, status penyiapan sekilas, pratinjau font langsung
• Pilihan tema (Sistem / Terang / Gelap) langsung diterapkan
• Satu ketukan untuk mengembalikan ukuran keyboard
• Kolom uji tambahan disembunyikan sampai dibutuhkan
• Diperbarui untuk Android 16
• Tetap tanpa izin internet: yang Anda ketik tidak pernah keluar dari ponsel
```

### Full notes (GitHub / website)

**What's new**
- Move from the old Ketik app (`id.ketik.keyboard`): Settings → "Import from old Ketik" copies settings,
  learned words, clipboard (including images) and emoji history in one tap. Needs the old app updated
  to 1.31, signed with the same key. Fallback: "Export my data to a file" in the old app, then
  "Import from a backup file" here.
- Back to the original blue keyboard app icon.
- Settings screen redesigned: grouped cards that follow the keyboard's light/dark palette, a setup
  status with numbered steps (hidden once Ketik is your keyboard), font choices shown in their own
  typeface, a segmented theme picker, sliders with their value shown, and a "Reset sizes" action.
- Destructive actions (clear clipboard, clear learned words, clear problem report) are shown in red
  and ask before deleting.
- Larger touch targets (48dp+), readable secondary text in both themes, and TalkBack labels for
  switches, radio choices and reorder buttons.
- Targets Android 16 (API 36). Built with Android Gradle Plugin 8.9.1 / Gradle 8.11.1.

## Version 1.32

### Play Store "What's new" (max 500 characters per language)

**English (en-US)**
```
Ketik Keyboard: a fast, private, offline keyboard for English and Indonesian.

• New app icon
• Word suggestions and auto-correct (English + Indonesian)
• Swipe typing and spacebar cursor control
• Clipboard history with pinned items, screenshots and images
• Emoji panel and text-editing panel
• One-handed mode, adjustable height, light/dark themes
• No internet permission: nothing you type ever leaves your phone
```

**Bahasa Indonesia (id)**
```
Ketik Keyboard: keyboard cepat, privat, dan offline untuk bahasa Inggris dan Indonesia.

• Ikon aplikasi baru
• Saran kata dan koreksi otomatis (Inggris + Indonesia)
• Ketik geser dan kontrol kursor lewat spasi
• Riwayat clipboard dengan item tersemat, screenshot, dan gambar
• Panel emoji dan panel edit teks
• Mode satu tangan, tinggi bisa diatur, tema terang/gelap
• Tanpa izin internet: yang Anda ketik tidak pernah keluar dari ponsel
```

### Full notes (GitHub / website)

**What's new**
- New app icon: KETIK by Faris.

**Highlights**
- English and Indonesian layouts with number hints, long-press symbols and accents, optional number row.
- ~80k-word suggestions, auto-correct (backspace undoes it), learns your words locally.
- Swipe typing, spacebar and backspace gestures, double-space types ". ".
- Clipboard history (pinning, screenshots and copied images).
- Emoji panel, text-editing panel (arrows, select, copy/cut/paste, undo/redo).
- One-handed mode, adjustable keyboard/key height and text size, light/dark/system theme, custom key fonts.
- Vibration and optional key sound.

**Privacy**
- No INTERNET permission, no trackers, no dependencies. Nothing is learned in password fields.

**Editions**
- `lite`: everything except one-time-code autofill (the Play Store edition).
- `full`: adds one-time-code autofill from notifications/SMS; install with adb, not distributed on Google Play.
