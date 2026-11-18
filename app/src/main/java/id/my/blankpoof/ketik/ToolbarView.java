package id.my.blankpoof.ketik;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

/** Top strip: tool icons when idle, word suggestions while typing. */
final class ToolbarView extends View {
    static final int ACT_EMOJI = 1, ACT_CLIPBOARD = 2, ACT_TEXT_EDIT = 3, ACT_ONE_HAND = 4,
            ACT_SIZE = 5, ACT_SETTINGS = 6, ACT_HIDE = 7, ACT_TOOLS = 8, ACT_PASTE_CLIP = 9, ACT_PASTE_OTP = 10, ACT_MORE = 11;

    interface Listener {
        void onToolbarAction(int action);
        void onSuggestionPicked(int index, String word);
        void onSuggestionLongPress(String word);
    }

    // keyboard size lives in Settings now, so the row stays clean
    // text editing and one-handed mode are used rarely, so they share the "more" button
    /** Tool names as stored in settings, in the default order. "!" in front means hidden. */
    static final String DEFAULT_ORDER = "more,emoji,clipboard,settings";
    static final String[] TOOL_NAMES = {"more", "emoji", "clipboard", "settings"};
    static final String[] TOOL_LABELS = {"More tools", "Emoji", "Clipboard", "Settings"};
    private static final int[] NAME_ACTIONS = {ACT_MORE, ACT_EMOJI, ACT_CLIPBOARD, ACT_SETTINGS};
    private static final int[] NAME_ICONS = {Icons.MORE, Icons.EMOJI, Icons.CLIPBOARD, Icons.SETTINGS};

    private int[] TOOL_ACTIONS = {ACT_MORE, ACT_EMOJI, ACT_CLIPBOARD, ACT_SETTINGS};
    private int[] TOOL_ICONS = {Icons.MORE, Icons.EMOJI, Icons.CLIPBOARD, Icons.SETTINGS};

    /** Applies the user's order, e.g. "emoji,!more,clipboard,settings" (more hidden). */
    void setOrder(String order) {
        java.util.List<Integer> acts = new java.util.ArrayList<>(), icons = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        String all = (order == null ? "" : order) + "," + DEFAULT_ORDER;   // anything missing goes last
        for (String part : all.split(",")) {
            String p = part.trim();
            boolean hidden = p.startsWith("!");
            String name = hidden ? p.substring(1) : p;
            if (name.isEmpty() || !seen.add(name)) continue;
            for (int i = 0; i < TOOL_NAMES.length; i++) {
                if (TOOL_NAMES[i].equals(name) && !hidden) {
                    acts.add(NAME_ACTIONS[i]);
                    icons.add(NAME_ICONS[i]);
                }
            }
        }
        TOOL_ACTIONS = new int[acts.size()];
        TOOL_ICONS = new int[icons.size()];
        for (int i = 0; i < acts.size(); i++) {
            TOOL_ACTIONS[i] = acts.get(i);
            TOOL_ICONS[i] = icons.get(i);
        }
        invalidate();
    }

    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Theme theme;
    private Listener listener;

    private boolean suggestionMode;
    private final String[] words = new String[3];
    private String clipText;          // when set, the strip shows a paste chip
    private Bitmap clipThumb;         // small picture for image clips (screenshots)
    private final Path thumbClip = new Path();
    private final Rect thumbSrc = new Rect();
    private final RectF thumbDst = new RectF();
    private String otpCode, otpSource; // when set, the strip shows the one-time code
    private int activeAction;

    private int pressedSlot = -1;
    private boolean longFired;
    private final Runnable longRunnable = () -> {
        if (suggestionMode && clipText == null && otpCode == null && pressedSlot >= 1 && pressedSlot <= 3) {
            String w = words[pressedSlot - 1];
            if (w != null && listener != null) {
                longFired = true;
                listener.onSuggestionLongPress(w);
            }
        }
    };

    ToolbarView(Context c, Theme t) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        theme = t;
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setTheme(Theme t) { theme = t; invalidate(); }
    void setListener(Listener l) { listener = l; }

    void setActive(int action) {
        activeAction = action;
        invalidate();
    }

    void showTools() {
        suggestionMode = false;
        clipText = null;
        otpCode = null;
        invalidate();
    }

    void showSuggestions(String left, String mid, String right) {
        suggestionMode = true;
        clipText = null;
        otpCode = null;
        words[0] = left;
        words[1] = mid;
        words[2] = right;
        invalidate();
    }

    void showClip(String text) {
        showClip(text, null);
    }

    void showClip(String text, Bitmap thumb) {
        clipThumb = thumb;
        suggestionMode = true;
        clipText = text;
        otpCode = null;
        invalidate();
    }

    void showOtp(String code, String source) {
        suggestionMode = true;
        clipText = null;
        otpCode = code;
        otpSource = source;
        invalidate();
    }

    boolean isShowingOtp() { return otpCode != null; }

    boolean isShowingSuggestions() { return suggestionMode; }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (46 * density));
    }

    private float hideWidth() { return 52 * density; }

    private float chevronW() { return 44 * density; }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float cy = h / 2f;
        if (!suggestionMode) {
            int slots = TOOL_ACTIONS.length + 1;
            float hideW = hideWidth();
            float sw = TOOL_ACTIONS.length == 0 ? 0 : (w - hideW) / TOOL_ACTIONS.length;
            for (int i = 0; i < slots; i++) {
                // tools share the space evenly; the hide arrow gets a narrow spot at the right edge
                float cx = i < TOOL_ACTIONS.length ? sw * i + sw / 2f : w - hideW / 2f;
                if (i < TOOL_ACTIONS.length) {
                    // soft round background behind each tool
                    paint.setColor(i == pressedSlot ? theme.funcKeyPressed : theme.toolCircle);
                    c.drawCircle(cx, cy, 18 * density, paint);
                    int col = TOOL_ACTIONS[i] == activeAction ? theme.iconActive : theme.icon;
                    Icons.draw(c, TOOL_ICONS[i], cx, cy, 20 * density, col);
                } else if (i == pressedSlot) {
                    drawPressed(c, cx, cy);
                }
                if (i >= TOOL_ACTIONS.length) Icons.draw(c, Icons.HIDE, cx, cy, 20 * density, theme.icon);
            }
            return;
        }
        float cw = chevronW();
        if (pressedSlot == 0) drawPressed(c, cw / 2f, cy);
        Icons.draw(c, Icons.CHEVRON_LEFT, cw / 2f, cy, 18 * density, theme.icon);
        if (otpCode != null) {
            float left = cw, right = w;
            float mx = (left + right) / 2f;
            if (pressedSlot >= 1) {
                paint.setColor(theme.funcKeyPressed);
                c.drawRoundRect(left + 8 * density, 4 * density, right - 8 * density, h - 4 * density,
                        10 * density, 10 * density, paint);
            }
            paint.setColor(theme.divider);
            c.drawRect(left, h * 0.22f, left + Math.max(1, density), h * 0.78f, paint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Fonts.regular());
            textPaint.setTextSize(11.5f * density);
            textPaint.setColor(theme.hintText);
            String src = ellipsize("From " + (otpSource == null ? "message" : otpSource), right - left - 24 * density);
            c.drawText(src, mx, h * 0.40f, textPaint);
            textPaint.setTextSize(16.5f * density);
            textPaint.setColor(theme.text);
            textPaint.setLetterSpacing(0.06f);
            c.drawText(otpCode, mx, h * 0.80f, textPaint);
            textPaint.setLetterSpacing(0f);
            return;
        }
        if (clipText != null) {
            float left = cw + 6 * density, right = w - 10 * density;
            RectF chip = new RectF(left, 7 * density, right, h - 7 * density);
            paint.setColor(pressedSlot >= 1 ? theme.keyPressed : theme.key);
            c.drawRoundRect(chip, chip.height() / 2, chip.height() / 2, paint);
            float textX = chip.left + 34 * density;
            if (clipThumb != null) {
                // mini screenshot on the left of the chip (centre-cropped, rounded)
                float th = chip.height() - 8 * density, tw = th * 1.25f;
                thumbDst.set(chip.left + 6 * density, chip.top + 4 * density, chip.left + 6 * density + tw, chip.bottom - 4 * density);
                int bw = clipThumb.getWidth(), bh = clipThumb.getHeight();
                float want = tw / th;
                if (bw / (float) bh > want) {
                    int cw2 = (int) (bh * want);
                    thumbSrc.set((bw - cw2) / 2, 0, (bw + cw2) / 2, bh);
                } else {
                    int ch2 = (int) (bw / want);
                    thumbSrc.set(0, 0, bw, Math.min(bh, ch2));   // top of a screenshot is the most useful part
                }
                thumbClip.reset();
                thumbClip.addRoundRect(thumbDst, 6 * density, 6 * density, Path.Direction.CW);
                c.save();
                c.clipPath(thumbClip);
                c.drawBitmap(clipThumb, thumbSrc, thumbDst, null);
                c.restore();
                textX = thumbDst.right + 10 * density;
            } else {
                Icons.draw(c, Icons.CLIPBOARD, chip.left + 18 * density, cy, 15 * density, theme.iconActive);
            }
            String t = clipText.replace('\n', ' ').trim();
            textPaint.setTypeface(Fonts.regular());
            textPaint.setTextSize(14 * density);
            textPaint.setColor(theme.text);
            textPaint.setTextAlign(Paint.Align.LEFT);
            float maxW = chip.right - textX - 10 * density;
            t = ellipsize(t, maxW);
            c.drawText(t, textX, baseline(cy), textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            return;
        }
        float sw = (w - cw) / 3f;
        for (int i = 0; i < 3; i++) {
            float l = cw + sw * i;
            float cx = l + sw / 2f;
            if (pressedSlot == i + 1) drawPressed(c, cx, cy, sw * 0.46f);
            if (i > 0) {
                paint.setColor(theme.divider);
                c.drawRect(l, h * 0.3f, l + Math.max(1, density), h * 0.7f, paint);
            }
            String s = words[i];
            if (s == null) continue;
            textPaint.setTypeface(i == 1 ? Fonts.bold() : Fonts.regular());
            textPaint.setTextSize(16 * density);
            textPaint.setColor(i == 1 ? theme.text : theme.hintText);
            float tw = textPaint.measureText(s);
            float maxW = sw - 12 * density;
            if (tw > maxW) textPaint.setTextScaleX(Math.max(0.6f, maxW / tw));
            String shown = textPaint.measureText(s) > maxW ? ellipsize(s, maxW) : s;
            c.drawText(shown, cx, baseline(cy), textPaint);
            textPaint.setTextScaleX(1f);
        }
    }

    private float baseline(float cy) {
        return cy - (textPaint.descent() + textPaint.ascent()) / 2f;
    }

    private String ellipsize(String s, float maxW) {
        if (textPaint.measureText(s) <= maxW) return s;
        int n = textPaint.breakText(s, true, maxW - textPaint.measureText("…"), null);
        return s.substring(0, Math.max(0, n)) + "…";
    }

    private void drawPressed(Canvas c, float cx, float cy) {
        drawPressed(c, cx, cy, 19 * density);
    }

    private void drawPressed(Canvas c, float cx, float cy, float halfW) {
        paint.setColor(theme.funcKeyPressed);
        float hh = 17 * density;
        c.drawRoundRect(cx - halfW, cy - hh, cx + halfW, cy + hh, hh, hh, paint);
    }

    private int slotAt(float x) {
        int w = getWidth();
        if (!suggestionMode) {
            float hideW = hideWidth();
            if (x >= w - hideW || TOOL_ACTIONS.length == 0) return TOOL_ACTIONS.length;
            float sw = (w - hideW) / TOOL_ACTIONS.length;
            return Math.min(TOOL_ACTIONS.length - 1, Math.max(0, (int) (x / sw)));
        }
        float cw = chevronW();
        if (x < cw) return 0;
        if (clipText != null || otpCode != null) return 1;
        return 1 + Math.min(2, Math.max(0, (int) ((x - cw) / ((w - cw) / 3f))));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedSlot = slotAt(e.getX());
                longFired = false;
                handler.postDelayed(longRunnable, 500);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (slotAt(e.getX()) != pressedSlot) {
                    handler.removeCallbacks(longRunnable);
                    pressedSlot = -1;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP: {
                handler.removeCallbacks(longRunnable);
                int slot = pressedSlot;
                pressedSlot = -1;
                invalidate();
                if (slot < 0 || longFired || listener == null) return true;
                performClick();
                if (!suggestionMode) {
                    if (slot < TOOL_ACTIONS.length) listener.onToolbarAction(TOOL_ACTIONS[slot]);
                    else listener.onToolbarAction(ACT_HIDE);
                } else if (slot == 0) {
                    listener.onToolbarAction(ACT_TOOLS);
                } else if (otpCode != null) {
                    listener.onToolbarAction(ACT_PASTE_OTP);
                } else if (clipText != null) {
                    listener.onToolbarAction(ACT_PASTE_CLIP);
                } else {
                    String w = words[slot - 1];
                    if (w != null) listener.onSuggestionPicked(slot - 1, w);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longRunnable);
                pressedSlot = -1;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
