package id.my.blankpoof.ketik;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Hand-drawn vector icons. Coordinates are in a -1..1 box scaled to the requested size. */
final class Icons {
    static final int NONE = 0, SHIFT = 1, SHIFT_ON = 2, SHIFT_LOCK = 3, BACKSPACE = 4, ENTER = 5,
            SEARCH = 6, SEND = 7, DONE = 8, NEXT = 9, EMOJI = 10, CLIPBOARD = 11, TEXT_EDIT = 12,
            ONE_HAND = 13, SETTINGS = 14, HIDE = 15, CHEVRON_LEFT = 16, CHEVRON_RIGHT = 17,
            CHEVRON_UP = 18, BACK = 19, TRASH = 20, PIN = 21, ARROW_LEFT = 22, ARROW_RIGHT = 23,
            ARROW_UP = 24, ARROW_DOWN = 25, RESIZE = 26, CLOSE = 27, CLOCK = 28, EXPAND = 29,
            GLOBE = 30, CHECK_ALL = 31, KEYBOARD = 32, KEY = 33, MORE = 34,
            // used by the settings screen
            SPARKLE = 35, VIBRATE = 36, PALETTE = 37, LOCK = 38, SHIELD = 39, INFO = 40,
            UPLOAD = 41, DOWNLOAD = 42, ARCHIVE = 43;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF R = new RectF();

    private static float cx, cy, u;

    private Icons() {}

    private static float x(float t) { return cx + t * u; }
    private static float y(float t) { return cy + t * u; }

    private static void line(Canvas c, float x1, float y1, float x2, float y2) {
        c.drawLine(x(x1), y(y1), x(x2), y(y2), P);
    }

    private static void poly(Canvas c, boolean close, float... pts) {
        PATH.reset();
        PATH.moveTo(x(pts[0]), y(pts[1]));
        for (int i = 2; i < pts.length; i += 2) PATH.lineTo(x(pts[i]), y(pts[i + 1]));
        if (close) PATH.close();
        c.drawPath(PATH, P);
    }

    private static void circle(Canvas c, float px, float py, float r) {
        c.drawCircle(x(px), y(py), r * u, P);
    }

    private static void rrect(Canvas c, float l, float t, float r, float b, float rad) {
        R.set(x(l), y(t), x(r), y(b));
        c.drawRoundRect(R, rad * u, rad * u, P);
    }

    static void draw(Canvas c, int icon, float centerX, float centerY, float size, int color) {
        cx = centerX;
        cy = centerY;
        u = size / 2f;
        P.setColor(color);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(Math.max(1.5f, size * 0.085f));
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        switch (icon) {
            case SHIFT:
            case SHIFT_ON:
            case SHIFT_LOCK:
                if (icon != SHIFT) P.setStyle(Paint.Style.FILL_AND_STROKE);
                poly(c, true, 0f, -0.8f, 0.72f, -0.02f, 0.32f, -0.02f, 0.32f, 0.55f,
                        -0.32f, 0.55f, -0.32f, -0.02f, -0.72f, -0.02f);
                if (icon == SHIFT_LOCK) {
                    P.setStyle(Paint.Style.STROKE);
                    line(c, -0.42f, 0.85f, 0.42f, 0.85f);
                }
                break;
            case BACKSPACE:
                poly(c, true, -0.9f, 0f, -0.42f, -0.58f, 0.85f, -0.58f, 0.85f, 0.58f, -0.42f, 0.58f);
                line(c, -0.02f, -0.24f, 0.44f, 0.24f);
                line(c, 0.44f, -0.24f, -0.02f, 0.24f);
                break;
            case ENTER:
                poly(c, false, 0.72f, -0.62f, 0.72f, 0.12f, -0.66f, 0.12f);
                poly(c, false, -0.34f, -0.2f, -0.68f, 0.12f, -0.34f, 0.44f);
                break;
            case SEARCH:
                circle(c, -0.14f, -0.14f, 0.5f);
                line(c, 0.24f, 0.24f, 0.72f, 0.72f);
                break;
            case SEND:
            case NEXT:
            case ARROW_RIGHT:
                line(c, -0.72f, 0f, 0.7f, 0f);
                poly(c, false, 0.22f, -0.46f, 0.7f, 0f, 0.22f, 0.46f);
                break;
            case BACK:
            case ARROW_LEFT:
                line(c, 0.72f, 0f, -0.7f, 0f);
                poly(c, false, -0.22f, -0.46f, -0.7f, 0f, -0.22f, 0.46f);
                break;
            case ARROW_UP:
                line(c, 0f, 0.72f, 0f, -0.7f);
                poly(c, false, -0.46f, -0.22f, 0f, -0.7f, 0.46f, -0.22f);
                break;
            case ARROW_DOWN:
                line(c, 0f, -0.72f, 0f, 0.7f);
                poly(c, false, -0.46f, 0.22f, 0f, 0.7f, 0.46f, 0.22f);
                break;
            case DONE:
                poly(c, false, -0.66f, 0.02f, -0.2f, 0.46f, 0.7f, -0.46f);
                break;
            case EMOJI:
                circle(c, 0f, 0f, 0.78f);
                P.setStyle(Paint.Style.FILL);
                circle(c, -0.28f, -0.2f, 0.09f);
                circle(c, 0.28f, -0.2f, 0.09f);
                P.setStyle(Paint.Style.STROKE);
                R.set(x(-0.42f), y(-0.12f), x(0.42f), y(0.46f));
                c.drawArc(R, 25, 130, false, P);
                break;
            case CLIPBOARD:
                rrect(c, -0.58f, -0.6f, 0.58f, 0.82f, 0.14f);
                rrect(c, -0.26f, -0.82f, 0.26f, -0.46f, 0.1f);
                line(c, -0.28f, -0.04f, 0.28f, -0.04f);
                line(c, -0.28f, 0.32f, 0.12f, 0.32f);
                break;
            case TEXT_EDIT:
                line(c, 0f, -0.6f, 0f, 0.6f);
                line(c, -0.18f, -0.6f, 0.18f, -0.6f);
                line(c, -0.18f, 0.6f, 0.18f, 0.6f);
                poly(c, false, -0.5f, -0.24f, -0.76f, 0f, -0.5f, 0.24f);
                poly(c, false, 0.5f, -0.24f, 0.76f, 0f, 0.5f, 0.24f);
                break;
            case ONE_HAND:
                rrect(c, -0.48f, -0.8f, 0.48f, 0.8f, 0.16f);
                P.setStyle(Paint.Style.FILL);
                rrect(c, -0.06f, 0.08f, 0.3f, 0.56f, 0.06f);
                break;
            case SETTINGS: {
                PATH.reset();
                int teeth = 8;
                float rIn = 0.6f, rOut = 0.84f;
                double seg = Math.PI * 2 / teeth;
                for (int i = 0; i < teeth; i++) {
                    double a0 = seg * i - seg * 0.5;
                    double[] angs = {a0 + seg * 0.08, a0 + seg * 0.3, a0 + seg * 0.7, a0 + seg * 0.92};
                    float[] rads = {rIn, rOut, rOut, rIn};
                    for (int k = 0; k < 4; k++) {
                        float px = (float) Math.cos(angs[k]) * rads[k], py = (float) Math.sin(angs[k]) * rads[k];
                        if (i == 0 && k == 0) PATH.moveTo(x(px), y(py)); else PATH.lineTo(x(px), y(py));
                    }
                }
                PATH.close();
                c.drawPath(PATH, P);
                circle(c, 0f, 0f, 0.27f);
                break;
            }
            case HIDE:
                poly(c, false, -0.58f, -0.22f, 0f, 0.32f, 0.58f, -0.22f);
                break;
            case CHEVRON_UP:
                poly(c, false, -0.58f, 0.22f, 0f, -0.32f, 0.58f, 0.22f);
                break;
            case CHEVRON_LEFT:
                poly(c, false, 0.24f, -0.58f, -0.3f, 0f, 0.24f, 0.58f);
                break;
            case CHEVRON_RIGHT:
                poly(c, false, -0.24f, -0.58f, 0.3f, 0f, -0.24f, 0.58f);
                break;
            case TRASH:
                line(c, -0.68f, -0.5f, 0.68f, -0.5f);
                poly(c, false, -0.22f, -0.5f, -0.22f, -0.74f, 0.22f, -0.74f, 0.22f, -0.5f);
                poly(c, false, -0.5f, -0.5f, -0.4f, 0.78f, 0.4f, 0.78f, 0.5f, -0.5f);
                line(c, -0.12f, -0.16f, -0.12f, 0.48f);
                line(c, 0.14f, -0.16f, 0.14f, 0.48f);
                break;
            case PIN:
                poly(c, true, -0.3f, -0.78f, 0.3f, -0.78f, 0.22f, -0.2f, 0.5f, 0.12f, -0.5f, 0.12f, -0.22f, -0.2f);
                line(c, 0f, 0.12f, 0f, 0.82f);
                break;
            case RESIZE:
                line(c, 0f, -0.74f, 0f, 0.74f);
                poly(c, false, -0.34f, -0.42f, 0f, -0.76f, 0.34f, -0.42f);
                poly(c, false, -0.34f, 0.42f, 0f, 0.76f, 0.34f, 0.42f);
                line(c, -0.7f, 0f, -0.36f, 0f);
                line(c, 0.36f, 0f, 0.7f, 0f);
                break;
            case CLOSE:
                line(c, -0.5f, -0.5f, 0.5f, 0.5f);
                line(c, 0.5f, -0.5f, -0.5f, 0.5f);
                break;
            case CLOCK:
                circle(c, 0f, 0f, 0.76f);
                poly(c, false, 0f, -0.44f, 0f, 0f, 0.34f, 0.2f);
                break;
            case EXPAND:
                line(c, -0.76f, 0f, 0.76f, 0f);
                poly(c, false, -0.46f, -0.3f, -0.78f, 0f, -0.46f, 0.3f);
                poly(c, false, 0.46f, -0.3f, 0.78f, 0f, 0.46f, 0.3f);
                break;
            case GLOBE:
                circle(c, 0f, 0f, 0.76f);
                line(c, -0.76f, 0f, 0.76f, 0f);
                R.set(x(-0.34f), y(-0.76f), x(0.34f), y(0.76f));
                c.drawOval(R, P);
                break;
            case CHECK_ALL:
                rrect(c, -0.7f, -0.7f, 0.7f, 0.7f, 0.14f);
                poly(c, false, -0.38f, 0.02f, -0.1f, 0.3f, 0.4f, -0.26f);
                break;
            case KEYBOARD:
                rrect(c, -0.86f, -0.56f, 0.86f, 0.56f, 0.14f);
                P.setStyle(Paint.Style.FILL);
                for (int i = 0; i < 4; i++) {
                    circle(c, -0.5f + i * 0.33f, -0.22f, 0.07f);
                    circle(c, -0.5f + i * 0.33f, 0.06f, 0.07f);
                }
                P.setStyle(Paint.Style.STROKE);
                line(c, -0.36f, 0.32f, 0.36f, 0.32f);
                break;
            case MORE:
                P.setStyle(Paint.Style.FILL);
                circle(c, -0.56f, 0f, 0.13f);
                circle(c, 0f, 0f, 0.13f);
                circle(c, 0.56f, 0f, 0.13f);
                break;
            case KEY:
                circle(c, -0.42f, 0f, 0.32f);
                line(c, -0.1f, 0f, 0.82f, 0f);
                line(c, 0.52f, 0f, 0.52f, 0.3f);
                line(c, 0.74f, 0f, 0.74f, 0.24f);
                break;
            case SPARKLE:
                poly(c, true, -0.1f, -0.8f, 0.08f, -0.22f, 0.62f, -0.04f, 0.08f, 0.14f,
                        -0.1f, 0.72f, -0.28f, 0.14f, -0.82f, -0.04f, -0.28f, -0.22f);
                line(c, 0.6f, -0.78f, 0.6f, -0.44f);
                line(c, 0.43f, -0.61f, 0.77f, -0.61f);
                break;
            case VIBRATE:
                rrect(c, -0.3f, -0.68f, 0.3f, 0.68f, 0.1f);
                line(c, -0.56f, -0.34f, -0.56f, 0.34f);
                line(c, 0.56f, -0.34f, 0.56f, 0.34f);
                line(c, -0.8f, -0.16f, -0.8f, 0.16f);
                line(c, 0.8f, -0.16f, 0.8f, 0.16f);
                break;
            case PALETTE:
                PATH.reset();
                R.set(x(-0.78f), y(-0.78f), x(0.78f), y(0.78f));
                PATH.arcTo(R, 60, 300, true);
                PATH.quadTo(x(0.24f), y(0.08f), x(0.12f), y(0.3f));
                PATH.quadTo(x(0.02f), y(0.56f), x(0.39f), y(0.68f));
                c.drawPath(PATH, P);
                P.setStyle(Paint.Style.FILL);
                circle(c, -0.38f, -0.1f, 0.11f);
                circle(c, -0.14f, -0.44f, 0.11f);
                circle(c, 0.26f, -0.4f, 0.11f);
                circle(c, -0.3f, 0.32f, 0.11f);
                break;
            case LOCK:
                rrect(c, -0.6f, -0.08f, 0.6f, 0.8f, 0.12f);
                R.set(x(-0.38f), y(-0.8f), x(0.38f), y(-0.04f));
                c.drawArc(R, 180, 180, false, P);
                line(c, -0.38f, -0.42f, -0.38f, -0.08f);
                line(c, 0.38f, -0.42f, 0.38f, -0.08f);
                line(c, 0f, 0.26f, 0f, 0.46f);
                break;
            case SHIELD:
                poly(c, true, 0f, -0.82f, 0.68f, -0.56f, 0.6f, 0.16f, 0f, 0.82f, -0.6f, 0.16f, -0.68f, -0.56f);
                poly(c, false, -0.28f, -0.02f, -0.06f, 0.2f, 0.3f, -0.22f);
                break;
            case INFO:
                circle(c, 0f, 0f, 0.78f);
                line(c, 0f, -0.04f, 0f, 0.42f);
                P.setStyle(Paint.Style.FILL);
                circle(c, 0f, -0.36f, 0.08f);
                break;
            case UPLOAD:
                line(c, 0f, 0.28f, 0f, -0.74f);
                poly(c, false, -0.36f, -0.4f, 0f, -0.76f, 0.36f, -0.4f);
                poly(c, false, -0.72f, 0.2f, -0.72f, 0.72f, 0.72f, 0.72f, 0.72f, 0.2f);
                break;
            case DOWNLOAD:
                line(c, 0f, -0.76f, 0f, 0.28f);
                poly(c, false, -0.36f, -0.06f, 0f, 0.3f, 0.36f, -0.06f);
                poly(c, false, -0.72f, 0.2f, -0.72f, 0.72f, 0.72f, 0.72f, 0.72f, 0.2f);
                break;
            case ARCHIVE:
                rrect(c, -0.8f, -0.7f, 0.8f, -0.3f, 0.08f);
                poly(c, false, -0.66f, -0.3f, -0.66f, 0.72f, 0.66f, 0.72f, 0.66f, -0.3f);
                line(c, -0.22f, 0.04f, 0.22f, 0.04f);
                break;
            default:
                break;
        }
    }
}
