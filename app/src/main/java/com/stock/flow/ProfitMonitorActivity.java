package com.stock.flow;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Profit Monitor - lists every product with its current profit
 * percentage (markup over cost), sorted lowest-margin first so
 * items that need a price adjustment stand out. Tapping a product
 * opens an editor with a live profit preview as cost/selling
 * price are changed.
 */
public class ProfitMonitorActivity extends AppCompatActivity {

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int ORANGE = Color.rgb(255, 159, 67);
    private final int RED = Color.rgb(230, 70, 70);
    private final int LIGHT_BORDER = Color.rgb(225, 231, 240);

    private float density;

    private List<Product> allProducts = new ArrayList<>();
    private DatabaseReference inventoryRef;
    private ValueEventListener inventoryListener;

    private String searchQuery = "";

    private LinearLayout listContainer;
    private TextView avgMarginValue;

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        density = getResources().getDisplayMetrics().density;

        setContentView(buildContent());

        startListeningToInventory();
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
        column.addView(spacer(16));

        column.addView(buildAverageMarginCard());
        column.addView(spacer(16));

        column.addView(buildSearchRow());
        column.addView(spacer(16));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        column.addView(listContainer);

        scrollView.addView(column);

        refreshList();

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
        title.setText("Profit Monitor");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        row.addView(title);

        return row;
    }

    private View buildAverageMarginCard() {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(222, 233, 250));
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        TextView label = new TextView(this);
        label.setText("Average Profit Margin");
        label.setTextSize(13);
        label.setTextColor(NAVY);
        card.addView(label);

        avgMarginValue = new TextView(this);
        avgMarginValue.setText("0%");
        avgMarginValue.setTextSize(28);
        avgMarginValue.setTypeface(null, Typeface.BOLD);
        avgMarginValue.setTextColor(BLUE);
        card.addView(avgMarginValue);

        TextView hint = new TextView(this);
        hint.setText("Tap a product to review or adjust its pricing");
        hint.setTextSize(11);
        hint.setTextColor(GRAY_TEXT);
        card.addView(hint);

        return card;
    }

    private View buildSearchRow() {

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(dp(14), dp(4), dp(14), dp(4));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.WHITE);
        searchBg.setCornerRadius(dp(16));
        searchBg.setStroke(dp(1), LIGHT_BORDER);
        searchBar.setBackground(searchBg);

        IconViews.IconView searchIcon = new IconViews.IconView(this, IconViews.TYPE_SEARCH, GRAY_TEXT);
        searchBar.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        EditText input = new EditText(this);
        input.setHint("Search products");
        input.setTextSize(13);
        input.setTextColor(NAVY);
        input.setHintTextColor(GRAY_TEXT);
        input.setBackground(null);
        input.setSingleLine(true);
        input.setPadding(dp(10), dp(10), dp(10), dp(10));

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                refreshList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        searchBar.addView(input, inputParams);

        searchBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ));

        return searchBar;
    }

    // -------------------------
    // FIREBASE
    // -------------------------

    private void startListeningToInventory() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        inventoryRef = FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("inventory");

        inventoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Product> products = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product != null) {
                        product.setKey(child.getKey());
                        products.add(product);
                    }
                }

                allProducts = products;
                refreshList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        ProfitMonitorActivity.this,
                        "Unable to load products: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        inventoryRef.addValueEventListener(inventoryListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (inventoryRef != null && inventoryListener != null) {
            inventoryRef.removeEventListener(inventoryListener);
        }
    }

    // -------------------------
    // LIST
    // -------------------------

    private double marginPercent(Product p) {
        if (p.getCostPrice() <= 0) {
            return 0;
        }
        return ((p.getSellingPrice() - p.getCostPrice()) / p.getCostPrice()) * 100;
    }

    private void refreshList() {

        if (listContainer == null) {
            return;
        }

        listContainer.removeAllViews();

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (searchQuery.isEmpty() || (p.getName() != null
                    && p.getName().toLowerCase(Locale.getDefault()).contains(searchQuery))) {
                filtered.add(p);
            }
        }

        double totalMargin = 0;
        int countedForAvg = 0;
        for (Product p : allProducts) {
            if (p.getCostPrice() > 0) {
                totalMargin += marginPercent(p);
                countedForAvg++;
            }
        }
        avgMarginValue.setText(
                countedForAvg > 0
                        ? String.format(Locale.getDefault(), "%.1f%%", totalMargin / countedForAvg)
                        : "N/A"
        );

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(allProducts.isEmpty() ? "No products yet." : "No matching products.");
            empty.setTextSize(13);
            empty.setTextColor(GRAY_TEXT);
            empty.setPadding(dp(4), dp(20), dp(4), dp(20));
            listContainer.addView(empty);
            return;
        }

        // Pinaka-mababang margin muna, para agad na makita kung
        // aling items ang kailangan ng price adjustment.
        List<Product> sorted = new ArrayList<>(filtered);
        Collections.sort(sorted, (a, b) -> Double.compare(marginPercent(a), marginPercent(b)));

        for (Product product : sorted) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.bottomMargin = dp(10);
            listContainer.addView(productRow(product), p);
        }
    }

    private View productRow(Product product) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setClickable(true);
        row.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        row.setElevation(dp(1));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );

        TextView nameView = new TextView(this);
        nameView.setText(product.getName() != null ? product.getName() : "");
        nameView.setTextSize(14);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        textCol.addView(nameView);

        TextView pricesView = new TextView(this);
        pricesView.setText("Cost \u20B1" + String.format(Locale.getDefault(), "%.2f", product.getCostPrice())
                + "  \u2192  Sell \u20B1" + String.format(Locale.getDefault(), "%.2f", product.getSellingPrice()));
        pricesView.setTextSize(11);
        pricesView.setTextColor(GRAY_TEXT);
        textCol.addView(pricesView);

        row.addView(textCol, textColParams);

        double margin = marginPercent(product);
        int marginColor = margin < 10 ? RED : (margin < 25 ? ORANGE : GREEN);

        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12), dp(6), dp(12), dp(6));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(withAlpha(marginColor, 28));
        badgeBg.setCornerRadius(dp(10));
        badge.setBackground(badgeBg);

        TextView marginView = new TextView(this);
        marginView.setText(String.format(Locale.getDefault(), "%.0f%%", margin));
        marginView.setTextSize(14);
        marginView.setTypeface(null, Typeface.BOLD);
        marginView.setTextColor(marginColor);
        badge.addView(marginView);

        TextView profitView = new TextView(this);
        profitView.setText("\u20B1" + String.format(
                Locale.getDefault(), "%.2f", product.getSellingPrice() - product.getCostPrice()
        ));
        profitView.setTextSize(10);
        profitView.setTextColor(marginColor);
        badge.addView(profitView);

        row.addView(badge);

        row.setOnClickListener(v -> showEditPriceDialog(product));

        return row;
    }

    private int withAlpha(int color, int alpha255) {
        return Color.argb(alpha255, Color.red(color), Color.green(color), Color.blue(color));
    }

    // -------------------------
    // EDIT COST / SELLING PRICE (may live profit preview)
    // -------------------------

    private void showEditPriceDialog(Product product) {

        LinearLayout shell = modernDialogShell(product.getName());

        TextInputLayout costField = textField("Cost Price", true);
        setFieldText(costField, String.valueOf(product.getCostPrice()));
        shell.addView(costField);

        TextInputLayout sellingField = textField("Selling Price", true);
        setFieldText(sellingField, String.valueOf(product.getSellingPrice()));
        shell.addView(sellingField);

        shell.addView(spacer(4));

        LinearLayout previewCard = new LinearLayout(this);
        previewCard.setOrientation(LinearLayout.HORIZONTAL);
        previewCard.setGravity(Gravity.CENTER_VERTICAL);
        previewCard.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(Color.rgb(246, 248, 252));
        previewBg.setCornerRadius(dp(12));
        previewCard.setBackground(previewBg);

        TextView previewLabel = new TextView(this);
        previewLabel.setText("New Profit");
        previewLabel.setTextSize(12);
        previewLabel.setTextColor(GRAY_TEXT);
        LinearLayout.LayoutParams previewLabelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        previewCard.addView(previewLabel, previewLabelParams);

        TextView previewValue = new TextView(this);
        previewValue.setTextSize(15);
        previewValue.setTypeface(null, Typeface.BOLD);
        previewCard.addView(previewValue);

        shell.addView(previewCard);
        shell.addView(spacer(14));

        Runnable[] updatePreview = new Runnable[1];
        updatePreview[0] = () -> {

            double cost = parseOrZero(fieldText(costField));
            double selling = parseOrZero(fieldText(sellingField));
            double profit = selling - cost;
            double margin = cost > 0 ? (profit / cost) * 100 : 0;

            int color = margin < 10 ? RED : (margin < 25 ? ORANGE : GREEN);

            previewValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", profit)
                    + "  (" + String.format(Locale.getDefault(), "%.1f", margin) + "%)");
            previewValue.setTextColor(color);
        };

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview[0].run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        if (costField.getEditText() != null) {
            costField.getEditText().addTextChangedListener(watcher);
        }
        if (sellingField.getEditText() != null) {
            sellingField.getEditText().addTextChangedListener(watcher);
        }

        updatePreview[0].run();

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(buttonRow);

        AlertDialog dialog = buildModernDialog(shell);

        LinearLayout cancelBtn = flatDialogButton("Cancel", NAVY, false);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        cancelParams.rightMargin = dp(8);
        buttonRow.addView(cancelBtn, cancelParams);

        LinearLayout saveBtn = flatDialogButton("Save", BLUE, true);
        buttonRow.addView(saveBtn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));

        saveBtn.setOnClickListener(v -> {

            String costStr = fieldText(costField);
            String sellingStr = fieldText(sellingField);

            if (sellingStr.isEmpty()) {
                sellingField.setError("Required");
                return;
            }
            sellingField.setError(null);

            double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
            double selling = Double.parseDouble(sellingStr);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("costPrice", cost);
            updates.put("sellingPrice", selling);
            updates.put("updatedAt", System.currentTimeMillis());

            FirebaseDatabase.getInstance()
                    .getReference("default_inventory")
                    .child(user.getUid())
                    .child("inventory")
                    .child(product.getBarcode())
                    .updateChildren(updates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Prices updated", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "Could not save: " + (task.getException() != null
                                    ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    private double parseOrZero(String s) {
        try {
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setFieldText(TextInputLayout field, String value) {
        if (field.getEditText() != null) {
            field.getEditText().setText(value);
        }
    }

    // -------------------------
    // MODERN DIALOG HELPERS
    // -------------------------

    private LinearLayout modernDialogShell(String title) {

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(22), dp(20), dp(22), dp(18));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(20));
        shell.setBackground(bg);

        if (title != null) {
            TextView titleView = new TextView(this);
            titleView.setText(title);
            titleView.setTextSize(17);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setTextColor(NAVY);
            shell.addView(titleView);
            shell.addView(spacer(14));
        }

        return shell;
    }

    private AlertDialog buildModernDialog(View content) {

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }

    private LinearLayout flatDialogButton(String label, int color, boolean filled) {

        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(13), 0, dp(13));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (filled) {
            bg.setColor(color);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), LIGHT_BORDER);
        }
        button.setBackground(bg);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(13);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(filled ? Color.WHITE : color);
        button.addView(text);

        return button;
    }

    private TextInputLayout textField(String hint, boolean numeric) {

        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(12), dp(12), dp(12), dp(12));
        layout.setBoxStrokeColor(BLUE);
        layout.setHintTextColor(android.content.res.ColorStateList.valueOf(BLUE));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.bottomMargin = dp(12);
        layout.setLayoutParams(layoutParams);

        TextInputEditText input = new TextInputEditText(this);
        input.setTextColor(NAVY);
        if (numeric) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        layout.addView(input);

        return layout;
    }

    private String fieldText(TextInputLayout field) {
        return field.getEditText() != null
                ? field.getEditText().getText().toString().trim() : "";
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }
}
