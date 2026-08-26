package com.stock.flow;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * POS (Point of Sale) screen - light theme. Stateful na ngayon:
 * may totoong cart, kumukuha ng products mula sa parehong
 * inventory na binabasa ng InventoryFragment, at ang PAY ay
 * bumabawas ng stock at gumagawa ng sales record sa Firebase.
 */
public class POSHomeView {

    private final Context context;
    private final float density;

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int BLUE_DARK = Color.rgb(30, 80, 200);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int RED = Color.rgb(230, 70, 70);
    private final int LIGHT_BORDER = Color.rgb(225, 231, 240);

    private List<Product> allProducts = new ArrayList<>();
    private final List<CartItem> cart = new ArrayList<>();
    private PaymentSettings paymentSettings;

    private EditText searchInput;
    private LinearLayout dropdownContainer;
    private LinearLayout cartSection;
    private TextView totalItemsValue;
    private TextView totalAmountValue;
    private LinearLayout payButton;

    /** Tinatawag pag na-tap ang scanner icon sa search bar. */
    public interface OnScannerClickListener {
        void onScannerClick();
    }

    private OnScannerClickListener scannerClickListener;

    public void setOnScannerClickListener(OnScannerClickListener listener) {
        this.scannerClickListener = listener;
    }

    public POSHomeView(Context context) {
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
        column.addView(spacer(18));

        column.addView(buildSearchRow());
        column.addView(spacer(6));

        dropdownContainer = new LinearLayout(context);
        dropdownContainer.setOrientation(LinearLayout.VERTICAL);
        dropdownContainer.setVisibility(View.GONE);
        column.addView(dropdownContainer);
        column.addView(spacer(10));

        column.addView(buildColumnHeaders());
        column.addView(spacer(10));

        cartSection = new LinearLayout(context);
        cartSection.setOrientation(LinearLayout.VERTICAL);
        column.addView(cartSection);
        column.addView(spacer(16));

        column.addView(buildSummaryCard());
        column.addView(spacer(16));

        payButton = buildPayButton();
        column.addView(payButton);

        scrollView.addView(column);

        refreshCart();

        return scrollView;
    }

    /** Tinatawag ng Fragment tuwing may bagong data mula Firebase. */
    public void setProducts(List<Product> products) {
        this.allProducts = products != null ? products : new ArrayList<>();
    }

    /** Tinatawag ng Fragment tuwing may bagong payment settings mula sa Profile. */
    public void setPaymentSettings(PaymentSettings settings) {
        this.paymentSettings = settings;
    }

    // -------------------------
    // HEADER
    // -------------------------

    private View buildHeader() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("POS");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(title, titleParams);

        LinearLayout dateChip = new LinearLayout(context);
        dateChip.setOrientation(LinearLayout.HORIZONTAL);
        dateChip.setGravity(Gravity.CENTER_VERTICAL);
        dateChip.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(Color.WHITE);
        chipBg.setCornerRadius(dp(14));
        dateChip.setBackground(chipBg);
        dateChip.setElevation(dp(2));

        IconViews.IconView calendarIcon = new IconViews.IconView(
                context, IconViews.TYPE_CALENDAR, BLUE
        );
        dateChip.addView(calendarIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView dateText = new TextView(context);
        dateText.setText(currentDateLabel());
        dateText.setTextSize(13);
        dateText.setTypeface(null, Typeface.BOLD);
        dateText.setTextColor(NAVY);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.leftMargin = dp(6);
        dateChip.addView(dateText, dateParams);

        row.addView(dateChip);

        return row;
    }

    private String currentDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        return sdf.format(new Date());
    }

    // -------------------------
    // SEARCH + ADD
    // -------------------------

    private View buildSearchRow() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

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

        searchInput = new EditText(context);
        searchInput.setHint("Search item or barcode");
        searchInput.setTextSize(13);
        searchInput.setTextColor(NAVY);
        searchInput.setHintTextColor(GRAY_TEXT);
        searchInput.setBackground(null);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(10), dp(10), dp(10), dp(10));
        searchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            handleAddToCart();
            return true;
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshDropdown(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        searchBar.addView(searchInput, inputParams);

        IconViews.IconView barcodeIcon = new IconViews.IconView(
                context, IconViews.TYPE_BARCODE, BLUE
        );
        barcodeIcon.setOnClickListener(v -> {
            if (scannerClickListener != null) {
                scannerClickListener.onScannerClick();
            }
        });
        searchBar.addView(barcodeIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                0, dp(52), 1f
        );
        row.addView(searchBar, searchParams);

        TextView addBtn = new TextView(context);
        addBtn.setText("ADD");
        addBtn.setTextSize(13);
        addBtn.setTypeface(null, Typeface.BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setGravity(Gravity.CENTER);

        GradientDrawable addBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BLUE, BLUE_DARK}
        );
        addBg.setCornerRadius(dp(16));
        addBtn.setBackground(addBg);

        addBtn.setOnClickListener(v -> handleAddToCart());

        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                dp(76), dp(52)
        );
        addParams.leftMargin = dp(10);
        row.addView(addBtn, addParams);

        return row;
    }

    private void handleAddToCart() {

        String query = searchInput.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(context, "Maglagay ng pangalan o barcode", Toast.LENGTH_SHORT).show();
            return;
        }

        // Exact barcode match muna
        for (Product p : allProducts) {
            if (p.getBarcode() != null && p.getBarcode().equals(query)) {
                addToCart(p);
                searchInput.setText("");
                return;
            }
        }

        // Name-contains match
        List<Product> matches = new ArrayList<>();
        String lower = query.toLowerCase(Locale.getDefault());
        for (Product p : allProducts) {
            if (p.getName() != null && p.getName().toLowerCase(Locale.getDefault()).contains(lower)) {
                matches.add(p);
            }
        }

        if (matches.isEmpty()) {
            Toast.makeText(context, "Walang nahanap na produkto", Toast.LENGTH_SHORT).show();
        } else if (matches.size() == 1) {
            addToCart(matches.get(0));
            searchInput.setText("");
        } else {
            String[] names = new String[matches.size()];
            for (int i = 0; i < matches.size(); i++) {
                names[i] = matches.get(i).getName() + " - \u20B1"
                        + String.format(Locale.getDefault(), "%.2f", matches.get(i).getSellingPrice());
            }
            new AlertDialog.Builder(context)
                    .setTitle("Piliin ang produkto")
                    .setItems(names, (dialog, which) -> {
                        addToCart(matches.get(which));
                        searchInput.setText("");
                    })
                    .show();
        }
    }

    private void refreshDropdown(String query) {

        dropdownContainer.removeAllViews();

        if (query.isEmpty()) {
            dropdownContainer.setVisibility(View.GONE);
            return;
        }

        String lower = query.toLowerCase(Locale.getDefault());
        List<Product> matches = new ArrayList<>();

        for (Product p : allProducts) {
            boolean barcodeMatch = p.getBarcode() != null && p.getBarcode().contains(query);
            boolean nameMatch = p.getName() != null
                    && p.getName().toLowerCase(Locale.getDefault()).contains(lower);
            if (barcodeMatch || nameMatch) {
                matches.add(p);
            }
            if (matches.size() >= 6) {
                break;
            }
        }

        if (matches.isEmpty()) {
            dropdownContainer.setVisibility(View.GONE);
            return;
        }

        GradientDrawable dropdownBg = new GradientDrawable();
        dropdownBg.setColor(Color.WHITE);
        dropdownBg.setCornerRadius(dp(14));
        dropdownBg.setStroke(dp(1), LIGHT_BORDER);
        dropdownContainer.setBackground(dropdownBg);
        dropdownContainer.setPadding(dp(4), dp(4), dp(4), dp(4));
        dropdownContainer.setElevation(dp(2));

        for (Product p : matches) {
            dropdownContainer.addView(dropdownRow(p));
        }

        dropdownContainer.setVisibility(View.VISIBLE);
    }

    private View dropdownRow(Product product) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );

        TextView nameView = new TextView(context);
        nameView.setText(product.getName());
        nameView.setTextSize(13);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        textCol.addView(nameView);

        TextView metaView = new TextView(context);
        metaView.setText(product.getBarcode() + " \u00B7 Stock: " + product.getStock());
        metaView.setTextSize(10.5f);
        metaView.setTextColor(GRAY_TEXT);
        textCol.addView(metaView);

        row.addView(textCol, textColParams);

        TextView priceView = new TextView(context);
        priceView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", product.getSellingPrice()));
        priceView.setTextSize(13);
        priceView.setTypeface(null, Typeface.BOLD);
        priceView.setTextColor(BLUE);
        row.addView(priceView);

        row.setOnClickListener(v -> {
            addToCart(product);
            searchInput.setText("");
            dropdownContainer.setVisibility(View.GONE);
        });

        return row;
    }

    /** Tinatawag ng Fragment pagbalik mula sa ScannerActivity na may resulta. */
    public void addScannedItems(List<String> barcodes, List<Integer> quantities) {

        if (barcodes == null || quantities == null) {
            return;
        }

        int notFound = 0;

        for (int i = 0; i < barcodes.size(); i++) {
            String barcode = barcodes.get(i);
            int qty = i < quantities.size() ? quantities.get(i) : 1;

            Product match = null;
            for (Product p : allProducts) {
                if (p.getBarcode() != null && p.getBarcode().equals(barcode)) {
                    match = p;
                    break;
                }
            }

            if (match != null) {
                addToCart(match, qty);
            } else {
                notFound++;
            }
        }

        if (notFound > 0) {
            Toast.makeText(
                    context,
                    notFound + " na barcode ang walang tumugmang produkto sa inventory",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void addToCart(Product product) {
        addToCart(product, 1);
    }

    private void addToCart(Product product, int qtyToAdd) {

        if (product.getStock() <= 0) {
            Toast.makeText(context, "Out of stock: " + product.getName(), Toast.LENGTH_SHORT).show();
            return;
        }

        for (CartItem item : cart) {
            if (item.product.getBarcode() != null
                    && item.product.getBarcode().equals(product.getBarcode())) {

                int newQty = item.quantity + qtyToAdd;
                if (newQty > product.getStock()) {
                    newQty = (int) product.getStock();
                    Toast.makeText(context, "Sapat lang sa stock (" + product.getStock() + ")",
                            Toast.LENGTH_SHORT).show();
                }
                item.quantity = newQty;
                refreshCart();
                return;
            }
        }

        int initialQty = Math.min(qtyToAdd, (int) product.getStock());
        cart.add(new CartItem(product, initialQty));
        refreshCart();
    }

    // -------------------------
    // COLUMN HEADERS
    // -------------------------

    private View buildColumnHeaders() {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        row.setElevation(dp(1));

        row.addView(columnHeaderItem(IconViews.TYPE_CUBE, "Item"), columnParams(1.5f, true));
        row.addView(columnHeaderItem(IconViews.TYPE_HASH, "Qty"), columnParams(0.9f, true));
        row.addView(columnHeaderItem(IconViews.TYPE_TAG, "Price"), columnParams(1f, true));
        row.addView(columnHeaderItem(IconViews.TYPE_COINS, "Total"), columnParams(1f, false));

        return row;
    }

    private LinearLayout.LayoutParams columnParams(float weight, boolean withMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight
        );
        if (withMargin) {
            params.rightMargin = dp(10);
        }
        return params;
    }

    private View columnHeaderItem(int iconType, String label) {

        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);

        IconViews.IconView icon = new IconViews.IconView(context, iconType, BLUE);
        item.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextSize(12);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(NAVY);
        text.setMaxLines(1);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.leftMargin = dp(8);
        item.addView(text, textParams);

        return item;
    }

    // -------------------------
    // CART (empty state o mga item rows)
    // -------------------------

    private void refreshCart() {

        cartSection.removeAllViews();

        if (cart.isEmpty()) {
            cartSection.addView(buildEmptyState());
        } else {
            for (int i = 0; i < cart.size(); i++) {
                CartItem item = cart.get(i);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                p.bottomMargin = dp(8);
                cartSection.addView(cartItemRow(item), p);
            }
        }

        refreshTotals();
    }

    private View buildEmptyState() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(48), dp(20), dp(48));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        card.setElevation(dp(1));

        IconViews.IconView cartIcon = new IconViews.IconView(
                context, IconViews.TYPE_CART, Color.rgb(190, 205, 230)
        );
        card.addView(cartIcon, new LinearLayout.LayoutParams(dp(56), dp(40)));
        card.addView(spacer(16));

        TextView title = new TextView(context);
        title.setText("No items added yet");
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("Search or add items to get started");
        subtitle.setTextSize(12);
        subtitle.setTextColor(GRAY_TEXT);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dp(4);
        card.addView(subtitle, subParams);

        return card;
    }

    private View cartItemRow(CartItem item) {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);
        card.setElevation(dp(1));

        // Row A: name + remove
        LinearLayout rowA = new LinearLayout(context);
        rowA.setOrientation(LinearLayout.HORIZONTAL);
        rowA.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(context);
        nameView.setText(item.product.getName());
        nameView.setTextSize(14);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        rowA.addView(nameView, nameParams);

        TextView removeBtn = new TextView(context);
        removeBtn.setText("\u2715");
        removeBtn.setTextSize(14);
        removeBtn.setTypeface(null, Typeface.BOLD);
        removeBtn.setTextColor(RED);
        removeBtn.setPadding(dp(8), dp(4), dp(4), dp(4));
        removeBtn.setOnClickListener(v -> {
            cart.remove(item);
            refreshCart();
        });
        rowA.addView(removeBtn);

        card.addView(rowA);

        TextView priceView = new TextView(context);
        priceView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", item.product.getSellingPrice())
                + " each");
        priceView.setTextSize(11);
        priceView.setTextColor(GRAY_TEXT);
        card.addView(priceView);

        card.addView(spacer(8));

        // Row C: qty stepper + subtotal
        LinearLayout rowC = new LinearLayout(context);
        rowC.setOrientation(LinearLayout.HORIZONTAL);
        rowC.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout stepper = new LinearLayout(context);
        stepper.setOrientation(LinearLayout.HORIZONTAL);
        stepper.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable stepperBg = new GradientDrawable();
        stepperBg.setColor(Color.rgb(240, 244, 250));
        stepperBg.setCornerRadius(dp(10));
        stepper.setBackground(stepperBg);
        stepper.setPadding(dp(4), dp(4), dp(4), dp(4));

        stepper.addView(stepperButton("\u2212", () -> {
            if (item.quantity <= 1) {
                cart.remove(item);
            } else {
                item.quantity = item.quantity - 1;
            }
            refreshCart();
        }));

        TextView qtyText = new TextView(context);
        qtyText.setText(String.valueOf(item.quantity));
        qtyText.setTextSize(14);
        qtyText.setTypeface(null, Typeface.BOLD);
        qtyText.setTextColor(NAVY);
        qtyText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(dp(32), dp(28));
        stepper.addView(qtyText, qtyParams);

        stepper.addView(stepperButton("+", () -> {
            if (item.quantity + 1 > item.product.getStock()) {
                Toast.makeText(context, "Sapat lang sa stock (" + item.product.getStock() + ")",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            item.quantity = item.quantity + 1;
            refreshCart();
        }));

        rowC.addView(stepper);

        TextView subtotalView = new TextView(context);
        subtotalView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", item.getSubtotal()));
        subtotalView.setTextSize(15);
        subtotalView.setTypeface(null, Typeface.BOLD);
        subtotalView.setTextColor(BLUE);
        subtotalView.setGravity(Gravity.END);
        LinearLayout.LayoutParams subtotalParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        rowC.addView(subtotalView, subtotalParams);

        card.addView(rowC);

        return card;
    }

    private View stepperButton(String label, Runnable onClick) {

        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextSize(16);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(BLUE);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.WHITE);
        btn.setBackground(bg);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(28), dp(28));
        btn.setLayoutParams(p);
        btn.setOnClickListener(v -> onClick.run());

        return btn;
    }

    // -------------------------
    // SUMMARY CARD
    // -------------------------

    private View buildSummaryCard() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        card.setElevation(dp(2));

        View itemsRow = summaryRow(IconViews.TYPE_BAG, "Total Items", NAVY);
        totalItemsValue = (TextView) ((LinearLayout) itemsRow).findViewWithTag("value");
        card.addView(itemsRow);

        card.addView(divider());

        View amountRow = summaryRow(IconViews.TYPE_WALLET, "Total Amount", BLUE);
        totalAmountValue = (TextView) ((LinearLayout) amountRow).findViewWithTag("value");
        card.addView(amountRow);

        return card;
    }

    private View summaryRow(int iconType, String label, int valueColor) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(Color.rgb(232, 239, 250));
        iconWrap.setBackground(circleBg);

        IconViews.IconView icon = new IconViews.IconView(context, iconType, BLUE);
        int iconSize = dp(20);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        int wrapSize = dp(38);
        row.addView(iconWrap, new LinearLayout.LayoutParams(wrapSize, wrapSize));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setTextColor(NAVY);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        labelParams.leftMargin = dp(12);
        row.addView(labelView, labelParams);

        TextView valueView = new TextView(context);
        valueView.setTag("value");
        valueView.setText(label.contains("Amount") ? "\u20B10.00" : "0");
        valueView.setTextSize(18);
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setTextColor(valueColor);
        row.addView(valueView);

        return row;
    }

    private void refreshTotals() {

        int totalItems = 0;
        double totalAmount = 0;

        for (CartItem item : cart) {
            totalItems += item.quantity;
            totalAmount += item.getSubtotal();
        }

        if (totalItemsValue != null) {
            totalItemsValue.setText(String.valueOf(totalItems));
        }
        if (totalAmountValue != null) {
            totalAmountValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", totalAmount));
        }
    }

    private View divider() {
        View line = new View(context);
        line.setBackgroundColor(LIGHT_BORDER);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ));
        return line;
    }

    // -------------------------
    // PAY BUTTON / CHECKOUT
    // -------------------------

    private LinearLayout buildPayButton() {

        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(20), dp(16), dp(20), dp(16));

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BLUE, BLUE_DARK}
        );
        bg.setCornerRadius(dp(18));
        button.setBackground(bg);
        button.setElevation(dp(3));

        IconViews.IconView lockIcon = new IconViews.IconView(
                context, IconViews.TYPE_LOCK, Color.WHITE
        );
        button.addView(lockIcon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView payText = new TextView(context);
        payText.setText("PAY");
        payText.setTextSize(16);
        payText.setTypeface(null, Typeface.BOLD);
        payText.setTextColor(Color.WHITE);
        payText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams payParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        payParams.leftMargin = dp(10);
        button.addView(payText, payParams);

        IconViews.IconView chevron = new IconViews.IconView(
                context, IconViews.TYPE_CHEVRON, Color.WHITE
        );
        button.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(18)));

        button.setOnClickListener(v -> handleCheckout());

        return button;
    }

    private void handleCheckout() {

        if (cart.isEmpty()) {
            Toast.makeText(context, "Wala pang laman ang cart", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Kailangan naka-login", Toast.LENGTH_SHORT).show();
            return;
        }

        showPaymentMethodDialog();
    }

    private void showPaymentMethodDialog() {

        String[] methods = {"Cash", "GCash", "Maya", "Maribank", "Card"};
        int[] icons = {
                IconViews.TYPE_WALLET,
                IconViews.TYPE_BARCODE,
                IconViews.TYPE_BARCODE,
                IconViews.TYPE_BARCODE,
                IconViews.TYPE_TAG
        };

        LinearLayout dialogList = new LinearLayout(context);
        dialogList.setOrientation(LinearLayout.VERTICAL);
        dialogList.setPadding(dp(8), dp(8), dp(8), dp(8));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Paraan ng Bayad")
                .setView(dialogList)
                .setNegativeButton("Cancel", null)
                .create();

        for (int i = 0; i < methods.length; i++) {

            String methodLabel = methods[i];

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setClickable(true);
            row.setFocusable(true);

            android.util.TypedValue outValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true
            );
            row.setBackgroundResource(outValue.resourceId != 0 ? outValue.resourceId : 0);

            IconViews.IconView icon = new IconViews.IconView(context, icons[i], BLUE);
            row.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

            TextView label = new TextView(context);
            label.setText(methodLabel);
            label.setTextSize(14);
            label.setTextColor(NAVY);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            labelParams.leftMargin = dp(14);
            row.addView(label, labelParams);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                routePaymentMethod(methodLabel);
            });

            dialogList.addView(row);
        }

        dialog.show();
    }

    private void routePaymentMethod(String methodLabel) {

        switch (methodLabel) {
            case "Cash":
                showCashDialog();
                break;
            case "GCash":
                showQrDialog(
                        "GCash",
                        paymentSettings != null ? paymentSettings.getGcashNumber() : null,
                        paymentSettings != null ? paymentSettings.getGcashName() : null
                );
                break;
            case "Maya":
                showQrDialog(
                        "Maya",
                        paymentSettings != null ? paymentSettings.getMayaNumber() : null,
                        null
                );
                break;
            case "Maribank":
                showQrDialog(
                        "Maribank",
                        paymentSettings != null ? paymentSettings.getMaribankNumber() : null,
                        null
                );
                break;
            case "Card":
                showCardDialog();
                break;
        }
    }

    // -------------------------
    // CASH DIALOG (tender + sukli)
    // -------------------------

    private void showCashDialog() {

        double total = cartTotal();

        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView totalLabel = new TextView(context);
        totalLabel.setText("Kabuuang Halaga");
        totalLabel.setTextSize(12);
        totalLabel.setTextColor(GRAY_TEXT);
        view.addView(totalLabel);

        TextView totalValue = new TextView(context);
        totalValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", total));
        totalValue.setTextSize(24);
        totalValue.setTypeface(null, Typeface.BOLD);
        totalValue.setTextColor(NAVY);
        view.addView(totalValue);
        view.addView(spacer(16));

        TextView cashLabel = new TextView(context);
        cashLabel.setText("Cash na Ibinigay ng Customer");
        cashLabel.setTextSize(12);
        cashLabel.setTextColor(GRAY_TEXT);
        view.addView(cashLabel);

        EditText cashInput = new EditText(context);
        cashInput.setHint("0.00");
        cashInput.setTextSize(20);
        cashInput.setTypeface(null, Typeface.BOLD);
        cashInput.setTextColor(NAVY);
        cashInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        cashInput.setPadding(dp(4), dp(8), dp(4), dp(8));
        view.addView(cashInput);
        view.addView(spacer(12));

        TextView changeLabel = new TextView(context);
        changeLabel.setText("Sukli");
        changeLabel.setTextSize(12);
        changeLabel.setTextColor(GRAY_TEXT);
        view.addView(changeLabel);

        TextView changeValue = new TextView(context);
        changeValue.setText("\u20B10.00");
        changeValue.setTextSize(22);
        changeValue.setTypeface(null, Typeface.BOLD);
        changeValue.setTextColor(GREEN_COLOR());
        view.addView(changeValue);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Cash Payment")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Done", null)
                .create();

        cashInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                double received = parseDoubleSafe(s.toString());
                double change = received - total;
                changeValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", Math.max(change, 0)));
                changeValue.setTextColor(change >= 0 ? GREEN_COLOR() : RED);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.setOnShowListener(d -> {
            android.widget.Button doneBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            doneBtn.setOnClickListener(v -> {

                double received = parseDoubleSafe(cashInput.getText().toString());

                if (received < total) {
                    Toast.makeText(context, "Hindi sapat ang cash na inilagay", Toast.LENGTH_SHORT).show();
                    return;
                }

                double change = received - total;
                dialog.dismiss();
                performCheckout("Cash", received, change);
            });
        });

        dialog.show();
    }

    private double parseDoubleSafe(String s) {
        try {
            return s == null || s.trim().isEmpty() ? 0 : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int GREEN_COLOR() {
        return Color.rgb(34, 197, 94);
    }

    // -------------------------
    // GCASH / MAYA / MARIBANK QR DIALOG
    // -------------------------

    private void showQrDialog(String methodName, String number, String accountName) {

        if (number == null || number.trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "Wala pang " + methodName + " number na naka-set. Pumunta sa Profile para i-set up.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        double total = cartTotal();

        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        view.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView totalValue = new TextView(context);
        totalValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", total));
        totalValue.setTextSize(22);
        totalValue.setTypeface(null, Typeface.BOLD);
        totalValue.setTextColor(NAVY);
        view.addView(totalValue);
        view.addView(spacer(14));

        String qrContent = methodName + " Payment\n"
                + "Number: " + number + "\n"
                + (accountName != null && !accountName.isEmpty() ? "Name: " + accountName + "\n" : "")
                + "Amount: \u20B1" + String.format(Locale.getDefault(), "%.2f", total);

        Bitmap qrBitmap = generateQrBitmap(qrContent, dp(200));

        if (qrBitmap != null) {
            ImageView qrImage = new ImageView(context);
            qrImage.setImageBitmap(qrBitmap);
            LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(dp(200), dp(200));
            view.addView(qrImage, qrParams);
        }

        view.addView(spacer(12));

        TextView numberView = new TextView(context);
        numberView.setText(methodName + ": " + number);
        numberView.setTextSize(14);
        numberView.setTypeface(null, Typeface.BOLD);
        numberView.setTextColor(NAVY);
        numberView.setGravity(Gravity.CENTER);
        view.addView(numberView);

        TextView hint = new TextView(context);
        hint.setText("Ipakita ito sa customer para i-scan gamit ang " + methodName + " app.");
        hint.setTextSize(11);
        hint.setTextColor(GRAY_TEXT);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.topMargin = dp(4);
        view.addView(hint, hintParams);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(methodName + " Payment")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Nabayaran na - Done", (d, w) -> performCheckout(methodName))
                .create();

        dialog.show();
    }

    private Bitmap generateQrBitmap(String content, int sizePx) {

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);

            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;

        } catch (WriterException e) {
            return null;
        }
    }

    // -------------------------
    // CARD DIALOG
    // -------------------------

    private void showCardDialog() {

        String details = paymentSettings != null ? paymentSettings.getCardDetails() : null;

        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(20), dp(12), dp(20), dp(4));

        double total = cartTotal();

        TextView totalValue = new TextView(context);
        totalValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", total));
        totalValue.setTextSize(22);
        totalValue.setTypeface(null, Typeface.BOLD);
        totalValue.setTextColor(NAVY);
        view.addView(totalValue);
        view.addView(spacer(12));

        TextView detailsView = new TextView(context);
        detailsView.setText(
                details != null && !details.isEmpty()
                        ? details
                        : "Walang naka-set na card details/notes. Pumunta sa Profile para mag-set up."
        );
        detailsView.setTextSize(13);
        detailsView.setTextColor(NAVY);
        view.addView(detailsView);

        new AlertDialog.Builder(context)
                .setTitle("Card Payment")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Nabayaran na - Done", (d, w) -> performCheckout("Card"))
                .show();
    }

    private double cartTotal() {
        double total = 0;
        for (CartItem item : cart) {
            total += item.getSubtotal();
        }
        return total;
    }

    private void performCheckout(String paymentMethod) {
        performCheckout(paymentMethod, null, null);
    }

    private void performCheckout(String paymentMethod, Double cashReceived, Double change) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Kailangan naka-login", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        double totalAmount = 0;
        double totalProfit = 0;
        List<Map<String, Object>> saleItems = new ArrayList<>();

        for (CartItem item : cart) {
            totalAmount += item.getSubtotal();
            totalProfit += item.getProfit();

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("barcode", item.product.getBarcode());
            itemMap.put("name", item.product.getName());
            itemMap.put("price", item.product.getSellingPrice());
            itemMap.put("qty", item.quantity);
            itemMap.put("subtotal", item.getSubtotal());
            itemMap.put("profit", item.getProfit());
            saleItems.add(itemMap);
        }

        long now = System.currentTimeMillis();
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat idFmt = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        Date nowDate = new Date(now);

        String saleId = "SALE_" + idFmt.format(nowDate);

        Map<String, Object> saleMap = new HashMap<>();
        saleMap.put("saleId", saleId);
        saleMap.put("date", dateFmt.format(nowDate));
        saleMap.put("time", timeFmt.format(nowDate));
        saleMap.put("paymentMethod", paymentMethod);
        saleMap.put("totalAmount", totalAmount);
        saleMap.put("totalProfit", totalProfit);
        saleMap.put("createdAt", now);
        saleMap.put("items", saleItems);

        if (cashReceived != null) {
            saleMap.put("cashReceived", cashReceived);
        }
        if (change != null) {
            saleMap.put("change", change);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(uid + "/sales/" + saleId, saleMap);

        for (CartItem item : cart) {
            long newStock = item.product.getStock() - item.quantity;
            if (newStock < 0) {
                newStock = 0;
            }
            updates.put(uid + "/inventory/" + item.product.getBarcode() + "/stock", newStock);
        }

        payButton.setEnabled(false);

        double finalTotalAmount = totalAmount;

        FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .updateChildren(updates)
                .addOnCompleteListener(task -> {

                    payButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                context,
                                "Successful sale (" + paymentMethod + ")! Total: \u20B1"
                                        + String.format(Locale.getDefault(), "%.2f", finalTotalAmount),
                                Toast.LENGTH_LONG
                        ).show();
                        cart.clear();
                        refreshCart();
                    } else {
                        Toast.makeText(
                                context,
                                "Nabigo ang checkout: " + (task.getException() != null
                                        ? task.getException().getMessage() : "unknown error"),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
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
