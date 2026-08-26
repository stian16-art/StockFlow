package com.stock.flow;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Inventory screen - grid ng products (2 columns), may search,
 * category filter chips, at stock status indicator bawat item.
 * Stateful na ito ngayon - tinatawag ng InventoryFragment ang
 * setProducts() tuwing may bagong data mula Firebase, at
 * nire-refresh lang ang mga apektadong bahagi (hindi buong screen).
 */
public class InventoryHomeView {

    private final Context context;
    private final float density;

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int BLUE_DARK = Color.rgb(30, 80, 200);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int ORANGE = Color.rgb(255, 159, 67);
    private final int RED = Color.rgb(230, 70, 70);
    private final int LIGHT_BORDER = Color.rgb(225, 231, 240);

    // Stock status thresholds - i-adjust kung kailangan
    private static final long LOW_STOCK_THRESHOLD = 5;

    private List<Product> allProducts = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";
    private String stockFilter = "All";

    private LinearLayout gridContainer;
    private LinearLayout categoryChipsRow;
    private TextView totalProductsValue;
    private TextView lowStockValue;

    /** Tinatawag pag na-tap ang larawan ng isang product - hudyat na magpalit ng picture. */
    public interface OnProductImageClickListener {
        void onImageClick(Product product);
    }

    private OnProductImageClickListener imageClickListener;

    public void setOnProductImageClickListener(OnProductImageClickListener listener) {
        this.imageClickListener = listener;
    }

    public InventoryHomeView(Context context) {
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

        column.addView(buildSearchRow());
        column.addView(spacer(12));

        categoryChipsRow = new LinearLayout(context);
        categoryChipsRow.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView chipScroll = new HorizontalScrollView(context);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.addView(categoryChipsRow);
        column.addView(chipScroll);
        column.addView(spacer(16));

        gridContainer = new LinearLayout(context);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        column.addView(gridContainer);

        scrollView.addView(column);

        refreshCategoryChips();
        refreshStats();
        refreshGrid();

        return scrollView;
    }

    /** Tinatawag ng Fragment tuwing may bagong data mula Firebase. */
    public void setProducts(List<Product> products) {
        this.allProducts = products != null ? products : new ArrayList<>();
        refreshCategoryChips();
        refreshStats();
        refreshGrid();
    }

    // -------------------------
    // HEADER
    // -------------------------

    private View buildHeader() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Inventory");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(title, titleParams);

        LinearLayout addBtn = new LinearLayout(context);
        addBtn.setOrientation(LinearLayout.HORIZONTAL);
        addBtn.setGravity(Gravity.CENTER_VERTICAL);
        addBtn.setPadding(dp(14), dp(9), dp(16), dp(9));

        GradientDrawable addBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BLUE, BLUE_DARK}
        );
        addBg.setCornerRadius(dp(16));
        addBtn.setBackground(addBg);
        addBtn.setElevation(dp(2));

        IconViews.IconView plusIcon = new IconViews.IconView(
                context, IconViews.TYPE_PLUS, Color.WHITE
        );
        addBtn.addView(plusIcon, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView addText = new TextView(context);
        addText.setText("Add");
        addText.setTextSize(13);
        addText.setTypeface(null, Typeface.BOLD);
        addText.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams addTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addTextParams.leftMargin = dp(6);
        addBtn.addView(addText, addTextParams);

        addBtn.setOnClickListener(v -> showAddProductDialog());

        row.addView(addBtn);

        return row;
    }

    // -------------------------
    // STATS ROW
    // -------------------------

    private View buildStatsRow() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        leftParams.rightMargin = dp(10);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );

        View totalCard = statCard(IconViews.TYPE_CUBE, "Total Products", BLUE);
        totalProductsValue = (TextView) ((LinearLayout) totalCard).findViewWithTag("value");
        row.addView(totalCard, leftParams);

        View lowStockCard = statCard(IconViews.TYPE_WARNING, "Low Stock", ORANGE);
        lowStockValue = (TextView) ((LinearLayout) lowStockCard).findViewWithTag("value");
        row.addView(lowStockCard, rightParams);

        return row;
    }

    private View statCard(int iconType, String label, int accentColor) {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

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
        int iconSize = dp(18);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        int wrapSize = dp(36);
        card.addView(iconWrap, new LinearLayout.LayoutParams(wrapSize, wrapSize));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textColParams.leftMargin = dp(10);

        TextView valueView = new TextView(context);
        valueView.setTag("value");
        valueView.setText("0");
        valueView.setTextSize(18);
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setTextColor(NAVY);
        textCol.addView(valueView);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(11);
        labelView.setTextColor(GRAY_TEXT);
        textCol.addView(labelView);

        card.addView(textCol, textColParams);

        return card;
    }

    private void refreshStats() {
        if (totalProductsValue == null || lowStockValue == null) {
            return;
        }

        int total = allProducts.size();
        int lowStock = 0;
        for (Product p : allProducts) {
            if (p.getStock() <= LOW_STOCK_THRESHOLD) {
                lowStock++;
            }
        }

        totalProductsValue.setText(String.valueOf(total));
        lowStockValue.setText(String.valueOf(lowStock));
    }

    private int withAlpha(int color, int alpha255) {
        return Color.argb(
                alpha255,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    // -------------------------
    // SEARCH
    // -------------------------

    private View buildSearchRow() {

        LinearLayout searchBar = new LinearLayout(context);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(dp(14), dp(4), dp(14), dp(4));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.WHITE);
        searchBg.setCornerRadius(dp(16));
        searchBg.setStroke(dp(1), LIGHT_BORDER);
        searchBar.setBackground(searchBg);

        IconViews.IconView searchIcon = new IconViews.IconView(
                context, IconViews.TYPE_SEARCH, GRAY_TEXT
        );
        searchBar.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        EditText input = new EditText(context);
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
                refreshGrid();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        searchBar.addView(input, inputParams);

        IconViews.IconView filterIcon = new IconViews.IconView(
                context, IconViews.TYPE_FILTER, BLUE
        );
        filterIcon.setOnClickListener(v -> showFilterDialog());
        searchBar.addView(filterIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        searchBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ));

        return searchBar;
    }

    // -------------------------
    // CATEGORY CHIPS (dynamic - base sa totoong categories mula Firebase)
    // -------------------------

    private void refreshCategoryChips() {

        if (categoryChipsRow == null) {
            return;
        }

        categoryChipsRow.removeAllViews();

        Set<String> categories = new LinkedHashSet<>();
        categories.add("All");
        for (Product p : allProducts) {
            if (p.getCategory() != null && !p.getCategory().isEmpty()) {
                categories.add(p.getCategory());
            }
        }

        // Kung tinanggal na ang currently selected category sa data, balik sa "All"
        if (!categories.contains(selectedCategory)) {
            selectedCategory = "All";
        }

        for (String category : categories) {
            boolean isSelected = category.equals(selectedCategory);
            View chip = categoryChip(category, isSelected);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.rightMargin = dp(8);
            categoryChipsRow.addView(chip, p);
        }
    }

    private View categoryChip(String label, boolean selected) {

        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(12.5f);
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(selected ? Color.WHITE : NAVY);
        chip.setPadding(dp(16), dp(9), dp(16), dp(9));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));

        if (selected) {
            bg.setColor(BLUE);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), LIGHT_BORDER);
        }

        chip.setBackground(bg);

        chip.setOnClickListener(v -> {
            selectedCategory = label;
            refreshCategoryChips();
            refreshGrid();
        });

        return chip;
    }

    // -------------------------
    // PRODUCT GRID
    // -------------------------

    private List<Product> filteredProducts() {

        List<Product> filtered = new ArrayList<>();

        for (Product p : allProducts) {

            boolean matchesCategory = selectedCategory.equals("All")
                    || selectedCategory.equals(p.getCategory());

            boolean matchesSearch = searchQuery.isEmpty()
                    || (p.getName() != null
                        && p.getName().toLowerCase(Locale.getDefault()).contains(searchQuery));

            boolean matchesStock;
            switch (stockFilter) {
                case "In Stock":
                    matchesStock = p.getStock() > LOW_STOCK_THRESHOLD;
                    break;
                case "Low Stock":
                    matchesStock = p.getStock() > 0 && p.getStock() <= LOW_STOCK_THRESHOLD;
                    break;
                case "Out of Stock":
                    matchesStock = p.getStock() <= 0;
                    break;
                default:
                    matchesStock = true;
            }

            if (matchesCategory && matchesSearch && matchesStock) {
                filtered.add(p);
            }
        }

        return filtered;
    }

    private void refreshGrid() {

        if (gridContainer == null) {
            return;
        }

        gridContainer.removeAllViews();

        List<Product> products = filteredProducts();

        if (products.isEmpty()) {
            gridContainer.addView(buildEmptyState());
            return;
        }

        for (int i = 0; i < products.size(); i += 2) {

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            leftParams.rightMargin = dp(6);
            leftParams.bottomMargin = dp(12);

            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            rightParams.leftMargin = dp(6);
            rightParams.bottomMargin = dp(12);

            row.addView(buildProductCard(products.get(i)), leftParams);

            if (i + 1 < products.size()) {
                row.addView(buildProductCard(products.get(i + 1)), rightParams);
            } else {
                View filler = new View(context);
                row.addView(filler, rightParams);
            }

            gridContainer.addView(row);
        }
    }

    private View buildEmptyState() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(40), dp(20), dp(40));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        TextView text = new TextView(context);
        text.setText(allProducts.isEmpty() ? "Loading products..." : "No products found");
        text.setTextSize(13);
        text.setTextColor(GRAY_TEXT);
        card.addView(text);

        return card;
    }

    private View buildProductCard(Product product) {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setElevation(dp(1));

        // Product image (Glide) o placeholder box kung walang imagePath
        View.OnClickListener imageTapListener = v -> {
            if (imageClickListener != null) {
                imageClickListener.onImageClick(product);
            }
        };

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {

            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackground(placeholderDrawable());
            imageView.setPadding(dp(6), dp(6), dp(6), dp(6));
            imageView.setOnClickListener(imageTapListener);

            Glide.with(context)
                    .load(product.getImagePath())
                    .placeholder(placeholderDrawable())
                    .error(placeholderDrawable())
                    .into(imageView);

            card.addView(imageView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(90)
            ));
        } else {
            View imageBox = new View(context);
            imageBox.setBackground(placeholderDrawable());
            imageBox.setOnClickListener(imageTapListener);
            card.addView(imageBox, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(90)
            ));
        }

        card.addView(spacer(8));

        TextView nameView = new TextView(context);
        nameView.setText(product.getName() != null ? product.getName() : "");
        nameView.setTextSize(13);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        card.addView(nameView);

        TextView categoryView = new TextView(context);
        categoryView.setText(product.getCategory() != null ? product.getCategory() : "");
        categoryView.setTextSize(11);
        categoryView.setTextColor(GRAY_TEXT);
        categoryView.setMaxLines(1);
        card.addView(categoryView);

        card.addView(spacer(6));

        TextView priceView = new TextView(context);
        priceView.setText(String.format(Locale.getDefault(), "\u20B1%.2f", product.getSellingPrice()));
        priceView.setTextSize(13);
        priceView.setTypeface(null, Typeface.BOLD);
        priceView.setTextColor(BLUE);
        card.addView(priceView);

        card.addView(spacer(6));

        // Stock status chip
        int stockColor;
        String stockLabel;

        if (product.getStock() <= 0) {
            stockColor = RED;
            stockLabel = "Out of Stock";
        } else if (product.getStock() <= LOW_STOCK_THRESHOLD) {
            stockColor = ORANGE;
            stockLabel = "Low Stock (" + product.getStock() + ")";
        } else {
            stockColor = GREEN;
            stockLabel = "In Stock (" + product.getStock() + ")";
        }

        LinearLayout stockChip = new LinearLayout(context);
        stockChip.setOrientation(LinearLayout.HORIZONTAL);
        stockChip.setGravity(Gravity.CENTER_VERTICAL);
        stockChip.setPadding(dp(8), dp(4), dp(8), dp(4));

        GradientDrawable stockBg = new GradientDrawable();
        stockBg.setColor(withAlpha(stockColor, 28));
        stockBg.setCornerRadius(dp(10));
        stockChip.setBackground(stockBg);

        View dot = new View(context);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(stockColor);
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(6), dp(6));
        dotParams.rightMargin = dp(5);
        stockChip.addView(dot, dotParams);

        TextView stockText = new TextView(context);
        stockText.setText(stockLabel);
        stockText.setTextSize(10);
        stockText.setTypeface(null, Typeface.BOLD);
        stockText.setTextColor(stockColor);
        stockChip.addView(stockText);

        card.addView(stockChip, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return card;
    }

    private GradientDrawable placeholderDrawable() {
        GradientDrawable imageBg = new GradientDrawable();
        imageBg.setColor(Color.rgb(230, 234, 240));
        imageBg.setCornerRadius(dp(10));
        return imageBg;
    }

    // -------------------------
    // ADD PRODUCT DIALOG
    // -------------------------

    private void showAddProductDialog() {

        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        form.setPadding(pad, dp(12), pad, 0);

        EditText nameInput = dialogInput("Product Name");
        EditText categoryInput = dialogInput("Category");
        EditText barcodeInput = dialogInput("Barcode");
        barcodeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText costInput = dialogInput("Cost Price");
        costInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText sellingInput = dialogInput("Selling Price");
        sellingInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText stockInput = dialogInput("Stock Quantity");
        stockInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        form.addView(nameInput);
        form.addView(categoryInput);
        form.addView(barcodeInput);
        form.addView(costInput);
        form.addView(sellingInput);
        form.addView(stockInput);

        ScrollView scroll = new ScrollView(context);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Add Product")
                .setView(scroll)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {

            Button addBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            addBtn.setOnClickListener(v -> {

                String name = nameInput.getText().toString().trim();
                String category = categoryInput.getText().toString().trim();
                String barcode = barcodeInput.getText().toString().trim();
                String costStr = costInput.getText().toString().trim();
                String sellingStr = sellingInput.getText().toString().trim();
                String stockStr = stockInput.getText().toString().trim();

                if (name.isEmpty()) {
                    nameInput.setError("Kailangan");
                    return;
                }
                if (barcode.isEmpty()) {
                    barcodeInput.setError("Kailangan");
                    return;
                }
                if (sellingStr.isEmpty()) {
                    sellingInput.setError("Kailangan");
                    return;
                }

                double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
                double selling = Double.parseDouble(sellingStr);
                long stock = stockStr.isEmpty() ? 0 : Long.parseLong(stockStr);

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(context, "Kailangan naka-login", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> productData = new HashMap<>();
                productData.put("name", name);
                productData.put("category", category);
                productData.put("barcode", barcode);
                productData.put("costPrice", cost);
                productData.put("sellingPrice", selling);
                productData.put("stock", stock);
                productData.put("imagePath", "");
                productData.put("id", System.currentTimeMillis());
                productData.put("updatedAt", System.currentTimeMillis());

                FirebaseDatabase.getInstance()
                        .getReference("default_inventory")
                        .child(user.getUid())
                        .child("inventory")
                        .child(barcode)
                        .setValue(productData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Naidagdag ang produkto", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(
                                        context,
                                        "Hindi na-save: " + (task.getException() != null
                                                ? task.getException().getMessage() : ""),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    private EditText dialogInput(String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = dp(8);
        input.setLayoutParams(p);
        return input;
    }

    // -------------------------
    // FILTER DIALOG (stock status)
    // -------------------------

    private void showFilterDialog() {

        String[] options = {"All", "In Stock", "Low Stock", "Out of Stock"};
        int checked = Arrays.asList(options).indexOf(stockFilter);

        new AlertDialog.Builder(context)
                .setTitle("Filter by Stock Status")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    stockFilter = options[which];
                    refreshGrid();
                    dialog.dismiss();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // -------------------------
    // SHARED
    // -------------------------

    private View spacer(int heightDp) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }
}
