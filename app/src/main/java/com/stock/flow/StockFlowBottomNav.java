package com.stock.flow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

public class StockFlowBottomNav extends View {

    public interface OnNavigationSelectedListener {
        void onNavigationSelected(int position);
    }

    private OnNavigationSelectedListener listener;

    private Paint paint;
    private Path path;

    private int selected = 0;

    private final int GREEN = Color.rgb(45, 157, 113);
    private final int INACTIVE = Color.rgb(185, 185, 185);
    private final int BAR_COLOR = Color.WHITE;
    private final int BACKGROUND = Color.rgb(245, 245, 245);

    private float density;

    private float barLeft;
    private float barRight;
    private float barTop;
    private float barBottom;

    private float centerX;
    private float centerY;

    private float barHeight;
    private float cornerRadius;
    private float fabRadius;

    // Icons - ilagay ang mga file sa res/drawable gamit itong
    // eksaktong pangalan (ic_nav_home, ic_nav_inventory, atbp.)
    private Drawable iconHome;
    private Drawable iconInventory;
    private Drawable iconPos;
    private Drawable iconSales;
    private Drawable iconProfile;

    public StockFlowBottomNav(Context context) {
        super(context);

        density = getResources()
                .getDisplayMetrics()
                .density;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path = new Path();

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setBackgroundColor(Color.TRANSPARENT);

        setClickable(true);

        iconHome = ContextCompat.getDrawable(context, R.drawable.ic_nav_home);
        iconInventory = ContextCompat.getDrawable(context, R.drawable.ic_nav_inventory);
        iconPos = ContextCompat.getDrawable(context, R.drawable.ic_nav_pos);
        iconSales = ContextCompat.getDrawable(context, R.drawable.ic_nav_sales);
        iconProfile = ContextCompat.getDrawable(context, R.drawable.ic_profile);
    }

    private boolean barVisible = true;

    /**
     * Itinatago o ipinapakita ang buong bar gamit ang slide-down/up
     * na animation. Ligtas itong tawagin bago pa ma-layout ang View
     * (naghihintay muna gamit ang post()).
     */
    public void setBarVisible(boolean visible) {

        if (visible == barVisible) {
            return;
        }

        barVisible = visible;

        if (getHeight() == 0) {
            post(() -> animate()
                    .translationY(visible ? 0f : getHeight())
                    .setDuration(200)
                    .start());
            return;
        }

        animate()
                .translationY(visible ? 0f : getHeight())
                .setDuration(200)
                .start();
    }

    public void setOnNavigationSelectedListener(
            OnNavigationSelectedListener listener) {

        this.listener = listener;
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        calculatePositions();

        drawNavigationBar(canvas);
        drawNavigationIcons(canvas);
        drawCenterButton(canvas);
    }

    private void calculatePositions() {

        float width = getWidth();
        float height = getHeight();

        barHeight = dp(64);

        barLeft = dp(24);
        barRight = width - dp(24);

        barBottom = height - dp(12);
        barTop = barBottom - barHeight;

        cornerRadius = dp(30);

        fabRadius = dp(29);

        centerX = width / 2f;

        centerY = barTop + dp(2);
    }

    private void drawNavigationBar(Canvas canvas) {

    path.reset();

    float top = barTop;
    float bottom = barBottom;

    // Malapad na cradle para may clearance sa FAB
    float notchHalfWidth = fabRadius + dp(18);

    float left = centerX - notchHalfWidth;
    float right = centerX + notchHalfWidth;

    // Sapat na lalim para totoong makita ang curve (hindi patag, hindi rin dikit)
    float depth = fabRadius * 0.85f;
    float bottomY = top + depth;

    // Control offset na naka-proportion sa lapad, para dahan-dahan
    // ang pag-yuko ng curve (hindi biglaang bend)
    float controlOffset = notchHalfWidth * 0.55f;

    path.moveTo(
            barLeft + cornerRadius,
            top
    );

    // LEFT FLAT
    path.lineTo(
            left,
            top
    );

    /*
     * LEFT CURVE
     *
     * Iisang cubic lang - tuloy-tuloy at smooth,
     * horizontal ang tangent sa dulo (start at bottom)
     * kaya walang kink.
     */
    path.cubicTo(
            left + controlOffset,
            top,
            centerX - controlOffset,
            bottomY,
            centerX,
            bottomY
    );

    /*
     * RIGHT CURVE
     */
    path.cubicTo(
            centerX + controlOffset,
            bottomY,
            right - controlOffset,
            top,
            right,
            top
    );

    // RIGHT FLAT
    path.lineTo(
            barRight - cornerRadius,
            top
    );

    // RIGHT TOP CORNER
    path.quadTo(
            barRight,
            top,
            barRight,
            top + cornerRadius
    );

    // RIGHT SIDE
    path.lineTo(
            barRight,
            bottom - cornerRadius
    );

    // RIGHT BOTTOM CORNER
    path.quadTo(
            barRight,
            bottom,
            barRight - cornerRadius,
            bottom
    );

    // BOTTOM
    path.lineTo(
            barLeft + cornerRadius,
            bottom
    );

    // LEFT BOTTOM CORNER
    path.quadTo(
            barLeft,
            bottom,
            barLeft,
            bottom - cornerRadius
    );

    // LEFT SIDE
    path.lineTo(
            barLeft,
            top + cornerRadius
    );

    // LEFT TOP CORNER
    path.quadTo(
            barLeft,
            top,
            barLeft + cornerRadius,
            top
    );

    path.close();

    paint.reset();
    paint.setAntiAlias(true);
    paint.setColor(BAR_COLOR);
    paint.setStyle(Paint.Style.FILL);

    paint.setShadowLayer(
            dp(9),
            0,
            dp(4),
            0x22000000
    );

    canvas.drawPath(path, paint);

    paint.clearShadowLayer();
}

    private void drawNavigationIcons(Canvas canvas) {

        float leftArea = barLeft;
        float rightArea = barRight;

        float usableWidth =
                (rightArea - leftArea) / 4f;

        float y = barTop + dp(29);

        // Four side positions
        float x1 = leftArea + usableWidth * 0.55f;
        float x2 = leftArea + usableWidth * 1.35f;

        float x3 = rightArea - usableWidth * 1.35f;
        float x4 = rightArea - usableWidth * 0.55f;

        float iconSize = dp(24);

        drawIcon(
                canvas,
                iconHome,
                x1,
                y,
                iconSize,
                selected == 0 ? GREEN : INACTIVE
        );

        drawIcon(
                canvas,
                iconInventory,
                x2,
                y,
                iconSize,
                selected == 1 ? GREEN : INACTIVE
        );

        drawIcon(
                canvas,
                iconSales,
                x3,
                y,
                iconSize,
                selected == 3 ? GREEN : INACTIVE
        );

        drawIcon(
                canvas,
                iconProfile,
                x4,
                y,
                iconSize,
                selected == 4 ? GREEN : INACTIVE
        );

        // Selection dots
        if (selected == 0) {
            drawDot(canvas, x1);
        }

        if (selected == 1) {
            drawDot(canvas, x2);
        }

        if (selected == 3) {
            drawDot(canvas, x3);
        }

        if (selected == 4) {
            drawDot(canvas, x4);
        }
    }

    private void drawCenterButton(Canvas canvas) {

        paint.reset();
        paint.setAntiAlias(true);

        paint.setColor(GREEN);
        paint.setStyle(Paint.Style.FILL);

        paint.setShadowLayer(
                dp(6),
                0,
                dp(3),
                0x33000000
        );

        canvas.drawCircle(
                centerX,
                centerY,
                fabRadius,
                paint
        );

        paint.clearShadowLayer();

        drawIcon(
                canvas,
                iconPos,
                centerX,
                centerY,
                dp(28),
                Color.WHITE
        );

        if (selected == 2) {
            drawDot(canvas, centerX);
        }
    }

    /**
     * Generic drawable-based icon renderer - gumagana sa
     * PNG/WebP/vector XML. Naka-tint ayon sa selected state.
     */
    private void drawIcon(
            Canvas canvas,
            Drawable drawable,
            float centerX,
            float centerY,
            float size,
            int tintColor) {

        if (drawable == null) {
            return;
        }

        int half = (int) (size / 2);

        drawable.setBounds(
                (int) (centerX - half),
                (int) (centerY - half),
                (int) (centerX + half),
                (int) (centerY + half)
        );

        drawable.setColorFilter(
                new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        );

        drawable.draw(canvas);
    }

    private void drawDot(Canvas canvas, float x) {

        paint.reset();
        paint.setAntiAlias(true);

        paint.setColor(GREEN);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(
                x,
                barBottom - dp(8),
                dp(4),
                paint
        );
    }

    // -------------------------
    // TOUCH
    // -------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        float usableWidth =
                (barRight - barLeft) / 4f;

        float x1 = barLeft + usableWidth * 0.55f;
        float x2 = barLeft + usableWidth * 1.35f;

        float x3 = barRight - usableWidth * 1.35f;
        float x4 = barRight - usableWidth * 0.55f;

        float touchRadius = dp(30);

        // Center button
        float distance =
                (float) Math.sqrt(
                        Math.pow(x - centerX, 2) +
                        Math.pow(y - centerY, 2)
                );

        if (distance <= fabRadius + dp(8)) {

            select(2);
            return true;
        }

        if (distanceTo(x, y, x1, barTop + dp(29))
                <= touchRadius) {

            select(0);
            return true;
        }

        if (distanceTo(x, y, x2, barTop + dp(29))
                <= touchRadius) {

            select(1);
            return true;
        }

        if (distanceTo(x, y, x3, barTop + dp(29))
                <= touchRadius) {

            select(3);
            return true;
        }

        if (distanceTo(x, y, x4, barTop + dp(29))
                <= touchRadius) {

            select(4);
            return true;
        }

        return true;
    }

    private float distanceTo(
            float x1,
            float y1,
            float x2,
            float y2) {

        float dx = x1 - x2;
        float dy = y1 - y2;

        return (float) Math.sqrt(
                dx * dx + dy * dy
        );
    }

    private void select(int position) {

        selected = position;

        invalidate();

        if (listener != null) {
            listener.onNavigationSelected(position);
        }
    }
}