package com.stock.flow;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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

import java.io.FileOutputStream;
import java.io.IOException;
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
    private List<Customer> allCustomers = new ArrayList<>();
    private final List<CartItem> cart = new ArrayList<>();
    private PaymentSettings paymentSettings;

    private EditText searchInput;
    private LinearLayout dropdownContainer;
    private LinearLayout cartSection;
    private TextView totalItemsValue;
    private TextView totalAmountValue;
    private LinearLayout payButton;
    private LinearLayout cancelButton;

    /** Tinatawag pag na-tap ang scanner icon sa search bar. */
    public interface OnScannerClickListener {
        void onScannerClick();
    }

    private OnScannerClickListener scannerClickListener;

    public void setOnScannerClickListener(OnScannerClickListener listener) {
        this.scannerClickListener = listener;
    }

    /** Tinatawag tuwing may pagbabago sa cart - meron/walang laman. */
    public interface OnCartStateChangeListener {
        void onCartStateChanged(boolean hasItems);
    }

    private OnCartStateChangeListener cartStateChangeListener;

    public void setOnCartStateChangeListener(OnCartStateChangeListener listener) {
        this.cartStateChangeListener = listener;
    }

    public POSHomeView(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    public View build() {

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // TOP - fixed (hindi gumagalaw): header, search, column headers
        LinearLayout topSection = new LinearLayout(context);
        topSection.setOrientation(LinearLayout.VERTICAL);
        topSection.setPadding(dp(20), dp(24), dp(20), 0);

        topSection.addView(buildHeader());
        topSection.addView(spacer(18));

        topSection.addView(buildSearchRow());
        topSection.addView(spacer(6));

        dropdownContainer = new LinearLayout(context);
        dropdownContainer.setOrientation(LinearLayout.VERTICAL);
        dropdownContainer.setVisibility(View.GONE);
        topSection.addView(dropdownContainer);
        topSection.addView(spacer(10));

        topSection.addView(buildColumnHeaders());

        root.addView(topSection);

        // GITNA - ito lang ang scrollable: yung cart items
        ScrollView cartScroll = new ScrollView(context);
        cartScroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        cartScroll.setPadding(dp(20), dp(10), dp(20), dp(10));
        cartScroll.setClipToPadding(false);

        cartSection = new LinearLayout(context);
        cartSection.setOrientation(LinearLayout.VERTICAL);
        cartScroll.addView(cartSection);

        root.addView(cartScroll);

        // IBABA - fixed (hindi gumagalaw): summary + PAY button
        LinearLayout bottomSection = new LinearLayout(context);
        bottomSection.setOrientation(LinearLayout.VERTICAL);
        bottomSection.setPadding(dp(20), 0, dp(20), dp(140));

        bottomSection.addView(buildSummaryCard());
        bottomSection.addView(spacer(10));

        cancelButton = flatDialogButton("Cancel Order", RED, false);
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v -> showCancelOrderConfirm());
        bottomSection.addView(cancelButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomSection.addView(spacer(10));

        payButton = buildPayButton();
        bottomSection.addView(payButton);

        root.addView(bottomSection);

        refreshCart();

        return root;
    }

    /** Tinatawag ng Fragment tuwing may bagong data mula Firebase. */
    public void setProducts(List<Product> products) {
        this.allProducts = products != null ? products : new ArrayList<>();
    }

    /** Tinatawag ng Fragment tuwing may bagong payment settings mula sa Profile. */
    public void setPaymentSettings(PaymentSettings settings) {
        this.paymentSettings = settings;
    }

    /** Tinatawag ng Fragment tuwing may bagong utang customers mula sa Firebase. */
    public void setCustomers(List<Customer> customers) {
        this.allCustomers = customers != null ? customers : new ArrayList<>();
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

        if (cancelButton != null) {
            cancelButton.setVisibility(cart.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (cartStateChangeListener != null) {
            cartStateChangeListener.onCartStateChanged(!cart.isEmpty());
        }
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

    private void showCancelOrderConfirm() {

        new AlertDialog.Builder(context)
                .setTitle("Cancel this order?")
                .setMessage("This will clear all items in the cart.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    cart.clear();
                    refreshCart();
                })
                .show();
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

        String[] methods = {"Cash", "GCash", "Maya", "Maribank", "Card", "Utang"};
        int[] icons = {
                IconViews.TYPE_WALLET,
                IconViews.TYPE_BARCODE,
                IconViews.TYPE_BARCODE,
                IconViews.TYPE_BARCODE,
                IconViews.TYPE_TAG,
                IconViews.TYPE_WARNING
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
            int iconColor = methodLabel.equals("Utang") ? RED : BLUE;

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

            IconViews.IconView icon = new IconViews.IconView(context, icons[i], iconColor);
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
                showEwalletDialog(
                        "GCash",
                        paymentSettings != null ? paymentSettings.getGcashNumber() : null,
                        paymentSettings != null ? paymentSettings.getGcashName() : null
                );
                break;
            case "Maya":
                showEwalletDialog(
                        "Maya",
                        paymentSettings != null ? paymentSettings.getMayaNumber() : null,
                        null
                );
                break;
            case "Maribank":
                showEwalletDialog(
                        "Maribank",
                        paymentSettings != null ? paymentSettings.getMaribankNumber() : null,
                        null
                );
                break;
            case "Card":
                showCardDialog();
                break;
            case "Utang":
                showUtangCustomerDialog();
                break;
        }
    }

    // -------------------------
    // UTANG (product credit sa customer)
    // -------------------------

    private void showUtangCustomerDialog() {

        LinearLayout dialogList = new LinearLayout(context);
        dialogList.setOrientation(LinearLayout.VERTICAL);
        dialogList.setPadding(dp(8), dp(8), dp(8), dp(8));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Utang - Sino ang bibili?")
                .setView(dialogList)
                .setNegativeButton("Cancel", null)
                .create();

        LinearLayout newRow = utangChoiceRow(IconViews.TYPE_PLUS, "Bagong Customer");
        newRow.setOnClickListener(v -> {
            dialog.dismiss();
            showNewUtangCustomerForm();
        });
        dialogList.addView(newRow);

        LinearLayout existingRow = utangChoiceRow(IconViews.TYPE_SEARCH, "Existing Customer");
        existingRow.setOnClickListener(v -> {
            dialog.dismiss();
            showUtangCustomerSearchDialog();
        });
        dialogList.addView(existingRow);

        dialog.show();
    }

    private LinearLayout utangChoiceRow(int iconType, String label) {

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

        IconViews.IconView icon = new IconViews.IconView(context, iconType, BLUE);
        row.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextSize(14);
        text.setTextColor(NAVY);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textParams.leftMargin = dp(14);
        row.addView(text, textParams);

        return row;
    }

    private void showNewUtangCustomerForm() {

        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(20), dp(12), dp(20), dp(4));

        EditText nameInput = new EditText(context);
        nameInput.setHint("Pangalan");
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.bottomMargin = dp(8);
        view.addView(nameInput, nameParams);

        EditText phoneInput = new EditText(context);
        phoneInput.setHint("Numero (opsyonal)");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        view.addView(phoneInput);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Bagong Customer")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Isumite", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitBtn.setOnClickListener(v -> {

                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    nameInput.setError("Kailangan");
                    return;
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(context, "Kailangan naka-login", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> customerData = new HashMap<>();
                customerData.put("name", name);
                customerData.put("phone", phoneInput.getText().toString().trim());
                customerData.put("balance", 0.0);
                customerData.put("createdAt", System.currentTimeMillis());

                com.google.firebase.database.DatabaseReference newRef = FirebaseDatabase.getInstance()
                        .getReference("default_inventory")
                        .child(user.getUid())
                        .child("utangCustomers")
                        .push();

                newRef.setValue(customerData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        performCheckoutUtang(newRef.getKey(), name, 0.0);
                    } else {
                        Toast.makeText(context, "Hindi na-save ang customer", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void showUtangCustomerSearchDialog() {

        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(8), dp(16), dp(4));

        EditText searchBox = new EditText(context);
        searchBox.setHint("I-search ang pangalan");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        searchParams.bottomMargin = dp(10);
        view.addView(searchBox, searchParams);

        MaxHeightScrollView resultsScroll = new MaxHeightScrollView(context, dp(280));
        LinearLayout resultsContainer = new LinearLayout(context);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        resultsScroll.addView(resultsContainer);
        view.addView(resultsScroll);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Piliin ang Customer")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        Runnable[] refreshResults = new Runnable[1];
        refreshResults[0] = () -> {

            String query = searchBox.getText().toString().trim().toLowerCase(Locale.getDefault());
            resultsContainer.removeAllViews();

            List<Customer> matches = new ArrayList<>();
            for (Customer c : allCustomers) {
                if (query.isEmpty() || (c.getName() != null
                        && c.getName().toLowerCase(Locale.getDefault()).contains(query))) {
                    matches.add(c);
                }
            }

            if (matches.isEmpty()) {
                TextView empty = new TextView(context);
                empty.setText(allCustomers.isEmpty()
                        ? "Wala pang existing customer." : "Walang nahanap.");
                empty.setTextSize(12);
                empty.setTextColor(GRAY_TEXT);
                empty.setPadding(dp(4), dp(12), dp(4), dp(12));
                resultsContainer.addView(empty);
                return;
            }

            for (Customer customer : matches) {

                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10), dp(12), dp(10), dp(12));
                row.setClickable(true);
                row.setFocusable(true);

                android.util.TypedValue outValue = new android.util.TypedValue();
                context.getTheme().resolveAttribute(
                        android.R.attr.selectableItemBackground, outValue, true
                );
                row.setBackgroundResource(outValue.resourceId != 0 ? outValue.resourceId : 0);

                TextView nameView = new TextView(context);
                nameView.setText(customer.getName());
                nameView.setTextSize(14);
                nameView.setTextColor(NAVY);
                LinearLayout.LayoutParams nameViewParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                );
                row.addView(nameView, nameViewParams);

                TextView balanceView = new TextView(context);
                balanceView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", customer.getBalance()));
                balanceView.setTextSize(12);
                balanceView.setTextColor(customer.getBalance() > 0 ? RED : GRAY_TEXT);
                row.addView(balanceView);

                row.setOnClickListener(v -> {
                    dialog.dismiss();
                    performCheckoutUtang(customer.getKey(), customer.getName(), customer.getBalance());
                });

                resultsContainer.addView(row);
            }
        };

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshResults[0].run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        refreshResults[0].run();
        dialog.show();
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

    private void showEwalletDialog(String methodName, String number, String accountName) {

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
        view.addView(spacer(14));

        LinearLayout numberCard = new LinearLayout(context);
        numberCard.setOrientation(LinearLayout.VERTICAL);
        numberCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable numberBg = new GradientDrawable();
        numberBg.setColor(Color.rgb(246, 248, 252));
        numberBg.setCornerRadius(dp(10));
        numberCard.setBackground(numberBg);

        TextView numberLabel = new TextView(context);
        numberLabel.setText(methodName + " Number");
        numberLabel.setTextSize(11);
        numberLabel.setTextColor(GRAY_TEXT);
        numberCard.addView(numberLabel);

        TextView numberValue = new TextView(context);
        numberValue.setText(number + (accountName != null && !accountName.isEmpty() ? " (" + accountName + ")" : ""));
        numberValue.setTextSize(15);
        numberValue.setTypeface(null, Typeface.BOLD);
        numberValue.setTextColor(NAVY);
        numberCard.addView(numberValue);

        view.addView(numberCard);
        view.addView(spacer(6));

        TextView hint = new TextView(context);
        hint.setText("Ipakita ito sa customer para magpadala via " + methodName + ".");
        hint.setTextSize(11);
        hint.setTextColor(GRAY_TEXT);
        view.addView(hint);
        view.addView(spacer(14));

        TextView refLabel = new TextView(context);
        refLabel.setText("Reference / Transaction Number");
        refLabel.setTextSize(12);
        refLabel.setTextColor(GRAY_TEXT);
        view.addView(refLabel);

        EditText refInput = new EditText(context);
        refInput.setHint("Mula sa resibo ng " + methodName);
        refInput.setTextSize(15);
        refInput.setTextColor(NAVY);
        refInput.setSingleLine(true);
        refInput.setPadding(dp(4), dp(8), dp(4), dp(8));
        view.addView(refInput);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(methodName + " Payment")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Done", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button doneBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            doneBtn.setOnClickListener(v -> {

                String reference = refInput.getText().toString().trim();

                if (reference.isEmpty()) {
                    Toast.makeText(context, "Ilagay ang reference/transaction number", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                performCheckout(methodName, null, null, reference);
            });
        });

        dialog.show();
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
        performCheckout(paymentMethod, null, null, null, null, null, 0);
    }

    private void performCheckout(String paymentMethod, Double cashReceived, Double change) {
        performCheckout(paymentMethod, cashReceived, change, null, null, null, 0);
    }

    private void performCheckout(
            String paymentMethod, Double cashReceived, Double change, String referenceNumber) {
        performCheckout(paymentMethod, cashReceived, change, referenceNumber, null, null, 0);
    }

    private void performCheckoutUtang(String utangCustomerKey, String utangCustomerName, double utangCurrentBalance) {
        performCheckout("Utang", null, null, null, utangCustomerKey, utangCustomerName, utangCurrentBalance);
    }

    private void performCheckout(
            String paymentMethod, Double cashReceived, Double change, String referenceNumber,
            String utangCustomerKey, String utangCustomerName, double utangCurrentBalance) {

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
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            saleMap.put("referenceNumber", referenceNumber);
        }
        if (utangCustomerName != null) {
            saleMap.put("utangCustomerName", utangCustomerName);
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

        // Utang - idagdag ang halaga sa balance ng customer, at mag-log ng
        // "charge" na transaction na kasing-halaga ng buong resibo.
        if (utangCustomerKey != null) {

            double newBalance = utangCurrentBalance + totalAmount;
            updates.put(uid + "/utangCustomers/" + utangCustomerKey + "/balance", newBalance);

            String txKey = FirebaseDatabase.getInstance()
                    .getReference("default_inventory")
                    .child(uid)
                    .child("utangCustomers")
                    .child(utangCustomerKey)
                    .child("transactions")
                    .push()
                    .getKey();

            Map<String, Object> txData = new HashMap<>();
            txData.put("type", UtangTransaction.TYPE_CHARGE);
            txData.put("amount", totalAmount);
            txData.put("note", "Resibo #" + saleId);
            txData.put("date", dateFmt.format(nowDate));
            txData.put("time", timeFmt.format(nowDate));
            txData.put("createdAt", now);
            txData.put("items", saleItems);

            updates.put(uid + "/utangCustomers/" + utangCustomerKey + "/transactions/" + txKey, txData);
        }

        payButton.setEnabled(false);

        double finalTotalAmount = totalAmount;
        List<CartItem> receiptItems = new ArrayList<>(cart);
        String receiptDate = dateFmt.format(nowDate);
        String receiptTime = timeFmt.format(nowDate);

        FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .updateChildren(updates)
                .addOnCompleteListener(task -> {

                    payButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        cart.clear();
                        refreshCart();
                        showReceiptDialog(
                                saleId, receiptDate, receiptTime, paymentMethod,
                                receiptItems, finalTotalAmount,
                                cashReceived, change, referenceNumber, utangCustomerName
                        );
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
    // RECEIPT
    // -------------------------

    private void showReceiptDialog(
            String saleId,
            String date,
            String time,
            String paymentMethod,
            List<CartItem> items,
            double totalAmount,
            Double cashReceived,
            Double change,
            String referenceNumber,
            String utangCustomerName) {

        // Buong bungkos - zigzag taas + white card + zigzag baba
        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(16), dp(24), dp(16), dp(24));

        ZigzagEdgeView topEdge = new ZigzagEdgeView(context, true, Color.WHITE, 14f);
        outer.addView(topEdge, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(12)
        ));

        MaxHeightScrollView scroll = new MaxHeightScrollView(context, dp(480));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        scroll.setBackground(cardBg);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollParams.weight = 0;
        scroll.setLayoutParams(scrollParams);

        LinearLayout receiptContent = buildReceiptContent(
                saleId, date, time, paymentMethod, items,
                totalAmount, cashReceived, change, referenceNumber, utangCustomerName
        );
        scroll.addView(receiptContent);
        outer.addView(scroll);

        ZigzagEdgeView bottomEdge = new ZigzagEdgeView(context, false, Color.WHITE, 14f);
        outer.addView(bottomEdge, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(12)
        ));

        // Print + Close buttons sa ilalim ng card
        LinearLayout buttonRow = new LinearLayout(context);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setPadding(0, dp(16), 0, 0);

        // "Naglilimbag..." indicator - tago muna, aakyat pag pinindot ang Print
        LinearLayout printingIndicator = new LinearLayout(context);
        printingIndicator.setOrientation(LinearLayout.HORIZONTAL);
        printingIndicator.setGravity(Gravity.CENTER);
        printingIndicator.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable printingBg = new GradientDrawable();
        printingBg.setColor(Color.rgb(232, 239, 250));
        printingBg.setCornerRadius(dp(12));
        printingIndicator.setBackground(printingBg);

        TextView printingText = new TextView(context);
        printingText.setText("\uD83D\uDDA8 Naglilimbag...");
        printingText.setTextSize(13);
        printingText.setTypeface(null, Typeface.BOLD);
        printingText.setTextColor(BLUE);
        printingIndicator.addView(printingText);

        printingIndicator.setVisibility(View.GONE);
        LinearLayout.LayoutParams printingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        printingParams.bottomMargin = dp(10);
        outer.addView(printingIndicator, printingParams);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(outer)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout printBtn = flatDialogButton("Print", BLUE, true);
        printBtn.setOnClickListener(v -> {

            printingIndicator.setVisibility(View.VISIBLE);
            printingIndicator.setTranslationY(dp(40));
            printingIndicator.setAlpha(0f);
            printingIndicator.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> printReceipt(
                            saleId, date, time, paymentMethod, items,
                            totalAmount, cashReceived, change, referenceNumber, utangCustomerName
                    ))
                    .start();
        });
        LinearLayout.LayoutParams printParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        printParams.rightMargin = dp(8);
        buttonRow.addView(printBtn, printParams);

        LinearLayout closeBtn = flatDialogButton("Close", NAVY, false);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        buttonRow.addView(closeBtn, closeParams);

        outer.addView(buttonRow);

        dialog.setOnShowListener(d -> {
            // Slide-up + fade-in papasok ang resibo (dahan-dahan)
            outer.setTranslationY(dp(300));
            outer.setAlpha(0f);
            outer.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(650)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });

        dialog.show();
    }

    private LinearLayout flatDialogButton(String label, int color, boolean filled) {

        LinearLayout button = new LinearLayout(context);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(12), 0, dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (filled) {
            bg.setColor(color);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), LIGHT_BORDER);
        }
        button.setBackground(bg);

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextSize(13);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(filled ? Color.WHITE : color);
        button.addView(text);

        return button;
    }

    /** Nilalaman lang ng resibo - ginagamit parehong sa dialog at sa print. */
    private LinearLayout buildReceiptContent(
            String saleId,
            String date,
            String time,
            String paymentMethod,
            List<CartItem> items,
            double totalAmount,
            Double cashReceived,
            Double change,
            String referenceNumber,
            String utangCustomerName) {

        LinearLayout receipt = new LinearLayout(context);
        receipt.setOrientation(LinearLayout.VERTICAL);
        receipt.setPadding(dp(20), dp(16), dp(20), dp(16));

        TextView successLabel = new TextView(context);
        successLabel.setText("\u2713 Payment Successful");
        successLabel.setTextSize(15);
        successLabel.setTypeface(null, Typeface.BOLD);
        successLabel.setTextColor(GREEN_COLOR());
        successLabel.setGravity(Gravity.CENTER);
        receipt.addView(successLabel);
        receipt.addView(spacer(14));

        ImageView logoView = new ImageView(context);
        logoView.setImageResource(R.drawable.ic_stockflow_logo);
        logoView.setAdjustViewBounds(true);
        logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)
        );
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.bottomMargin = dp(6);
        receipt.addView(logoView, logoParams);

        String storeName = (paymentSettings != null
                && paymentSettings.getStoreName() != null
                && !paymentSettings.getStoreName().isEmpty())
                ? paymentSettings.getStoreName() : "StockFlow POS";

        TextView storeNameView = new TextView(context);
        storeNameView.setText(storeName);
        storeNameView.setTextSize(18);
        storeNameView.setTypeface(null, Typeface.BOLD);
        storeNameView.setTextColor(NAVY);
        storeNameView.setGravity(Gravity.CENTER);
        receipt.addView(storeNameView);

        TextView dateView = new TextView(context);
        dateView.setText(date + " \u2022 " + time);
        dateView.setTextSize(12);
        dateView.setTextColor(GRAY_TEXT);
        dateView.setGravity(Gravity.CENTER);
        receipt.addView(dateView);

        receipt.addView(spacer(14));
        receipt.addView(dashedDivider());
        receipt.addView(spacer(10));

        for (CartItem item : items) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4), 0, dp(4));

            LinearLayout nameCol = new LinearLayout(context);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nameColParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );

            TextView nameView = new TextView(context);
            nameView.setText(item.product.getName());
            nameView.setTextSize(13);
            nameView.setTextColor(NAVY);
            nameCol.addView(nameView);

            TextView qtyPriceView = new TextView(context);
            qtyPriceView.setText(item.quantity + " x \u20B1"
                    + String.format(Locale.getDefault(), "%.2f", item.product.getSellingPrice()));
            qtyPriceView.setTextSize(11);
            qtyPriceView.setTextColor(GRAY_TEXT);
            nameCol.addView(qtyPriceView);

            row.addView(nameCol, nameColParams);

            TextView subtotalView = new TextView(context);
            subtotalView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", item.getSubtotal()));
            subtotalView.setTextSize(13);
            subtotalView.setTypeface(null, Typeface.BOLD);
            subtotalView.setTextColor(NAVY);
            row.addView(subtotalView);

            receipt.addView(row);
        }

        receipt.addView(spacer(10));
        receipt.addView(dashedDivider());
        receipt.addView(spacer(10));

        receipt.addView(receiptRow("Total", "\u20B1" + String.format(Locale.getDefault(), "%.2f", totalAmount), true));
        receipt.addView(receiptRow("Payment Method", paymentMethod, false));

        if (cashReceived != null) {
            receipt.addView(receiptRow("Cash Received", "\u20B1" + String.format(Locale.getDefault(), "%.2f", cashReceived), false));
        }
        if (change != null) {
            receipt.addView(receiptRow("Change", "\u20B1" + String.format(Locale.getDefault(), "%.2f", change), false));
        }
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            receipt.addView(receiptRow("Reference No.", referenceNumber, false));
        }
        if (utangCustomerName != null && !utangCustomerName.isEmpty()) {
            receipt.addView(receiptRow("Utang ni", utangCustomerName, false));
        }

        receipt.addView(spacer(10));
        receipt.addView(dashedDivider());
        receipt.addView(spacer(10));

        receipt.addView(receiptRow("Transaction No.", saleId, false));

        receipt.addView(spacer(16));

        TextView thanksView = new TextView(context);
        thanksView.setText("Salamat sa pagbili!");
        thanksView.setTextSize(12);
        thanksView.setTextColor(GRAY_TEXT);
        thanksView.setGravity(Gravity.CENTER);
        receipt.addView(thanksView);

        return receipt;
    }

    private View receiptRow(String label, String value, boolean emphasize) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(emphasize ? 15 : 12);
        labelView.setTypeface(null, emphasize ? Typeface.BOLD : Typeface.NORMAL);
        labelView.setTextColor(emphasize ? NAVY : GRAY_TEXT);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(labelView, labelParams);

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextSize(emphasize ? 15 : 12);
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setTextColor(emphasize ? BLUE : NAVY);
        row.addView(valueView);

        return row;
    }

    private View dashedDivider() {
        View line = new View(context);
        line.setBackgroundColor(LIGHT_BORDER);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ));
        return line;
    }

    /** Plain ScrollView ay walang setMaxHeight() - dito natin ito idinadagdag. */
    private static class MaxHeightScrollView extends ScrollView {

        private final int maxHeightPx;

        MaxHeightScrollView(Context context, int maxHeightPx) {
            super(context);
            this.maxHeightPx = maxHeightPx;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

            int heightSpec = heightMeasureSpec;

            if (View.MeasureSpec.getMode(heightMeasureSpec) != View.MeasureSpec.EXACTLY) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(maxHeightPx, View.MeasureSpec.AT_MOST);
            }

            super.onMeasure(widthMeasureSpec, heightSpec);
        }
    }

    /**
     * Zigzag/torn-paper na gilid, gaya ng putol na resibo mula sa
     * thermal printer. pointUp=true kapag nasa itaas ng card ito
     * (dulo tumuturo pataas), false kapag nasa ibaba (tumuturo pababa).
     */
    private static class ZigzagEdgeView extends View {

        private final boolean pointUp;
        private final int fillColor;
        private final float toothWidthDp;
        private final float density;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ZigzagEdgeView(Context context, boolean pointUp, int fillColor, float toothWidthDp) {
            super(context);
            this.pointUp = pointUp;
            this.fillColor = fillColor;
            this.toothWidthDp = toothWidthDp;
            this.density = context.getResources().getDisplayMetrics().density;
            paint.setColor(fillColor);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            float toothWidthPx = toothWidthDp * density;
            int count = Math.max(1, Math.round(width / toothWidthPx));
            float toothWidth = width / count;

            Path path = new Path();

            if (pointUp) {
                // Flush sa ibaba (kasama ng card), tumuturo pataas ang mga tulis
                path.moveTo(0, height);
                for (int i = 0; i < count; i++) {
                    float midX = (i + 0.5f) * toothWidth;
                    float nextX = (i + 1f) * toothWidth;
                    path.lineTo(midX, 0);
                    path.lineTo(nextX, height);
                }
            } else {
                // Flush sa itaas (kasama ng card), tumuturo pababa ang mga tulis
                path.moveTo(0, 0);
                for (int i = 0; i < count; i++) {
                    float midX = (i + 0.5f) * toothWidth;
                    float nextX = (i + 1f) * toothWidth;
                    path.lineTo(midX, height);
                    path.lineTo(nextX, 0);
                }
            }

            path.close();
            canvas.drawPath(path, paint);
        }
    }

    // -------------------------
    // PRINT
    // -------------------------

    private void printReceipt(
            String saleId,
            String date,
            String time,
            String paymentMethod,
            List<CartItem> items,
            double totalAmount,
            Double cashReceived,
            Double change,
            String referenceNumber,
            String utangCustomerName) {

        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        if (printManager == null) {
            Toast.makeText(context, "Hindi available ang print service sa device na ito", Toast.LENGTH_LONG).show();
            return;
        }

        // Bagong, hiwalay na (unattached) na view lang para sa pag-print,
        // hindi yung nasa dialog na - para ligtas itong ma-measure/i-layout ulit.
        LinearLayout printContent = buildReceiptContent(
                saleId, date, time, paymentMethod, items,
                totalAmount, cashReceived, change, referenceNumber, utangCustomerName
        );
        printContent.setBackgroundColor(Color.WHITE);

        String jobName = "Resibo_" + saleId;

        PrintDocumentAdapter adapter = new PrintDocumentAdapter() {

            private PrintedPdfDocument pdfDocument;

            @Override
            public void onLayout(
                    PrintAttributes oldAttributes,
                    PrintAttributes newAttributes,
                    CancellationSignal cancellationSignal,
                    LayoutResultCallback callback,
                    Bundle extras) {

                pdfDocument = new PrintedPdfDocument(context, newAttributes);

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build();

                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(
                    PageRange[] pages,
                    ParcelFileDescriptor destination,
                    CancellationSignal cancellationSignal,
                    WriteResultCallback callback) {

                PdfDocument.Page page = pdfDocument.startPage(1);

                int pageWidth = page.getInfo().getPageWidth();

                int widthSpec = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                printContent.measure(widthSpec, heightSpec);
                printContent.layout(0, 0, pageWidth, printContent.getMeasuredHeight());

                printContent.draw(page.getCanvas());
                pdfDocument.finishPage(page);

                try {
                    pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                } finally {
                    pdfDocument.close();
                    pdfDocument = null;
                }
            }

            @Override
            public void onFinish() {
                super.onFinish();
            }
        };

        printManager.print(jobName, adapter, new PrintAttributes.Builder().build());
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
