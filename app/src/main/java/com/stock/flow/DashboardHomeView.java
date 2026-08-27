package com.stock.flow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.stock.flow.adapter.ProductSaleAdapter;

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
 * Buong Dashboard/Home content - stateful na ngayon. Tinatawag
 * ng MainActivity ang setData() tuwing may bagong inventory o
 * sales data mula Firebase.
 */
public class DashboardHomeView {

    private final Context context;
    private final float density;

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int RED = Color.rgb(230, 70, 70);
    private final int ORANGE = Color.rgb(255, 159, 67);
    private final int PURPLE = Color.rgb(139, 92, 246);

    private List<Product> allProducts = new ArrayList<>();
    private List<SaleRecord> allSales = new ArrayList<>();

    private final Map<String, Product> productByBarcode = new LinkedHashMap<>();

    // replaced hotSaleRow with RecyclerView + adapter
    private RecyclerView hotSaleRecycler;
    private ProductSaleAdapter hotSaleAdapter;

    private PieChart categoryChart;
    private LinearLayout categoryLegendCol;
    private BarChart salesOverviewChart;
    private TextView profitValueText;
    private TextView profitDeltaText;
    private BarChart profitChart;

    private static final SimpleDateFormat DATE_FMT_DAY = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT_MONTH = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private static final SimpleDateFormat LABEL_FMT = new SimpleDateFormat("EEE", Locale.getDefault());

    public DashboardHomeView(Context context) {
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
        column.addView(spacer(20));

        column.addView(buildSearchBar());
        column.addView(spacer(24));

        column.addView(sectionTitle("Hot Sale Today", true));
        column.addView(spacer(12));

        // create RecyclerView for hot sale
        hotSaleRecycler = new RecyclerView(context);
        hotSaleRecycler.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams recyclerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hotSaleRecycler.setLayoutParams(recyclerLp);
        hotSaleRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        hotSaleAdapter = new ProductSaleAdapter();
        hotSaleRecycler.setAdapter(hotSaleAdapter);
        column.addView(hotSaleRecycler);
        column.addView(spacer(28));

        column.addView(sectionTitleWithChip("Sales Data", "This Month"));
        column.addView(spacer(12));
        column.addView(buildSalesDataRow());
        column.addView(spacer(28));

        column.addView(sectionTitleWithChip("Profit Report", "This Month"));
        column.addView(spacer(12));
        column.addView(buildProfitReportCard());

        scrollView.addView(column);

        refreshAll();

        return scrollView;
    }

    /** Tinatawag ng MainActivity tuwing may bagong data mula Firebase. */
    public void setData(List<Product> products, List<SaleRecord> sales) {
        this.allProducts = products != null ? products : new ArrayList<>();
        this.allSales = sales != null ? sales : new ArrayList<>();

        productByBarcode.clear();
        for (Product p : allProducts) {
            if (p.getBarcode() != null) {
                productByBarcode.put(p.getBarcode(), p);
            }
        }

        refreshAll();
    }

    private void refreshAll() {
        refreshHotSale();
        refreshCategoryChart();
        refreshSalesOverview();
        refreshProfitReport();
    }

    // -------------------------
    // HEADER
    // -------------------------

    private View buildHeader() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ic_stockflow_logo);
        logo.setAdjustViewBounds(true);

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                0, dp(64), 1f
        );
        logo.setLayoutParams(logoParams);
        logo.setScaleType(ImageView.ScaleType.FIT_START);

        row.addView(logo);

        FrameLikeBell bell = new FrameLikeBell(context);
        row.addView(bell.view);

        return row;
    }

    private static class FrameLikeBell {
        View view;

        FrameLikeBell(Context context) {
            FrameLayout wrap = new FrameLayout(context);

            float density = context.getResources().getDisplayMetrics().density;
            int size = (int) (44 * density);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.WHITE);
            wrap.setBackground(bg);
            wrap.setElevation(4 * density);

            IconViews.IconView bell = new IconViews.IconView(
                    context, IconViews.TYPE_BELL, Color.rgb(27, 42, 74)
            );

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

            wrap.addView(bell, params);
            wrap.setLayoutParams(new LinearLayout.LayoutParams(size, size));

            view = wrap;
        }
    }

    // -------------------------
    // SEARCH BAR
    // -------------------------

    private View buildSearchBar() {

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), dp(6), dp(16), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(28));
        bar.setBackground(bg);
        bar.setElevation(dp(3));

        FrameLayout searchBtn = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(BLUE);
        searchBtn.setBackground(circleBg);

        IconViews.IconView searchIcon = new IconViews.IconView(
                context, IconViews.TYPE_SEARCH, Color.WHITE
        );

        int circleSize = dp(40);
        searchBtn.addView(searchIcon, new FrameLayout.LayoutParams(circleSize, circleSize));
        bar.addView(searchBtn, new LinearLayout.LayoutParams(circleSize, circleSize));

        TextView hint = new TextView(context);
        hint.setText("Need Something? Search products, sales, reports\u2026");
        hint.setTextSize(13);
        hint.setTextColor(GRAY_TEXT);
        hint.setMaxLines(1);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        hintParams.leftMargin = dp(12);
        bar.addView(hint, hintParams);

        IconViews.IconView filterIcon = new IconViews.IconView(
                context, IconViews.TYPE_FILTER, BLUE
        );
        bar.addView(filterIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        return bar;
    }

    // -------------------------
    // SECTION TITLES
    // -------------------------

    private View sectionTitle(String text, boolean withChevron) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(17);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(NAVY);
        row.addView(label);

        if (withChevron) {
            IconViews.IconView chevron = new IconViews.IconView(
                    context, IconViews.TYPE_CHEVRON, NAVY
            );
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(16), dp(16));
            p.leftMargin = dp(4);
            row.addView(chevron, p);
        }

        View spacer = new View(context);
        row.addView(spacer, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView viewAll = new TextView(context);
        viewAll.setText("View All");
        viewAll.setTextSize(13);
        viewAll.setTextColor(BLUE);
        row.addView(viewAll);

        return row;
    }

    private View sectionTitleWithChip(String text, String chipText) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(17);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(NAVY);
        row.addView(label);

        IconViews.IconView chevron = new IconViews.IconView(
                context, IconViews.TYPE_CHEVRON, NAVY
        );
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(16), dp(16));
        cp.leftMargin = dp(4);
        row.addView(chevron, cp);

        View spacer = new View(context);
        row.addView(spacer, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView chip = new TextView(context);
        chip.setText(chipText + "  \u25BE");
        chip.setTextSize(12);
        chip.setTextColor(NAVY);
        chip.setPadding(dp(14), dp(6), dp(14), dp(6));

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(Color.WHITE);
        chipBg.setCornerRadius(dp(16));
        chip.setBackground(chipBg);

        row.addView(chip);

        return row;
    }

    // -------------------------
    // HOT SALE TODAY (totoong data)
    // -------------------------

    private void refreshHotSale() {

        if (hotSaleRecycler == null || hotSaleAdapter == null) {
            return;
        }

        SimpleDateFormat dateFmt = DATE_FMT_DAY;
        String today = dateFmt.format(new java.util.Date());

        Map<String, Long> qtyByBarcode = new LinkedHashMap<>();

        for (SaleRecord sale : allSales) {
            if (sale.getDate() == null || !sale.getDate().equals(today)) {
                continue;
            }
            for (SaleItem item : sale.getItems()) {
                if (item.getBarcode() == null) {
                    continue;
                }
                qtyByBarcode.put(
                        item.getBarcode(),
                        qtyByBarcode.getOrDefault(item.getBarcode(), 0L) + item.getQty()
                );
            }
        }

        if (qtyByBarcode.isEmpty()) {
            // show a single empty text in the RecyclerView via adapter
            List<ProductSaleAdapter.Item> list = new ArrayList<>();
            hotSaleAdapter.updateItems(list);
            return;
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(qtyByBarcode.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return Long.compare(b.getValue(), a.getValue());
            }
        });

        List<ProductSaleAdapter.Item> toShow = new ArrayList<>();
        int displayed = 0;
        for (Map.Entry<String, Long> e : sorted) {
            if (displayed >= 4) break;
            String barcode = e.getKey();
            long qtySold = e.getValue();
            Product product = productByBarcode.get(barcode);
            if (product == null) continue;
            int rank = displayed + 1;
            toShow.add(new ProductSaleAdapter.Item(rank, product, qtySold));
            displayed++;
        }

        hotSaleAdapter.updateItems(toShow);
    }

    private View buildProductCard(int rank, Product product, long qtySold) {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setElevation(dp(2));

        FrameLayout imageWrap = new FrameLayout(context);

        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(Color.rgb(230, 234, 240));
        boxBg.setCornerRadius(dp(10));

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackground(boxBg);
            imageView.setPadding(dp(4), dp(4), dp(4), dp(4));
            Glide.with(imageView).load(product.getImagePath())
                    .placeholder(boxBg).error(boxBg).into(imageView);
            imageWrap.addView(imageView, new FrameLayout.LayoutParams(dp(118), dp(90)));
        } else {
            View placeholderBox = new View(context);
            placeholderBox.setBackground(boxBg);
            imageWrap.addView(placeholderBox, new FrameLayout.LayoutParams(dp(118), dp(90)));
        }

        TextView rankBadge = new TextView(context);
        rankBadge.setText(String.valueOf(rank));
        rankBadge.setTextColor(Color.WHITE);
        rankBadge.setTextSize(11);
        rankBadge.setGravity(Gravity.CENTER);

        GradientDrawable rankBg = new GradientDrawable();
        rankBg.setColor(BLUE);
        rankBg.setCornerRadius(dp(8));
        rankBadge.setBackground(rankBg);

        FrameLayout.LayoutParams rankParams = new FrameLayout.LayoutParams(dp(20), dp(20));
        rankParams.topMargin = dp(4);
        rankParams.leftMargin = dp(4);
        imageWrap.addView(rankBadge, rankParams);

        card.addView(imageWrap);
        card.addView(spacer(8));

        TextView nameView = new TextView(context);
        nameView.setText(product.getName());
        nameView.setTextSize(13);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        card.addView(nameView);

        TextView priceView = new TextView(context);
        priceView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", product.getSellingPrice()));
        priceView.setTextSize(13);
        priceView.setTextColor(BLUE);
        priceView.setTypeface(null, Typeface.BOLD);
        card.addView(priceView);

        TextView statsView = new TextView(context);
        statsView.setText(qtySold + " sold today");
        statsView.setTextSize(11);
        statsView.setTextColor(GREEN);
        card.addView(statsView);

        return card;
    }

    // -------------------------
    // SALES DATA (category pie + overview bar)
    // -------------------------

    private View buildSalesDataRow() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        View pieCard = buildSalesByCategoryCard();
        View barCard = buildSalesOverviewCard();

        LinearLayout.LayoutParams pieParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        pieParams.rightMargin = dp(10);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );

        row.addView(pieCard, pieParams);
        row.addView(barCard, barParams);

        return row;
    }

    private View buildSalesByCategoryCard() {

        LinearLayout card = cardContainer();

        card.addView(cardLabel("Sales by Category"));
        card.addView(spacer(8));

        categoryChart = new PieChart(context);
        categoryChart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
        ));

        categoryChart.setDrawHoleEnabled(true);
        categoryChart.setHoleRadius(62f);
        categoryChart.setTransparentCircleRadius(0f);
        categoryChart.setHoleColor(Color.WHITE);
        categoryChart.setCenterTextSize(11f);
        categoryChart.setCenterTextColor(NAVY);
        categoryChart.getDescription().setEnabled(false);
        categoryChart.getLegend().setEnabled(false);
        categoryChart.setDrawEntryLabels(false);
        categoryChart.setRotationEnabled(false);
        categoryChart.setTouchEnabled(false);

        card.addView(categoryChart);
        card.addView(spacer(10));

        categoryLegendCol = new LinearLayout(context);
        categoryLegendCol.setOrientation(LinearLayout.VERTICAL);
        card.addView(categoryLegendCol);

        return card;
    }

    private void refreshCategoryChart() {

        if (categoryChart == null) {
            return;
        }

        List<SaleRecord> monthSales = salesThisMonth();

        Map<String, Double> byCategory = new LinkedHashMap<>();
        double total = 0;

        for (SaleRecord sale : monthSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getBarcode() != null ? productByBarcode.get(item.getBarcode()) : null;
                String category = (product != null && product.getCategory() != null)
                        ? product.getCategory() : "Others";
                byCategory.put(category, byCategory.getOrDefault(category, 0.0) + item.getSubtotal());
                total += item.getSubtotal();
            }
        }

        categoryLegendCol.removeAllViews();

        if (byCategory.isEmpty() || total <= 0) {
            categoryChart.setCenterText("Total Sales\n\u20B10.00");
            categoryChart.setData(null);
            categoryChart.invalidate();

            TextView empty = new TextView(context);
            empty.setText("Wala pang benta ngayong buwan");
            empty.setTextSize(12);
            empty.setTextColor(GRAY_TEXT);
            categoryLegendCol.addView(empty);
            return;
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(byCategory.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int[] palette = new int[]{BLUE, Color.rgb(30, 80, 200), PURPLE, ORANGE, GRAY_TEXT};

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int shown = Math.min(sorted.size(), 5);
        for (int i = 0; i < shown; i++) {
            String category = sorted.get(i).getKey();
            double amount = sorted.get(i).getValue();

            entries.add(new PieEntry((float) amount, category));
            int color = palette[i % palette.length];
            colors.add(color);

            int percent = (int) Math.round((amount / total) * 100);
            categoryLegendCol.addView(legendRow(color, category, percent + "%"));
        }

        categoryChart.setCenterText("Total Sales\n\u20B1" + String.format(Locale.getDefault(), "%.2f", total));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        categoryChart.setData(new PieData(dataSet));
        categoryChart.invalidate();
    }

    private View buildSalesOverviewCard() {

        LinearLayout card = cardContainer();

        card.addView(cardLabel("Sales Overview"));
        card.addView(spacer(8));

        salesOverviewChart = new BarChart(context);
        salesOverviewChart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180)
        ));

        salesOverviewChart.getDescription().setEnabled(false);
        salesOverviewChart.getLegend().setEnabled(false);
        salesOverviewChart.setTouchEnabled(false);
        salesOverviewChart.setDrawGridBackground(false);
        salesOverviewChart.getAxisRight().setEnabled(false);
        salesOverviewChart.getAxisLeft().setTextColor(GRAY_TEXT);
        salesOverviewChart.getAxisLeft().setDrawGridLines(true);
        salesOverviewChart.getAxisLeft().setAxisMinimum(0f);

        card.addView(salesOverviewChart);

        return card;
    }

    private void refreshSalesOverview() {

        if (salesOverviewChart == null) {
            return;
        }

        Map<String, Double> byDate = last7DaysMap();
        SimpleDateFormat labelFmt = LABEL_FMT;
        SimpleDateFormat keyFmt = DATE_FMT_DAY;

        for (SaleRecord sale : allSales) {
            if (sale.getDate() != null && byDate.containsKey(sale.getDate())) {
                byDate.put(sale.getDate(), byDate.get(sale.getDate()) + sale.getTotalAmount());
            }
        }

        String[] labels = new String[7];
        List<BarEntry> entries = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        int i = 0;
        for (String key : byDate.keySet()) {
            labels[i] = labelFmt.format(cal.getTime());
            entries.add(new BarEntry(i, byDate.get(key).floatValue()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(BLUE);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);
        salesOverviewChart.setData(barData);

        XAxis xAxis = salesOverviewChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(GRAY_TEXT);

        salesOverviewChart.invalidate();
    }

    // -------------------------
    // PROFIT REPORT
    // -------------------------

    private View buildProfitReportCard() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(222, 233, 250));
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        LinearLayout left = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText("Total Profit");
        label.setTextSize(13);
        label.setTextColor(NAVY);
        left.addView(label);

        profitValueText = new TextView(context);
        profitValueText.setText("\u20B10.00");
        profitValueText.setTextSize(22);
        profitValueText.setTypeface(null, Typeface.BOLD);
        profitValueText.setTextColor(BLUE);
        left.addView(profitValueText);

        profitDeltaText = new TextView(context);
        profitDeltaText.setText("--% vs Last Month");
        profitDeltaText.setTextSize(11);
        profitDeltaText.setTextColor(GREEN);
        left.addView(profitDeltaText);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f
        );
        card.addView(left, leftParams);

        profitChart = new BarChart(context);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                0, dp(160), 1.3f
        );
        profitChart.setLayoutParams(chartParams);

        profitChart.getDescription().setEnabled(false);
        profitChart.getLegend().setEnabled(false);
        profitChart.setTouchEnabled(false);
        profitChart.setDrawGridBackground(false);
        profitChart.getAxisRight().setEnabled(false);
        profitChart.getAxisLeft().setTextColor(GRAY_TEXT);
        profitChart.getAxisLeft().setAxisMinimum(0f);

        card.addView(profitChart);

        return card;
    }

    private void refreshProfitReport() {

        if (profitChart == null) {
            return;
        }

        List<SaleRecord> monthSales = salesThisMonth();
        double monthProfit = 0;
        for (SaleRecord s : monthSales) {
            monthProfit += s.getTotalProfit();
        }

        List<SaleRecord> lastMonthSales = salesLastMonth();
        double lastMonthProfit = 0;
        for (SaleRecord s : lastMonthSales) {
            lastMonthProfit += s.getTotalProfit();
        }

        profitValueText.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", monthProfit));

        if (lastMonthProfit > 0) {
            double change = ((monthProfit - lastMonthProfit) / lastMonthProfit) * 100;
            String sign = change >= 0 ? "\u25B2 " : "\u25BC ";
            profitDeltaText.setText(sign + String.format(Locale.getDefault(), "%.0f", Math.abs(change))
                    + "% vs Last Month");
            profitDeltaText.setTextColor(change >= 0 ? GREEN : RED);
        } else {
            profitDeltaText.setText("Walang datos noong nakaraang buwan");
            profitDeltaText.setTextColor(GRAY_TEXT);
        }

        Map<String, Double> byDate = last7DaysMap();
        for (SaleRecord sale : allSales) {
            if (sale.getDate() != null && byDate.containsKey(sale.getDate())) {
                byDate.put(sale.getDate(), byDate.get(sale.getDate()) + sale.getTotalProfit());
            }
        }

        SimpleDateFormat labelFmt = LABEL_FMT;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        String[] labels = new String[7];
        List<BarEntry> entries = new ArrayList<>();

        int i = 0;
        for (String key : byDate.keySet()) {
            labels[i] = labelFmt.format(cal.getTime());
            entries.add(new BarEntry(i, byDate.get(key).floatValue()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(BLUE);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        profitChart.setData(barData);

        XAxis xAxis = profitChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(GRAY_TEXT);
        xAxis.setTextSize(10f);

        profitChart.invalidate();
    }

    // -------------------------
    // HELPERS
    // -------------------------

    private Map<String, Double> last7DaysMap() {

        Map<String, Double> map = new LinkedHashMap<>();
        SimpleDateFormat keyFmt = DATE_FMT_DAY;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            map.put(keyFmt.format(cal.getTime()), 0.0);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return map;
    }

    private List<SaleRecord> salesThisMonth() {

        String currentMonth = DATE_FMT_MONTH.format(new java.util.Date());

        List<SaleRecord> result = new ArrayList<>();
        for (SaleRecord s : allSales) {
            if (s.getDate() != null && s.getDate().startsWith(currentMonth)) {
                result.add(s);
            }
        }
        return result;
    }

    private List<SaleRecord> salesLastMonth() {

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        String lastMonth = DATE_FMT_MONTH.format(cal.getTime());

        List<SaleRecord> result = new ArrayList<>();
        for (SaleRecord s : allSales) {
            if (s.getDate() != null && s.getDate().startsWith(lastMonth)) {
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

    private View legendRow(int color, String label, String percent) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        View dot = new View(context);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        dot.setBackground(dotBg);

        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotParams.rightMargin = dp(8);
        row.addView(dot, dotParams);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(NAVY);
        labelView.setMaxLines(1);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(labelView, labelParams);

        if (percent != null) {
            TextView percentView = new TextView(context);
            percentView.setText(percent);
            percentView.setTextSize(12);
            percentView.setTypeface(null, Typeface.BOLD);
            percentView.setTextColor(NAVY);
            row.addView(percentView);
        }

        return row;
    }

    private View spacer(int heightDp) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }
}
