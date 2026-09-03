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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utang (customer credit) screen - list of customers with a
 * balance, each customer has a transaction history where every
 * "charge" shows the actual products bought (table-style), and
 * new charges are built by searching/picking products from
 * inventory instead of a plain amount field.
 */
public class UtangActivity extends AppCompatActivity {

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int BLUE_DARK = Color.rgb(30, 80, 200);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int RED = Color.rgb(230, 70, 70);
    private final int LIGHT_BORDER = Color.rgb(225, 231, 240);

    private float density;

    private List<Customer> allCustomers = new ArrayList<>();
    private DatabaseReference customersRef;
    private ValueEventListener customersListener;

    private List<Product> allProducts = new ArrayList<>();
    private DatabaseReference inventoryRef;
    private ValueEventListener inventoryListener;

    private LinearLayout listContainer;
    private TextView totalUtangValue;

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        density = getResources().getDisplayMetrics().density;

        setContentView(buildContent());

        startListeningToCustomers();
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

        column.addView(buildTotalCard());
        column.addView(spacer(20));

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("Customers");
        sectionTitle.setTextSize(16);
        sectionTitle.setTypeface(null, Typeface.BOLD);
        sectionTitle.setTextColor(NAVY);
        column.addView(sectionTitle);
        column.addView(spacer(10));

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

        TextView backBtn = new TextView(this);
        backBtn.setText("\u2190");
        backBtn.setTextSize(20);
        backBtn.setTextColor(NAVY);
        backBtn.setPadding(dp(6), dp(4), dp(14), dp(4));
        backBtn.setOnClickListener(v -> finish());
        row.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("Utang");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        row.addView(title, titleParams);

        LinearLayout addBtn = new LinearLayout(this);
        addBtn.setOrientation(LinearLayout.HORIZONTAL);
        addBtn.setGravity(Gravity.CENTER_VERTICAL);
        addBtn.setPadding(dp(14), dp(9), dp(16), dp(9));

        GradientDrawable addBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{BLUE, BLUE_DARK}
        );
        addBg.setCornerRadius(dp(16));
        addBtn.setBackground(addBg);

        IconViews.IconView plusIcon = new IconViews.IconView(this, IconViews.TYPE_PLUS, Color.WHITE);
        addBtn.addView(plusIcon, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView addText = new TextView(this);
        addText.setText("Add");
        addText.setTextSize(13);
        addText.setTypeface(null, Typeface.BOLD);
        addText.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams addTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addTextParams.leftMargin = dp(6);
        addBtn.addView(addText, addTextParams);

        addBtn.setOnClickListener(v -> showAddCustomerDialog());
        row.addView(addBtn);

        return row;
    }

    private View buildTotalCard() {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(222, 233, 250));
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);

        TextView label = new TextView(this);
        label.setText("Total Outstanding Utang");
        label.setTextSize(13);
        label.setTextColor(NAVY);
        card.addView(label);

        totalUtangValue = new TextView(this);
        totalUtangValue.setText("\u20B10.00");
        totalUtangValue.setTextSize(26);
        totalUtangValue.setTypeface(null, Typeface.BOLD);
        totalUtangValue.setTextColor(BLUE);
        card.addView(totalUtangValue);

        return card;
    }

    // -------------------------
    // FIREBASE - CUSTOMERS
    // -------------------------

    private void startListeningToCustomers() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        customersRef = FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("utangCustomers");

        customersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Customer> customers = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Customer customer = child.getValue(Customer.class);
                    if (customer != null) {
                        customer.setKey(child.getKey());
                        customers.add(customer);
                    }
                }

                allCustomers = customers;
                refreshList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        UtangActivity.this,
                        "Unable to load customers: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        customersRef.addValueEventListener(customersListener);
    }

    // -------------------------
    // FIREBASE - INVENTORY (para sa product picker)
    // -------------------------

    private void startListeningToInventory() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently skip - not critical for the list screen itself
            }
        };

        inventoryRef.addValueEventListener(inventoryListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (customersRef != null && customersListener != null) {
            customersRef.removeEventListener(customersListener);
        }
        if (inventoryRef != null && inventoryListener != null) {
            inventoryRef.removeEventListener(inventoryListener);
        }
    }

    // -------------------------
    // CUSTOMER LIST
    // -------------------------

    private void refreshList() {

        if (listContainer == null) {
            return;
        }

        listContainer.removeAllViews();

        double total = 0;
        for (Customer c : allCustomers) {
            total += c.getBalance();
        }
        totalUtangValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", total));

        if (allCustomers.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No customers yet. Tap \"Add\" to create one.");
            empty.setTextSize(13);
            empty.setTextColor(GRAY_TEXT);
            empty.setPadding(dp(4), dp(20), dp(4), dp(20));
            listContainer.addView(empty);
            return;
        }

        List<Customer> sorted = new ArrayList<>(allCustomers);
        Collections.sort(sorted, (a, b) -> Double.compare(b.getBalance(), a.getBalance()));

        for (Customer customer : sorted) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.bottomMargin = dp(10);
            listContainer.addView(customerRow(customer), p);
        }
    }

    private View customerRow(Customer customer) {

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

        FrameLayout avatarWrap = new FrameLayout(this);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.rgb(232, 239, 250));
        avatarWrap.setBackground(avatarBg);

        IconViews.IconView personIcon = new IconViews.IconView(this, IconViews.TYPE_PERSON, BLUE);
        avatarWrap.addView(personIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        row.addView(avatarWrap, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textColParams.leftMargin = dp(12);

        TextView nameView = new TextView(this);
        nameView.setText(customer.getName());
        nameView.setTextSize(14);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        textCol.addView(nameView);

        if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            TextView phoneView = new TextView(this);
            phoneView.setText(customer.getPhone());
            phoneView.setTextSize(11);
            phoneView.setTextColor(GRAY_TEXT);
            textCol.addView(phoneView);
        }

        row.addView(textCol, textColParams);

        TextView balanceView = new TextView(this);
        balanceView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", customer.getBalance()));
        balanceView.setTextSize(15);
        balanceView.setTypeface(null, Typeface.BOLD);
        balanceView.setTextColor(customer.getBalance() > 0 ? RED : GREEN);
        row.addView(balanceView);

        row.setOnClickListener(v -> showCustomerDetailDialog(customer));

        return row;
    }

    // -------------------------
    // ADD CUSTOMER
    // -------------------------

    private void showAddCustomerDialog() {

        LinearLayout shell = modernDialogShell("Add Customer");

        TextInputLayout nameField = textField("Name", false);
        shell.addView(nameField);

        TextInputLayout phoneField = textField("Phone (optional)", false);
        shell.addView(phoneField);

        shell.addView(spacer(8));

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

        LinearLayout saveBtn = flatDialogButton("Add", BLUE, true);
        buttonRow.addView(saveBtn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));

        saveBtn.setOnClickListener(v -> {

            String name = fieldText(nameField);
            String phone = fieldText(phoneField);

            if (name.isEmpty()) {
                nameField.setError("Required");
                return;
            }
            nameField.setError(null);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("phone", phone);
            data.put("balance", 0.0);
            data.put("createdAt", System.currentTimeMillis());

            customersRef.push().setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Could not save: " + (task.getException() != null
                            ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    // -------------------------
    // CUSTOMER DETAIL (balance + history + charge/payment)
    // -------------------------

    private void showCustomerDetailDialog(Customer customer) {

        LinearLayout shell = modernDialogShell(customer.getName());

        TextView balanceLabel = new TextView(this);
        balanceLabel.setText("Current Balance");
        balanceLabel.setTextSize(12);
        balanceLabel.setTextColor(GRAY_TEXT);
        shell.addView(balanceLabel);

        TextView balanceValue = new TextView(this);
        balanceValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", customer.getBalance()));
        balanceValue.setTextSize(24);
        balanceValue.setTypeface(null, Typeface.BOLD);
        balanceValue.setTextColor(customer.getBalance() > 0 ? RED : GREEN);
        shell.addView(balanceValue);
        shell.addView(spacer(14));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(actionRow);
        shell.addView(spacer(14));

        TextView historyLabel = new TextView(this);
        historyLabel.setText("Transaction History");
        historyLabel.setTextSize(13);
        historyLabel.setTypeface(null, Typeface.BOLD);
        historyLabel.setTextColor(NAVY);
        shell.addView(historyLabel);
        shell.addView(spacer(8));

        MaxHeightScrollView historyScroll = new MaxHeightScrollView(this, dp(320));
        LinearLayout historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyScroll.addView(historyContainer);
        shell.addView(historyScroll);
        shell.addView(spacer(14));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(bottomRow);

        AlertDialog dialog = buildModernDialog(shell);

        LinearLayout chargeBtn = flatDialogButton("+ Add Utang", RED, false);
        LinearLayout.LayoutParams chargeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        chargeParams.rightMargin = dp(6);
        actionRow.addView(chargeBtn, chargeParams);
        chargeBtn.setOnClickListener(v -> showAddChargeDialog(customer, dialog));

        LinearLayout paymentBtn = flatDialogButton("Payment", GREEN, true);
        actionRow.addView(paymentBtn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));
        paymentBtn.setOnClickListener(v -> showAddPaymentDialog(customer, dialog));

        LinearLayout deleteBtn = flatDialogButton("Delete Customer", RED, false);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        deleteParams.rightMargin = dp(6);
        bottomRow.addView(deleteBtn, deleteParams);
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteCustomerConfirm(customer);
        });

        LinearLayout closeBtn = flatDialogButton("Close", NAVY, true);
        bottomRow.addView(closeBtn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        loadTransactionHistory(customer, historyContainer);

        dialog.show();
    }

    private void loadTransactionHistory(Customer customer, LinearLayout container) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("utangCustomers")
                .child(customer.getKey())
                .child("transactions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<UtangTransaction> txList = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            UtangTransaction tx = child.getValue(UtangTransaction.class);
                            if (tx != null) {
                                txList.add(tx);
                            }
                        }

                        Collections.sort(txList, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                        container.removeAllViews();

                        if (txList.isEmpty()) {
                            TextView empty = new TextView(UtangActivity.this);
                            empty.setText("No transactions yet.");
                            empty.setTextSize(12);
                            empty.setTextColor(GRAY_TEXT);
                            container.addView(empty);
                            return;
                        }

                        for (UtangTransaction tx : txList) {
                            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            p.bottomMargin = dp(10);
                            container.addView(transactionCard(tx), p);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Silently skip
                    }
                });
    }

    /**
     * Isang transaksyon = isang card. Header ay ang date/time, tapos
     * kung "charge" (may binili), ipinapakita bilang Product/Price/
     * Qty/Total na table. Kung "payment", isang linya lang.
     */
    private View transactionCard(UtangTransaction tx) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(248, 250, 253));
        bg.setCornerRadius(dp(10));
        card.setBackground(bg);

        boolean isCharge = UtangTransaction.TYPE_CHARGE.equals(tx.getType());

        // Header row: date/time + type badge + amount
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dateView = new TextView(this);
        dateView.setText(formatTransactionDateTime(tx));
        dateView.setTextSize(11.5f);
        dateView.setTypeface(null, Typeface.BOLD);
        dateView.setTextColor(GRAY_TEXT);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        headerRow.addView(dateView, dateParams);

        TextView amountView = new TextView(this);
        amountView.setText((isCharge ? "+" : "-") + "\u20B1"
                + String.format(Locale.getDefault(), "%.2f", tx.getAmount()));
        amountView.setTextSize(13);
        amountView.setTypeface(null, Typeface.BOLD);
        amountView.setTextColor(isCharge ? RED : GREEN);
        headerRow.addView(amountView);

        card.addView(headerRow);

        if (isCharge && tx.getItems() != null && !tx.getItems().isEmpty()) {

            card.addView(spacer(8));
            card.addView(divider());
            card.addView(spacer(6));

            // Table header
            LinearLayout tableHeader = new LinearLayout(this);
            tableHeader.setOrientation(LinearLayout.HORIZONTAL);
            tableHeader.addView(tableCell("Product", 2f, true, NAVY));
            tableHeader.addView(tableCell("Price", 1f, true, NAVY));
            tableHeader.addView(tableCell("Qty", 0.6f, true, NAVY));
            tableHeader.addView(tableCell("Total", 1f, true, NAVY));
            card.addView(tableHeader);

            card.addView(spacer(4));

            for (SaleItem item : tx.getItems()) {

                LinearLayout itemRow = new LinearLayout(this);
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setPadding(0, dp(2), 0, dp(2));

                itemRow.addView(tableCell(item.getName(), 2f, false, NAVY));
                itemRow.addView(tableCell(
                        "\u20B1" + String.format(Locale.getDefault(), "%.2f", item.getPrice()),
                        1f, false, GRAY_TEXT
                ));
                itemRow.addView(tableCell(String.valueOf(item.getQty()), 0.6f, false, GRAY_TEXT));
                itemRow.addView(tableCell(
                        "\u20B1" + String.format(Locale.getDefault(), "%.2f", item.getSubtotal()),
                        1f, false, NAVY
                ));

                card.addView(itemRow);
            }

        } else if (tx.getNote() != null && !tx.getNote().isEmpty()) {

            TextView noteView = new TextView(this);
            noteView.setText(tx.getNote());
            noteView.setTextSize(11.5f);
            noteView.setTextColor(GRAY_TEXT);
            noteView.setPadding(0, dp(4), 0, 0);
            card.addView(noteView);
        }

        return card;
    }

    private View tableCell(String text, float weight, boolean header, int color) {

        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setTextSize(header ? 10.5f : 11.5f);
        cell.setTypeface(null, header ? Typeface.BOLD : Typeface.NORMAL);
        cell.setTextColor(color);
        cell.setMaxLines(2);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight
        );
        cell.setLayoutParams(p);

        return cell;
    }

    private String formatTransactionDateTime(UtangTransaction tx) {

        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date parsed = parser.parse(tx.getDate() + " " + tx.getTime());

            SimpleDateFormat display = new SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.US);
            return display.format(parsed);

        } catch (Exception e) {
            return tx.getDate() + " " + tx.getTime();
        }
    }

    // -------------------------
    // ADD UTANG (product picker - search + mini cart)
    // -------------------------

    private void showAddChargeDialog(Customer customer, AlertDialog parentDialog) {

        LinearLayout shell = modernDialogShell("Add Utang - " + customer.getName());

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(dp(12), dp(4), dp(12), dp(4));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.rgb(246, 248, 252));
        searchBg.setCornerRadius(dp(12));
        searchRow.setBackground(searchBg);

        EditText searchInput = new EditText(this);
        searchInput.setHint("Search product or barcode");
        searchInput.setTextSize(13);
        searchInput.setBackground(null);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(6), dp(10), dp(6), dp(10));
        LinearLayout.LayoutParams searchInputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        searchRow.addView(searchInput, searchInputParams);

        shell.addView(searchRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        shell.addView(spacer(6));

        MaxHeightScrollView suggestionsScroll = new MaxHeightScrollView(this, dp(140));
        LinearLayout suggestionsContainer = new LinearLayout(this);
        suggestionsContainer.setOrientation(LinearLayout.VERTICAL);
        suggestionsScroll.addView(suggestionsContainer);
        suggestionsScroll.setVisibility(View.GONE);
        shell.addView(suggestionsScroll);
        shell.addView(spacer(10));

        TextView cartLabel = new TextView(this);
        cartLabel.setText("Items to charge");
        cartLabel.setTextSize(12);
        cartLabel.setTypeface(null, Typeface.BOLD);
        cartLabel.setTextColor(NAVY);
        shell.addView(cartLabel);
        shell.addView(spacer(6));

        MaxHeightScrollView cartScroll = new MaxHeightScrollView(this, dp(200));
        LinearLayout cartContainer = new LinearLayout(this);
        cartContainer.setOrientation(LinearLayout.VERTICAL);
        cartScroll.addView(cartContainer);
        shell.addView(cartScroll);
        shell.addView(spacer(10));

        LinearLayout totalRow = new LinearLayout(this);
        totalRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView totalLabel = new TextView(this);
        totalLabel.setText("Total");
        totalLabel.setTextSize(14);
        totalLabel.setTypeface(null, Typeface.BOLD);
        totalLabel.setTextColor(NAVY);
        LinearLayout.LayoutParams totalLabelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        totalRow.addView(totalLabel, totalLabelParams);
        TextView totalValue = new TextView(this);
        totalValue.setText("\u20B10.00");
        totalValue.setTextSize(16);
        totalValue.setTypeface(null, Typeface.BOLD);
        totalValue.setTextColor(RED);
        totalRow.addView(totalValue);
        shell.addView(totalRow);
        shell.addView(spacer(14));

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(buttonRow);

        AlertDialog dialog = buildModernDialog(shell);

        List<CartItem> chargeCart = new ArrayList<>();

        Runnable[] refreshCart = new Runnable[1];
        refreshCart[0] = () -> {

            cartContainer.removeAllViews();
            double total = 0;

            if (chargeCart.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No items added yet.");
                empty.setTextSize(11.5f);
                empty.setTextColor(GRAY_TEXT);
                empty.setPadding(0, dp(6), 0, dp(6));
                cartContainer.addView(empty);
            }

            for (CartItem item : chargeCart) {

                total += item.getSubtotal();

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(6), 0, dp(6));

                TextView nameView = new TextView(this);
                nameView.setText(item.product.getName() + " x" + item.quantity);
                nameView.setTextSize(12.5f);
                nameView.setTextColor(NAVY);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                );
                row.addView(nameView, nameParams);

                TextView subtotalView = new TextView(this);
                subtotalView.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", item.getSubtotal()));
                subtotalView.setTextSize(12.5f);
                subtotalView.setTypeface(null, Typeface.BOLD);
                subtotalView.setTextColor(NAVY);
                row.addView(subtotalView);

                TextView removeView = new TextView(this);
                removeView.setText(" \u2715");
                removeView.setTextSize(12.5f);
                removeView.setTypeface(null, Typeface.BOLD);
                removeView.setTextColor(RED);
                removeView.setPadding(dp(8), 0, 0, 0);
                removeView.setOnClickListener(v -> {
                    chargeCart.remove(item);
                    refreshCart[0].run();
                });
                row.addView(removeView);

                cartContainer.addView(row);
            }

            totalValue.setText("\u20B1" + String.format(Locale.getDefault(), "%.2f", total));
        };

        Runnable[] refreshSuggestions = new Runnable[1];
        refreshSuggestions[0] = () -> {

            String query = searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());
            suggestionsContainer.removeAllViews();

            if (query.isEmpty()) {
                suggestionsScroll.setVisibility(View.GONE);
                return;
            }

            List<Product> matches = new ArrayList<>();
            for (Product p : allProducts) {
                boolean barcodeMatch = p.getBarcode() != null && p.getBarcode().equals(query);
                boolean nameMatch = p.getName() != null
                        && p.getName().toLowerCase(Locale.getDefault()).contains(query);
                if (barcodeMatch || nameMatch) {
                    matches.add(p);
                }
                if (matches.size() >= 6) {
                    break;
                }
            }

            if (matches.isEmpty()) {
                suggestionsScroll.setVisibility(View.GONE);
                return;
            }

            suggestionsScroll.setVisibility(View.VISIBLE);

            for (Product product : matches) {

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(10), dp(8), dp(10));
                row.setClickable(true);
                row.setFocusable(true);

                android.util.TypedValue outValue = new android.util.TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                row.setBackgroundResource(outValue.resourceId != 0 ? outValue.resourceId : 0);

                TextView nameView = new TextView(this);
                nameView.setText(product.getName() + " - \u20B1"
                        + String.format(Locale.getDefault(), "%.2f", product.getSellingPrice()));
                nameView.setTextSize(12.5f);
                nameView.setTextColor(NAVY);
                row.addView(nameView, new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ));

                row.setOnClickListener(v -> {

                    boolean found = false;
                    for (CartItem item : chargeCart) {
                        if (item.product.getBarcode() != null
                                && item.product.getBarcode().equals(product.getBarcode())) {
                            item.quantity += 1;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        chargeCart.add(new CartItem(product, 1));
                    }

                    searchInput.setText("");
                    suggestionsScroll.setVisibility(View.GONE);
                    refreshCart[0].run();
                });

                suggestionsContainer.addView(row);
            }
        };

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshSuggestions[0].run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        refreshCart[0].run();

        LinearLayout cancelBtn = flatDialogButton("Cancel", NAVY, false);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        cancelParams.rightMargin = dp(8);
        buttonRow.addView(cancelBtn, cancelParams);

        LinearLayout saveBtn = flatDialogButton("Save Utang", RED, true);
        buttonRow.addView(saveBtn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));

        saveBtn.setOnClickListener(v -> {

            if (chargeCart.isEmpty()) {
                Toast.makeText(this, "Add at least one product", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = 0;
            List<Map<String, Object>> items = new ArrayList<>();
            for (CartItem item : chargeCart) {
                total += item.getSubtotal();

                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("barcode", item.product.getBarcode());
                itemMap.put("name", item.product.getName());
                itemMap.put("price", item.product.getSellingPrice());
                itemMap.put("qty", item.quantity);
                itemMap.put("subtotal", item.getSubtotal());
                itemMap.put("profit", item.getProfit());
                items.add(itemMap);
            }

            long now = System.currentTimeMillis();
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date nowDate = new Date(now);

            Map<String, Object> txData = new HashMap<>();
            txData.put("type", UtangTransaction.TYPE_CHARGE);
            txData.put("amount", total);
            txData.put("note", "");
            txData.put("date", dateFmt.format(nowDate));
            txData.put("time", timeFmt.format(nowDate));
            txData.put("createdAt", now);
            txData.put("items", items);

            double newBalance = customer.getBalance() + total;

            DatabaseReference customerRef = FirebaseDatabase.getInstance()
                    .getReference("default_inventory")
                    .child(user.getUid())
                    .child("utangCustomers")
                    .child(customer.getKey());

            Map<String, Object> updates = new HashMap<>();
            updates.put("balance", newBalance);
            updates.put("transactions/" + customerRef.child("transactions").push().getKey(), txData);

            customerRef.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Utang saved", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (parentDialog != null) {
                        parentDialog.dismiss();
                    }
                } else {
                    Toast.makeText(this, "Could not save: " + (task.getException() != null
                            ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    // -------------------------
    // ADD PAYMENT (simple amount)
    // -------------------------

    private void showAddPaymentDialog(Customer customer, AlertDialog parentDialog) {

        LinearLayout shell = modernDialogShell("Record Payment - " + customer.getName());

        TextInputLayout amountField = textField("Amount", true);
        shell.addView(amountField);

        TextInputLayout noteField = textField("Note (optional)", false);
        shell.addView(noteField);

        shell.addView(spacer(8));

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

        LinearLayout saveBtn = flatDialogButton("Save", GREEN, true);
        buttonRow.addView(saveBtn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));

        saveBtn.setOnClickListener(v -> {

            String amountStr = fieldText(amountField);
            if (amountStr.isEmpty()) {
                amountField.setError("Required");
                return;
            }
            amountField.setError(null);

            double amount = Double.parseDouble(amountStr);
            String note = fieldText(noteField);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            long now = System.currentTimeMillis();
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date nowDate = new Date(now);

            Map<String, Object> txData = new HashMap<>();
            txData.put("type", UtangTransaction.TYPE_PAYMENT);
            txData.put("amount", amount);
            txData.put("note", note);
            txData.put("date", dateFmt.format(nowDate));
            txData.put("time", timeFmt.format(nowDate));
            txData.put("createdAt", now);

            double newBalance = customer.getBalance() - amount;

            DatabaseReference customerRef = FirebaseDatabase.getInstance()
                    .getReference("default_inventory")
                    .child(user.getUid())
                    .child("utangCustomers")
                    .child(customer.getKey());

            Map<String, Object> updates = new HashMap<>();
            updates.put("balance", newBalance);
            updates.put("transactions/" + customerRef.child("transactions").push().getKey(), txData);

            customerRef.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Payment saved", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (parentDialog != null) {
                        parentDialog.dismiss();
                    }
                } else {
                    Toast.makeText(this, "Could not save: " + (task.getException() != null
                            ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void showDeleteCustomerConfirm(Customer customer) {

        new AlertDialog.Builder(this)
                .setTitle("Delete " + customer.getName() + "?")
                .setMessage("This will also remove their entire utang history.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        return;
                    }

                    customersRef.child(customer.getKey()).removeValue()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Could not delete", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
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

    private View divider() {
        View line = new View(this);
        line.setBackgroundColor(LIGHT_BORDER);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ));
        return line;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }

    /** Plain ScrollView has no setMaxHeight() - adding it here. */
    private static class MaxHeightScrollView extends ScrollView {

        private final int maxHeightPx;

        MaxHeightScrollView(android.content.Context context, int maxHeightPx) {
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
}
