package com.stock.flow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Munting mga icon na Canvas-drawn, gaya ng style ng
 * StockFlowBottomNav - walang drawable resources na kailangan.
 */
public class IconViews {

    public static final int TYPE_BELL = 0;
    public static final int TYPE_SEARCH = 1;
    public static final int TYPE_FILTER = 2;
    public static final int TYPE_CHEVRON = 3;
    public static final int TYPE_MENU = 4;
    public static final int TYPE_CALENDAR = 5;
    public static final int TYPE_BARCODE = 6;
    public static final int TYPE_CUBE = 7;
    public static final int TYPE_HASH = 8;
    public static final int TYPE_TAG = 9;
    public static final int TYPE_COINS = 10;
    public static final int TYPE_BAG = 11;
    public static final int TYPE_WALLET = 12;
    public static final int TYPE_LOCK = 13;
    public static final int TYPE_CART = 14;
    public static final int TYPE_PLUS = 15;
    public static final int TYPE_WARNING = 16;
    public static final int TYPE_EMAIL = 17;
    public static final int TYPE_PERSON = 18;
    public static final int TYPE_EYE = 19;
    public static final int TYPE_EYE_OFF = 20;
    public static final int TYPE_MAIL = 21;
    public static final int TYPE_USER = 22;
    public static final int TYPE_GOOGLE = 23;
    public static final int TYPE_LOGOUT = 24;

    private static final int GOOGLE_BLUE = Color.rgb(66, 133, 244);

    public static class IconView extends View {

        private int type;
        private final int color;
        private final float density;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean showBadge = false;

        public IconView(Context context, int type, int color) {
            super(context);
            this.type = type;
            this.color = color;
            this.density = context.getResources()
                    .getDisplayMetrics().density;
        }

        public void setType(int type) {
            this.type = type;
            invalidate();
        }

        public void setShowBadge(boolean show) {
            this.showBadge = show;
            invalidate();
        }

        private float dp(float v) {
            return v * density;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.8f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            switch (type) {
                case TYPE_BELL:
                    drawBell(canvas, cx, cy);
                    break;
                case TYPE_SEARCH:
                    drawSearch(canvas, cx, cy);
                    break;
                case TYPE_FILTER:
                    drawFilter(canvas, cx, cy);
                    break;
                case TYPE_CHEVRON:
                    drawChevron(canvas, cx, cy);
                    break;
                case TYPE_MENU:
                    drawMenu(canvas, cx, cy);
                    break;
                case TYPE_CALENDAR:
                    drawCalendar(canvas, cx, cy);
                    break;
                case TYPE_BARCODE:
                    drawBarcode(canvas, cx, cy);
                    break;
                case TYPE_CUBE:
                    drawCube(canvas, cx, cy);
                    break;
                case TYPE_HASH:
                    drawHash(canvas, cx, cy);
                    break;
                case TYPE_TAG:
                    drawTag(canvas, cx, cy);
                    break;
                case TYPE_COINS:
                    drawCoins(canvas, cx, cy);
                    break;
                case TYPE_BAG:
                    drawBag(canvas, cx, cy);
                    break;
                case TYPE_WALLET:
                    drawWallet(canvas, cx, cy);
                    break;
                case TYPE_LOCK:
                    drawLock(canvas, cx, cy);
                    break;
                case TYPE_CART:
                    drawCart(canvas, cx, cy);
                    break;
                case TYPE_PLUS:
                    drawPlus(canvas, cx, cy);
                    break;
                case TYPE_WARNING:
                    drawWarning(canvas, cx, cy);
                    break;
                case TYPE_EMAIL:
                    drawEmail(canvas, cx, cy);
                    break;
                case TYPE_PERSON:
                    drawPerson(canvas, cx, cy);
                    break;
                case TYPE_EYE:
                    drawEye(canvas, cx, cy);
                    break;
                case TYPE_EYE_OFF:
                    drawEyeOff(canvas, cx, cy);
                    break;
                case TYPE_MAIL:
                    drawMail(canvas, cx, cy);
                    break;
                case TYPE_USER:
                    drawUser(canvas, cx, cy);
                    break;
                case TYPE_GOOGLE:
                    drawGoogle(canvas, cx, cy);
                    break;
                case TYPE_LOGOUT:
                    drawLogout(canvas, cx, cy);
                    break;
            }
        }

        private void drawBell(Canvas canvas, float cx, float cy) {

            Path bell = new Path();

            bell.moveTo(cx - dp(6), cy + dp(3));
            bell.cubicTo(
                    cx - dp(6), cy - dp(6),
                    cx - dp(4), cy - dp(9),
                    cx, cy - dp(9)
            );
            bell.cubicTo(
                    cx + dp(4), cy - dp(9),
                    cx + dp(6), cy - dp(6),
                    cx + dp(6), cy + dp(3)
            );
            bell.lineTo(cx + dp(8), cy + dp(6));
            bell.lineTo(cx - dp(8), cy + dp(6));
            bell.close();

            canvas.drawPath(bell, paint);

            canvas.drawLine(
                    cx - dp(2.5f), cy + dp(8),
                    cx + dp(2.5f), cy + dp(8),
                    paint
            );

            if (showBadge) {
                Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                badgePaint.setColor(Color.rgb(230, 60, 60));
                badgePaint.setStyle(Paint.Style.FILL);

                canvas.drawCircle(
                        cx + dp(9),
                        cy - dp(9),
                        dp(5),
                        badgePaint
                );
            }
        }

        private void drawSearch(Canvas canvas, float cx, float cy) {

            canvas.drawCircle(
                    cx - dp(1.5f),
                    cy - dp(1.5f),
                    dp(6),
                    paint
            );

            canvas.drawLine(
                    cx + dp(3),
                    cy + dp(3),
                    cx + dp(7.5f),
                    cy + dp(7.5f),
                    paint
            );
        }

        private void drawFilter(Canvas canvas, float cx, float cy) {

            float left = cx - dp(8);
            float right = cx + dp(8);

            float y1 = cy - dp(6);
            float y2 = cy;
            float y3 = cy + dp(6);

            canvas.drawLine(left, y1, right, y1, paint);
            canvas.drawLine(left, y2, right, y2, paint);
            canvas.drawLine(left, y3, right, y3, paint);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(color);
            dotPaint.setStyle(Paint.Style.FILL);

            canvas.drawCircle(cx - dp(3), y1, dp(2), dotPaint);
            canvas.drawCircle(cx + dp(2), y2, dp(2), dotPaint);
            canvas.drawCircle(cx - dp(1), y3, dp(2), dotPaint);
        }

        private void drawChevron(Canvas canvas, float cx, float cy) {

            Path chevron = new Path();

            chevron.moveTo(cx - dp(3), cy - dp(5));
            chevron.lineTo(cx + dp(3), cy);
            chevron.lineTo(cx - dp(3), cy + dp(5));

            canvas.drawPath(chevron, paint);
        }

        private void drawMenu(Canvas canvas, float cx, float cy) {

            float half = dp(8);

            canvas.drawLine(cx - half, cy - dp(6), cx + half, cy - dp(6), paint);
            canvas.drawLine(cx - half, cy, cx + half, cy, paint);
            canvas.drawLine(cx - half, cy + dp(6), cx + half, cy + dp(6), paint);
        }

        private void drawCalendar(Canvas canvas, float cx, float cy) {

            RectF r = new RectF(
                    cx - dp(9), cy - dp(7),
                    cx + dp(9), cy + dp(9)
            );

            canvas.drawRoundRect(r, dp(2.5f), dp(2.5f), paint);

            canvas.drawLine(cx - dp(9), cy - dp(2), cx + dp(9), cy - dp(2), paint);

            canvas.drawLine(cx - dp(5), cy - dp(9), cx - dp(5), cy - dp(5), paint);
            canvas.drawLine(cx + dp(5), cy - dp(9), cx + dp(5), cy - dp(5), paint);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(color);
            dotPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx + dp(4), cy + dp(3), dp(1.6f), dotPaint);
        }

        private void drawBarcode(Canvas canvas, float cx, float cy) {

            float top = cy - dp(7);
            float bottom = cy + dp(7);

            float[] xs = {-dp(8), -dp(4.5f), -dp(1.5f), dp(2), dp(5), dp(8)};
            float[] widths = {1.6f, 1f, 1.6f, 1f, 1.6f, 1f};

            for (int i = 0; i < xs.length; i++) {
                paint.setStrokeWidth(dp(widths[i]));
                canvas.drawLine(cx + xs[i], top, cx + xs[i], bottom, paint);
            }
        }

        private void drawCube(Canvas canvas, float cx, float cy) {

            Path top = new Path();
            top.moveTo(cx, cy - dp(9));
            top.lineTo(cx + dp(8), cy - dp(4.5f));
            top.lineTo(cx, cy);
            top.lineTo(cx - dp(8), cy - dp(4.5f));
            top.close();
            canvas.drawPath(top, paint);

            canvas.drawLine(cx - dp(8), cy - dp(4.5f), cx - dp(8), cy + dp(4.5f), paint);
            canvas.drawLine(cx + dp(8), cy - dp(4.5f), cx + dp(8), cy + dp(4.5f), paint);
            canvas.drawLine(cx, cy, cx, cy + dp(9), paint);
            canvas.drawLine(cx - dp(8), cy + dp(4.5f), cx, cy + dp(9), paint);
            canvas.drawLine(cx + dp(8), cy + dp(4.5f), cx, cy + dp(9), paint);
        }

        private void drawHash(Canvas canvas, float cx, float cy) {

            canvas.drawLine(cx - dp(3), cy - dp(8), cx - dp(5), cy + dp(8), paint);
            canvas.drawLine(cx + dp(3), cy - dp(8), cx + dp(1), cy + dp(8), paint);

            canvas.drawLine(cx - dp(7), cy - dp(2.5f), cx + dp(7), cy - dp(2.5f), paint);
            canvas.drawLine(cx - dp(7), cy + dp(2.5f), cx + dp(7), cy + dp(2.5f), paint);
        }

        private void drawTag(Canvas canvas, float cx, float cy) {

            Path tag = new Path();
            tag.moveTo(cx - dp(8), cy - dp(2));
            tag.lineTo(cx, cy - dp(9));
            tag.lineTo(cx + dp(8), cy - dp(1));
            tag.lineTo(cx, cy + dp(9));
            tag.close();

            canvas.drawPath(tag, paint);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(color);
            dotPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx - dp(3.5f), cy - dp(2), dp(1.6f), dotPaint);
        }

        private void drawCoins(Canvas canvas, float cx, float cy) {

            RectF top = new RectF(
                    cx - dp(8), cy - dp(9), cx + dp(8), cy - dp(2)
            );
            canvas.drawOval(top, paint);

            canvas.drawLine(cx - dp(8), cy - dp(5.5f), cx - dp(8), cy + dp(6.5f), paint);
            canvas.drawLine(cx + dp(8), cy - dp(5.5f), cx + dp(8), cy + dp(6.5f), paint);

            Path bottomArc = new Path();
            bottomArc.addArc(
                    cx - dp(8), cy + dp(2), cx + dp(8), cy + dp(9),
                    0, 180
            );
            canvas.drawPath(bottomArc, paint);

            canvas.drawLine(cx - dp(8), cy + dp(0.5f), cx + dp(8), cy + dp(0.5f), paint);
        }

        private void drawBag(Canvas canvas, float cx, float cy) {

            RectF body = new RectF(
                    cx - dp(8), cy - dp(3), cx + dp(8), cy + dp(9)
            );
            canvas.drawRoundRect(body, dp(2), dp(2), paint);

            Path handle = new Path();
            handle.moveTo(cx - dp(4), cy - dp(3));
            handle.cubicTo(
                    cx - dp(4), cy - dp(10),
                    cx + dp(4), cy - dp(10),
                    cx + dp(4), cy - dp(3)
            );
            canvas.drawPath(handle, paint);
        }

        private void drawWallet(Canvas canvas, float cx, float cy) {

            RectF body = new RectF(
                    cx - dp(9), cy - dp(6), cx + dp(9), cy + dp(7)
            );
            canvas.drawRoundRect(body, dp(3), dp(3), paint);

            RectF flap = new RectF(
                    cx + dp(2), cy - dp(1.5f), cx + dp(9), cy + dp(4.5f)
            );
            canvas.drawRoundRect(flap, dp(2), dp(2), paint);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(color);
            dotPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx + dp(5.5f), cy + dp(1.5f), dp(1.4f), dotPaint);
        }

        private void drawLock(Canvas canvas, float cx, float cy) {

            RectF body = new RectF(
                    cx - dp(7), cy - dp(1), cx + dp(7), cy + dp(9)
            );
            canvas.drawRoundRect(body, dp(2), dp(2), paint);

            Path shackle = new Path();
            shackle.moveTo(cx - dp(4.5f), cy - dp(1));
            shackle.lineTo(cx - dp(4.5f), cy - dp(6));
            shackle.cubicTo(
                    cx - dp(4.5f), cy - dp(11),
                    cx + dp(4.5f), cy - dp(11),
                    cx + dp(4.5f), cy - dp(6)
            );
            shackle.lineTo(cx + dp(4.5f), cy - dp(1));
            canvas.drawPath(shackle, paint);
        }

        private void drawCart(Canvas canvas, float cx, float cy) {

            Path basket = new Path();
            basket.moveTo(cx - dp(9), cy - dp(7));
            basket.lineTo(cx - dp(6), cy - dp(7));
            basket.lineTo(cx - dp(2), cy + dp(3));
            basket.lineTo(cx + dp(8), cy + dp(3));
            basket.lineTo(cx + dp(10), cy - dp(4));
            basket.lineTo(cx - dp(4.5f), cy - dp(4));

            canvas.drawPath(basket, paint);

            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setColor(color);
            fillPaint.setStyle(Paint.Style.FILL);

            canvas.drawCircle(cx - dp(0.5f), cy + dp(6.5f), dp(1.6f), fillPaint);
            canvas.drawCircle(cx + dp(6), cy + dp(6.5f), dp(1.6f), fillPaint);
        }

        private void drawPlus(Canvas canvas, float cx, float cy) {

            canvas.drawLine(cx, cy - dp(8), cx, cy + dp(8), paint);
            canvas.drawLine(cx - dp(8), cy, cx + dp(8), cy, paint);
        }

        private void drawWarning(Canvas canvas, float cx, float cy) {

            Path triangle = new Path();
            triangle.moveTo(cx, cy - dp(9));
            triangle.lineTo(cx + dp(9), cy + dp(7));
            triangle.lineTo(cx - dp(9), cy + dp(7));
            triangle.close();

            canvas.drawPath(triangle, paint);

            canvas.drawLine(cx, cy - dp(3), cx, cy + dp(1.5f), paint);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(color);
            dotPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy + dp(4.5f), dp(1.3f), dotPaint);
        }

        private void drawEmail(Canvas canvas, float cx, float cy) {

            RectF body = new RectF(
                    cx - dp(9), cy - dp(6.5f), cx + dp(9), cy + dp(6.5f)
            );
            canvas.drawRoundRect(body, dp(2), dp(2), paint);

            Path flap = new Path();
            flap.moveTo(cx - dp(9), cy - dp(5));
            flap.lineTo(cx, cy + dp(1));
            flap.lineTo(cx + dp(9), cy - dp(5));
            canvas.drawPath(flap, paint);
        }

        private void drawPerson(Canvas canvas, float cx, float cy) {

            canvas.drawCircle(cx, cy - dp(5), dp(4), paint);

            Path shoulders = new Path();
            shoulders.moveTo(cx - dp(8), cy + dp(9));
            shoulders.cubicTo(
                    cx - dp(8), cy + dp(1),
                    cx + dp(8), cy + dp(1),
                    cx + dp(8), cy + dp(9)
            );
            canvas.drawPath(shoulders, paint);
        }

        private void drawEye(Canvas canvas, float cx, float cy) {

            Path eye = new Path();
            eye.moveTo(cx - dp(10), cy);
            eye.quadTo(cx, cy - dp(7), cx + dp(10), cy);
            eye.quadTo(cx, cy + dp(7), cx - dp(10), cy);
            eye.close();

            canvas.drawPath(eye, paint);

            canvas.drawCircle(cx, cy, dp(3), paint);
        }

        private void drawEyeOff(Canvas canvas, float cx, float cy) {

            Path eye = new Path();
            eye.moveTo(cx - dp(10), cy);
            eye.quadTo(cx, cy - dp(7), cx + dp(10), cy);
            eye.quadTo(cx, cy + dp(7), cx - dp(10), cy);
            eye.close();

            canvas.drawPath(eye, paint);

            canvas.drawCircle(cx, cy, dp(3), paint);

            canvas.drawLine(
                    cx - dp(11), cy + dp(9),
                    cx + dp(11), cy - dp(9),
                    paint
            );
        }

        private void drawMail(Canvas canvas, float cx, float cy) {

            RectF body = new RectF(
                    cx - dp(10), cy - dp(7), cx + dp(10), cy + dp(7)
            );
            canvas.drawRoundRect(body, dp(2), dp(2), paint);

            Path flap = new Path();
            flap.moveTo(cx - dp(10), cy - dp(6));
            flap.lineTo(cx, cy + dp(1));
            flap.lineTo(cx + dp(10), cy - dp(6));
            canvas.drawPath(flap, paint);
        }

        private void drawUser(Canvas canvas, float cx, float cy) {

            canvas.drawCircle(cx, cy - dp(5), dp(4.5f), paint);

            Path shoulders = new Path();
            shoulders.moveTo(cx - dp(8), cy + dp(9));
            shoulders.quadTo(cx - dp(8), cy + dp(1), cx, cy + dp(1));
            shoulders.quadTo(cx + dp(8), cy + dp(1), cx + dp(8), cy + dp(9));

            canvas.drawPath(shoulders, paint);
        }

        private void drawGoogle(Canvas canvas, float cx, float cy) {

            Paint gPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gPaint.setColor(GOOGLE_BLUE);
            gPaint.setStyle(Paint.Style.STROKE);
            gPaint.setStrokeWidth(dp(2));
            gPaint.setStrokeCap(Paint.Cap.ROUND);

            RectF oval = new RectF(
                    cx - dp(8), cy - dp(8), cx + dp(8), cy + dp(8)
            );

            // "C" shape - bukas sa kanan, gaya ng letrang G
            canvas.drawArc(oval, 30, 300, false, gPaint);

            // Crossbar ng G
            canvas.drawLine(cx + dp(1), cy, cx + dp(8), cy, gPaint);
        }

        private void drawLogout(Canvas canvas, float cx, float cy) {

            // Pintuan (bukas sa kanan)
            Path door = new Path();
            door.moveTo(cx + dp(1), cy - dp(9));
            door.lineTo(cx - dp(8), cy - dp(9));
            door.lineTo(cx - dp(8), cy + dp(9));
            door.lineTo(cx + dp(1), cy + dp(9));
            canvas.drawPath(door, paint);

            // Palabas na arrow
            canvas.drawLine(cx - dp(3), cy, cx + dp(9), cy, paint);
            canvas.drawLine(cx + dp(4), cy - dp(5), cx + dp(9), cy, paint);
            canvas.drawLine(cx + dp(4), cy + dp(5), cx + dp(9), cy, paint);
        }
    }
}
