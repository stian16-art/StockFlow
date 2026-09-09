package com.stock.flow;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dedicated Profit Report screen - Total Profit (this month),
 * profit trend chart (last 7 days), and top profitable products
 * ranked by total profit contributed across all sales.
 */
public class ProfitReportActivity extends AppCompatActivity {

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int RED = Color.rgb(230, 70, 70);

    private float density;

    private List<SaleRecord> allSales = new ArrayList<>();
    private DatabaseReference salesRef;
    private ValueEventListener salesListener;

    private TextView totalProfitValue;
    private TextView profitDeltaValue;
    private BarChart trendChart;
    private LinearLayout topProductsContainer;

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        density = getResources().getDisplayMetrics().density;

        setContentView(buildContent());

        startListeningToSales();
    }

    // -------------------------
    // UI
    // -------------------------

    private View buildContent() {

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(PAGE_BG);
        scrollView.setFillViewport(true);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(20), dp(24), dp(20), dp(40));

        column.addView(buildHeader());
        column.addView(spacer(18));

        column.addView(buildTotalProfitCard());
        column.addView(spacer(18));

        column.addView(buildTrendCard());
        column.addView(spacer(20));

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("Top Profitable Products");
        sectionTitle.setTextSize(16);
        sectionTitle.setTypeface(null, Typeface.BOLD);
        sectionTitle.setTextColor(NAVY);
        column.addView(sectionTitle);
        column.addView(spacer(10));

        topProductsContainer = new LinearLayout(this);
        topProductsContainer.setOrientation(LinearLayout.VERTICAL);
        column.addView(topProductsContainer);

        scrollView.addView(column);

        refreshAll();

        return scrollView;
    }

    private View buildHeader() {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout backBtn = new FrameLayout(this);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setShape(GradientDrawable.OVAL);
        backBg.setColor(Color.WHITE);
        backBtn.setBackground(backBg);
        backBtn.setElevation(dp(3));

        IconViews.IconView backIcon = new IconViews.IconView(this, IconViews.TYPE_CHEVRON, NAVY);
        backIcon.setRotation(180f);
        int backIconSize = dp(16);
        backBtn.addView(backIcon, new FrameLayout.LayoutParams(
                backIconSize, backIconSize, Gravity.CENTER
        ));

        int backSize = dp(38);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(backSize, backSize);
        backParams.rightMargin = dp(14);
        backBtn.setLayoutParams(backParams);

        backBtn.setOnClickListener(v -> finish());
        row.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("Profit Report");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        row.addView(title);

        return row;
    }

    private View buildTotalProfitCard() {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(222, 233, 250));
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        TextView label = new TextView(this);
        label.setText("Total Profit (This Month)");
        label.setTextSize(13);
        label.setTextColor(NAVY);
        card.addView(label);

        totalProfitValue = new TextView(this);
        totalProfitValue.setText("\u20B10.00");
        totalProfitValue.setTextSize(28);
        totalProfitValue.setTypeface(null, Typeface.BOLD);
        totalProfitValue.setTextColor(BLUE);
        card.addView(totalProfitValue);

        profitDeltaValue = new TextView(this);
        profitDeltaValue.setText("-- vs last month");
        profitDeltaValue.setTextSize(11);
        profitDeltaValue.setTextColor(GRAY_TEXT);
        card.addView(profitDeltaValue);

        return card;
    }

    private View buildTrendCard() {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        card.setElevation(dp(2));

        TextView label = new TextView(this);
        label.setText("Profit Trend (Last 7 Days)");
        label.setTextSize(14);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(NAVY);
        card.addView(label);
        card.addView(spacer(10));

        trendChart = new BarChart(this);
        trendChart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180)
        ));

        trendChart.getDescription().setEnabled(false);
        trendChart.getLegend().setEnabled(false);
        trendChart.setTouchEnabled(false);
        trendChart.setDrawGridBackground(false);
        trendChart.getAxisRight().setEnabled(false);
        trendChart.getAxisLeft().setTextColor(GRAY_TEXT);
        trendChart.getAxisLeft().setAxisMinimum(0f);

        card.addView(trendChart);

        return card;
    }

    // -------------------------
    // FIREBASE
    // -------------------------

    private void startListeningToSales() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        salesRef = FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("sales");

        salesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<SaleRecord> sales = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SaleRecord sale = child.getValue(SaleRecord.class);
                    if (sale != null) {
                        sales.add(sale);
                    }
                }

                allSales = sales;
                refreshAll();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        ProfitReportActivity.this,
                        "Unable to load sales: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        salesRef.addValueEventListener(salesListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (salesRef != null && salesListener != null) {
            salesRef.removeEventListener(salesListener);
        }
    }

    // -------------------------
    // COMPUTE + REFRESH
    // -------------------------

    private void refreshAll() {
        refreshTotalProfit();
        refreshTrendChart();
        refreshTopProducts();
    }

    private void refreshTotalProfit() {

        if (totalProfitValue == null) {
            return;
        }

        List<SaleRecord> monthSales = salesInMonth(0);
        double monthProfit = 0;
        for (SaleRecord s : monthSales) {
            monthProfit += s.getTotalProfit();
        }

        List<SaleRecord> lastMonthSales = salesInMonth(-1);
        double lastMonthProfit = 0;
        for (SaleRecord s : lastMonthSales) {
            lastMonthProfit += s.getTotalProfit();
        }

        totalProfitValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", monthProfit));

        if (lastMonthProfit > 0) {
            double change = ((monthProfit - lastMonthProfit) / lastMonthProfit) * 100;
            String sign = change >= 0 ? "\u25B2 " : "\u25BC ";
            profitDeltaValue.setText(sign + String.format(Locale.getDefault(), "%.0f", Math.abs(change))
                    + "% vs last month");
            profitDeltaValue.setTextColor(change >= 0 ? GREEN : RED);
        } else {
            profitDeltaValue.setText("No data for last month");
            profitDeltaValue.setTextColor(GRAY_TEXT);
        }
    }

    private void refreshTrendChart() {

        if (trendChart == null) {
            return;
        }

        Map<String, Double> byDate = new LinkedHashMap<>();
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEE", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            String key = keyFmt.format(cal.getTime());
            labels[i] = labelFmt.format(cal.getTime());
            byDate.put(key, 0.0);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        for (SaleRecord sale : allSales) {
            if (sale.getDate() != null && byDate.containsKey(sale.getDate())) {
                byDate.put(sale.getDate(), byDate.get(sale.getDate()) + sale.getTotalProfit());
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        int i = 0;
        for (Double v : byDate.values()) {
            entries.add(new BarEntry(i, v.floatValue()));
            i++;
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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(GRAY_TEXT);

        trendChart.invalidate();
    }

    private void refreshTopProducts() {

        if (topProductsContainer == null) {
            return;
        }

        topProductsContainer.removeAllViews();

        Map<String, Double> profitByProduct = new LinkedHashMap<>();
        Map<String, String> nameByBarcode = new LinkedHashMap<>();
        Map<String, Long> unitsByBarcode = new LinkedHashMap<>();

        for (SaleRecord sale : allSales) {
            for (SaleItem item : sale.getItems()) {
                if (item.getBarcode() == null) {
                    continue;
                }
                profitByProduct.put(
                        item.getBarcode(),
                        profitByProduct.getOrDefault(item.getBarcode(), 0.0) + item.getProfit()
                );
                unitsByBarcode.put(
                        item.getBarcode(),
                        unitsByBarcode.getOrDefault(item.getBarcode(), 0L) + item.getQty()
                );
                nameByBarcode.put(item.getBarcode(), item.getName());
            }
        }

        if (profitByProduct.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No sales yet.");
            empty.setTextSize(13);
            empty.setTextColor(GRAY_TEXT);
            empty.setPadding(dp(4), dp(16), dp(4), dp(16));
            topProductsContainer.addView(empty);
            return;
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(profitByProduct.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(sorted.size(), 10);

        for (int i = 0; i < limit; i++) {

            String barcode = sorted.get(i).getKey();
            double profit = sorted.get(i).getValue();
            String name = nameByBarcode.get(barcode);
            long units = unitsByBarcode.getOrDefault(barcode, 0L);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.bottomMargin = dp(10);
            topProductsContainer.addView(topProductRow(i + 1, name, units, profit), p);
        }
    }

    private View topProductRow(int rank, String name, long unitsSold, double profit) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        row.setElevation(dp(1));

        FrameLayout rankWrap = new FrameLayout(this);
        GradientDrawable rankBg = new GradientDrawable();
        rankBg.setShape(GradientDrawable.OVAL);
        rankBg.setColor(Color.rgb(224, 246, 233));
        rankWrap.setBackground(rankBg);

        TextView rankText = new TextView(this);
        rankText.setText(String.valueOf(rank));
        rankText.setTextSize(13);
        rankText.setTypeface(null, Typeface.BOLD);
        rankText.setTextColor(GREEN);
        rankWrap.addView(rankText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ));

        row.addView(rankWrap, new LinearLayout.LayoutParams(dp(30), dp(30)));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textColParams.leftMargin = dp(12);

        TextView nameView = new TextView(this);
        nameView.setText(name != null ? name : "Unknown product");
        nameView.setTextSize(13);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        textCol.addView(nameView);

        TextView unitsView = new TextView(this);
        unitsView.setText(unitsSold + " units sold");
        unitsView.setTextSize(11);
        unitsView.setTextColor(GRAY_TEXT);
        textCol.addView(unitsView);

        row.addView(textCol, textColParams);

        TextView profitView = new TextView(this);
        profitView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", profit));
        profitView.setTextSize(14);
        profitView.setTypeface(null, Typeface.BOLD);
        profitView.setTextColor(GREEN);
        row.addView(profitView);

        return row;
    }

    // -------------------------
    // HELPERS
    // -------------------------

    private List<SaleRecord> salesInMonth(int monthOffset) {

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, monthOffset);
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String targetMonth = monthFmt.format(cal.getTime());

        List<SaleRecord> result = new ArrayList<>();
        for (SaleRecord s : allSales) {
            if (s.getDate() != null && s.getDate().startsWith(targetMonth)) {
                result.add(s);
            }
        }
        return result;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }
}
