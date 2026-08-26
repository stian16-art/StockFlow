package com.stock.flow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sales Report screen - stateful. Tinatawag ng SalesFragment ang
 * setSales() tuwing may bagong data mula Firebase, at dito na
 * kino-compute lahat ng stats/charts mula sa totoong records.
 */
public class SalesHomeView {

    private final Context context;
    private final float density;

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int ORANGE = Color.rgb(255, 159, 67);
    private final int PURPLE = Color.rgb(139, 92, 246);
    private final int LIGHT_BORDER = Color.rgb(225, 231, 240);

    private List<SaleRecord> allSales = new ArrayList<>();

    private TextView totalSalesValue;
    private TextView ordersValue;
    private TextView avgOrderValue;

    private BarChart trendChart;
    private PieChart paymentChart;
    private LinearLayout paymentLegendCol;

    private LinearLayout transactionsListContainer;

    public SalesHomeView(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    public View build() {

        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(PAGE_BG);
        scrollView.setFillViewport(true);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(20), dp(24), dp(20), dp(140));

        column.addView(buildHeader());
        column.addView(spacer(16));

        column.addView(buildStatsRow());
        column.addView(spacer(16));

        column.addView(buildTrendCard());
        column.addView(spacer(16));

        column.addView(buildPaymentBreakdownCard());
        column.addView(spacer(20));

        column.addView(sectionTitle("Recent Transactions"));
        column.addView(spacer(10));

        transactionsListContainer = cardContainer();
        column.addView(transactionsListContainer);

        scrollView.addView(column);

        refreshAll();

        return scrollView;
    }

    /** Tinatawag ng Fragment tuwing may bagong data mula Firebase. */
    public void setSales(List<SaleRecord> sales) {
        this.allSales = sales != null ? sales : new ArrayList<>();
        refreshAll();
    }

    private void refreshAll() {
        refreshStats();
        refreshTrendChart();
        refreshPaymentBreakdown();
        refreshTransactionsList();
    }

    // -------------------------
    // HEADER
    // -------------------------

    private View buildHeader() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Sales Report");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(title, titleParams);

        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(14), dp(9), dp(14), dp(9));

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(Color.WHITE);
        chipBg.setCornerRadius(dp(16));
        chip.setBackground(chipBg);
        chip.setElevation(dp(2));

        IconViews.IconView calendarIcon = new IconViews.IconView(
                context, IconViews.TYPE_CALENDAR, BLUE
        );
        chip.addView(calendarIcon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView chipText = new TextView(context);
        chipText.setText("This Month");
        chipText.setTextSize(13);
        chipText.setTypeface(null, Typeface.BOLD);
        chipText.setTextColor(NAVY);
        LinearLayout.LayoutParams chipTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chipTextParams.leftMargin = dp(6);
        chip.addView(chipText, chipTextParams);

        row.addView(chip);

        return row;
    }

    // -------------------------
    // STATS ROW
    // -------------------------

    private View buildStatsRow() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        View salesCard = statCard(IconViews.TYPE_WALLET, "Total Sales", BLUE);
        totalSalesValue = (TextView) ((LinearLayout) salesCard).findViewWithTag("value");
        row.addView(salesCard, rowParams(true));

        View ordersCard = statCard(IconViews.TYPE_BAG, "Orders", GREEN);
        ordersValue = (TextView) ((LinearLayout) ordersCard).findViewWithTag("value");
        row.addView(ordersCard, rowParams(true));

        View avgCard = statCard(IconViews.TYPE_TAG, "Avg. Order", ORANGE);
        avgOrderValue = (TextView) ((LinearLayout) avgCard).findViewWithTag("value");
        row.addView(avgCard, rowParams(false));

        return row;
    }

    private LinearLayout.LayoutParams rowParams(boolean withMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        if (withMargin) {
            p.rightMargin = dp(8);
        }
        return p;
    }

    private View statCard(int iconType, String label, int accentColor) {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(14), dp(12), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setElevation(dp(1));

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(withAlpha(accentColor, 30));
        iconWrap.setBackground(circleBg);

        IconViews.IconView icon = new IconViews.IconView(context, iconType, accentColor);
        int iconSize = dp(16);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        int wrapSize = dp(32);
        card.addView(iconWrap, new LinearLayout.LayoutParams(wrapSize, wrapSize));
        card.addView(spacer(8));

        TextView valueView = new TextView(context);
        valueView.setTag("value");
        valueView.setText("0");
        valueView.setTextSize(15);
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setTextColor(NAVY);
        valueView.setMaxLines(1);
        card.addView(valueView);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(10.5f);
        labelView.setTextColor(GRAY_TEXT);
        card.addView(labelView);

        return card;
    }

    private void refreshStats() {

        List<SaleRecord> monthSales = salesThisMonth();

        double totalAmount = 0;
        for (SaleRecord s : monthSales) {
            totalAmount += s.getTotalAmount();
        }

        int orders = monthSales.size();
        double avgOrder = orders > 0 ? totalAmount / orders : 0;

        if (totalSalesValue != null) {
            totalSalesValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", totalAmount));
        }
        if (ordersValue != null) {
            ordersValue.setText(String.valueOf(orders));
        }
        if (avgOrderValue != null) {
            avgOrderValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", avgOrder));
        }
    }

    private int withAlpha(int color, int alpha255) {
        return Color.argb(
                alpha255, Color.red(color), Color.green(color), Color.blue(color)
        );
    }

    // -------------------------
    // SALES TREND (huling 7 araw)
    // -------------------------

    private View buildTrendCard() {

        LinearLayout card = cardContainer();

        card.addView(cardLabel("Sales Trend"));
        card.addView(spacer(10));

        trendChart = new BarChart(context);
        trendChart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180)
        ));

        trendChart.getDescription().setEnabled(false);
        trendChart.getLegend().setEnabled(false);
        trendChart.setTouchEnabled(false);
        trendChart.setDrawGridBackground(false);
        trendChart.getAxisRight().setEnabled(false);
        trendChart.getAxisLeft().setTextColor(GRAY_TEXT);
        trendChart.getAxisLeft().setDrawGridLines(true);
        trendChart.getAxisLeft().setAxisMinimum(0f);

        card.addView(trendChart);

        return card;
    }

    private void refreshTrendChart() {

        if (trendChart == null) {
            return;
        }

        String[] days = new String[7];
        float[] totals = new float[7];

        Map<String, Double> byDate = new LinkedHashMap<>();
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEE", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            String key = keyFmt.format(cal.getTime());
            days[i] = labelFmt.format(cal.getTime());
            byDate.put(key, 0.0);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        for (SaleRecord s : allSales) {
            if (s.getDate() != null && byDate.containsKey(s.getDate())) {
                byDate.put(s.getDate(), byDate.get(s.getDate()) + s.getTotalAmount());
            }
        }

        int i = 0;
        for (Double v : byDate.values()) {
            totals[i] = v.floatValue();
            i++;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int j = 0; j < totals.length; j++) {
            entries.add(new BarEntry(j, totals[j]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(BLUE);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);
        trendChart.setData(barData);

        XAxis xAxis = trendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setTextColor(GRAY_TEXT);

        trendChart.invalidate();
    }

    // -------------------------
    // PAYMENT METHOD BREAKDOWN
    // -------------------------

    private View buildPaymentBreakdownCard() {

        LinearLayout card = cardContainer();

        card.addView(cardLabel("Payment Methods"));
        card.addView(spacer(10));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        paymentChart = new PieChart(context);
        paymentChart.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(120)));

        paymentChart.setDrawHoleEnabled(true);
        paymentChart.setHoleRadius(58f);
        paymentChart.setTransparentCircleRadius(0f);
        paymentChart.setHoleColor(Color.WHITE);
        paymentChart.getDescription().setEnabled(false);
        paymentChart.getLegend().setEnabled(false);
        paymentChart.setDrawEntryLabels(false);
        paymentChart.setRotationEnabled(false);
        paymentChart.setTouchEnabled(false);

        row.addView(paymentChart);

        paymentLegendCol = new LinearLayout(context);
        paymentLegendCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        legendParams.leftMargin = dp(16);

        row.addView(paymentLegendCol, legendParams);

        card.addView(row);

        return card;
    }

    private void refreshPaymentBreakdown() {

        if (paymentChart == null) {
            return;
        }

        List<SaleRecord> monthSales = salesThisMonth();

        Map<String, Double> byMethod = new LinkedHashMap<>();
        double total = 0;

        for (SaleRecord s : monthSales) {
            String method = s.getPaymentMethod() != null ? s.getPaymentMethod() : "Cash";
            byMethod.put(method, byMethod.getOrDefault(method, 0.0) + s.getTotalAmount());
            total += s.getTotalAmount();
        }

        paymentLegendCol.removeAllViews();

        if (byMethod.isEmpty() || total <= 0) {
            paymentChart.setData(null);
            paymentChart.invalidate();

            TextView empty = new TextView(context);
            empty.setText("Wala pang benta ngayong buwan");
            empty.setTextSize(12);
            empty.setTextColor(GRAY_TEXT);
            paymentLegendCol.addView(empty);
            return;
        }

        int[] palette = new int[]{BLUE, PURPLE, ORANGE, GREEN};

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int colorIndex = 0;
        for (Map.Entry<String, Double> entry : byMethod.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            int color = palette[colorIndex % palette.length];
            colors.add(color);

            int percent = (int) Math.round((entry.getValue() / total) * 100);
            paymentLegendCol.addView(legendRow(color, entry.getKey(), percent + "%"));

            colorIndex++;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        paymentChart.setData(new PieData(dataSet));
        paymentChart.invalidate();
    }

    private View legendRow(int color, String label, String percent) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        View dot = new View(context);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotParams.rightMargin = dp(8);
        dot.setBackground(dotBg);
        row.addView(dot, dotParams);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(NAVY);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(labelView, labelParams);

        TextView percentView = new TextView(context);
        percentView.setText(percent);
        percentView.setTextSize(12);
        percentView.setTypeface(null, Typeface.BOLD);
        percentView.setTextColor(NAVY);
        row.addView(percentView);

        return row;
    }

    // -------------------------
    // TRANSACTIONS LIST
    // -------------------------

    private void refreshTransactionsList() {

        if (transactionsListContainer == null) {
            return;
        }

        transactionsListContainer.removeAllViews();

        List<SaleRecord> sorted = new ArrayList<>(allSales);
        Collections.sort(sorted, new Comparator<SaleRecord>() {
            @Override
            public int compare(SaleRecord a, SaleRecord b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });

        if (sorted.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Wala pang transaksyon");
            empty.setTextSize(13);
            empty.setTextColor(GRAY_TEXT);
            empty.setPadding(0, dp(8), 0, dp(8));
            transactionsListContainer.addView(empty);
            return;
        }

        int limit = Math.min(sorted.size(), 8);

        for (int i = 0; i < limit; i++) {
            transactionsListContainer.addView(transactionRow(sorted.get(i)));
            if (i < limit - 1) {
                transactionsListContainer.addView(divider());
            }
        }
    }

    private View transactionRow(SaleRecord sale) {

        int paymentColor = paymentColorFor(sale.getPaymentMethod());

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(withAlpha(paymentColor, 28));
        iconWrap.setBackground(circleBg);

        IconViews.IconView icon = new IconViews.IconView(context, IconViews.TYPE_CART, paymentColor);
        int iconSize = dp(18);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        int wrapSize = dp(36);
        row.addView(iconWrap, new LinearLayout.LayoutParams(wrapSize, wrapSize));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textColParams.leftMargin = dp(12);

        int itemCount = sale.getItems() != null ? sale.getItems().size() : 0;

        TextView itemsView = new TextView(context);
        itemsView.setText(itemCount + (itemCount == 1 ? " item" : " items"));
        itemsView.setTextSize(13);
        itemsView.setTypeface(null, Typeface.BOLD);
        itemsView.setTextColor(NAVY);
        textCol.addView(itemsView);

        TextView timeView = new TextView(context);
        timeView.setText(formatDateTime(sale));
        timeView.setTextSize(11);
        timeView.setTextColor(GRAY_TEXT);
        textCol.addView(timeView);

        row.addView(textCol, textColParams);

        TextView amountView = new TextView(context);
        amountView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", sale.getTotalAmount()));
        amountView.setTextSize(14);
        amountView.setTypeface(null, Typeface.BOLD);
        amountView.setTextColor(NAVY);
        row.addView(amountView);

        return row;
    }

    private int paymentColorFor(String method) {
        if (method == null) {
            return BLUE;
        }
        switch (method) {
            case "GCash":
                return PURPLE;
            case "Card":
                return ORANGE;
            default:
                return BLUE;
        }
    }

    private String formatDateTime(SaleRecord sale) {

        try {
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayKey = keyFmt.format(new java.util.Date());

            Calendar yesterdayCal = Calendar.getInstance();
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterdayKey = keyFmt.format(yesterdayCal.getTime());

            String timeLabel = sale.getTime() != null ? sale.getTime() : "";

            if (sale.getDate() != null && sale.getDate().equals(todayKey)) {
                return "Today, " + timeLabel;
            } else if (sale.getDate() != null && sale.getDate().equals(yesterdayKey)) {
                return "Yesterday, " + timeLabel;
            } else {
                return sale.getDate() + ", " + timeLabel;
            }
        } catch (Exception e) {
            return sale.getDate() + " " + sale.getTime();
        }
    }

    // -------------------------
    // HELPERS
    // -------------------------

    private List<SaleRecord> salesThisMonth() {

        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonth = monthFmt.format(new java.util.Date());

        List<SaleRecord> result = new ArrayList<>();
        for (SaleRecord s : allSales) {
            if (s.getDate() != null && s.getDate().startsWith(currentMonth)) {
                result.add(s);
            }
        }
        return result;
    }

    private LinearLayout cardContainer() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        card.setElevation(dp(2));

        return card;
    }

    private TextView cardLabel(String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(14);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(NAVY);
        return label;
    }

    private View sectionTitle(String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(16);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(NAVY);
        return label;
    }

    private View divider() {
        View line = new View(context);
        line.setBackgroundColor(LIGHT_BORDER);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ));
        return line;
    }

    private View spacer(int heightDp) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }
}
