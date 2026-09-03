package com.stock.flow;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Profile screen - user info + Logout. Dito pa lang natin
 * ilalagay yung ibang settings/profile options mamaya.
 */
public class ProfileHomeView {

    private final android.content.Context context;
    private final float density;

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int RED = Color.rgb(230, 70, 70);

    public ProfileHomeView(android.content.Context context) {
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

        TextView title = new TextView(context);
        title.setText("Profile");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        column.addView(title);
        column.addView(spacer(20));

        column.addView(buildUserCard());
        column.addView(spacer(16));

        column.addView(buildPaymentSettingsCard());
        column.addView(spacer(16));

        column.addView(buildUtangButton());
        column.addView(spacer(16));

        column.addView(buildMigrateButton());
        column.addView(spacer(16));

        column.addView(buildLogoutButton());

        scrollView.addView(column);
        return scrollView;
    }

    // -------------------------
    // USER INFO CARD
    // -------------------------

    private View buildUserCard() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        card.setElevation(dp(2));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String displayName = "User";
        String email = "";

        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                displayName = user.getDisplayName();
            } else if (user.getEmail() != null) {
                displayName = user.getEmail();
            }
            if (user.getEmail() != null) {
                email = user.getEmail();
            }
        }

        FrameLayout avatarWrap = new FrameLayout(context);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.rgb(222, 233, 250));
        avatarWrap.setBackground(avatarBg);

        IconViews.IconView personIcon = new IconViews.IconView(
                context, IconViews.TYPE_PERSON, BLUE
        );
        int iconSize = dp(24);
        avatarWrap.addView(personIcon, new FrameLayout.LayoutParams(
                iconSize, iconSize, Gravity.CENTER
        ));

        int avatarSize = dp(56);
        card.addView(avatarWrap, new LinearLayout.LayoutParams(avatarSize, avatarSize));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textColParams.leftMargin = dp(14);

        TextView nameView = new TextView(context);
        nameView.setText(displayName);
        nameView.setTextSize(15);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(NAVY);
        nameView.setMaxLines(1);
        textCol.addView(nameView);

        if (!email.isEmpty()) {
            TextView emailView = new TextView(context);
            emailView.setText(email);
            emailView.setTextSize(12);
            emailView.setTextColor(GRAY_TEXT);
            emailView.setMaxLines(1);
            textCol.addView(emailView);
        }

        card.addView(textCol, textColParams);

        return card;
    }

    // -------------------------
    // PAYMENT SETTINGS (GCash/Maya/Maribank/Card)
    // -------------------------

    private EditText storeNameInput;
    private EditText gcashNumberInput;
    private EditText gcashNameInput;
    private EditText mayaNumberInput;
    private EditText maribankNumberInput;
    private EditText cardDetailsInput;

    private View buildPaymentSettingsCard() {

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        card.setElevation(dp(2));

        // Title row - clickable, may arrow na nagto-toggle ng fields
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setClickable(true);
        titleRow.setFocusable(true);

        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true
        );
        titleRow.setBackgroundResource(outValue.resourceId != 0 ? outValue.resourceId : 0);

        TextView title = new TextView(context);
        title.setText("Payment Methods");
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        titleRow.addView(title, titleParams);

        TextView arrow = new TextView(context);
        arrow.setText("\u25BC");
        arrow.setTextSize(13);
        arrow.setTextColor(GRAY_TEXT);
        titleRow.addView(arrow);

        card.addView(titleRow);

        // Lahat ng fields - naka-tago (GONE) by default, ito lang ang
        // itinatago/ipinapakita ng arrow, hindi yung title row.
        LinearLayout fieldsContainer = new LinearLayout(context);
        fieldsContainer.setOrientation(LinearLayout.VERTICAL);
        fieldsContainer.setVisibility(View.GONE);
        LinearLayout.LayoutParams fieldsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fieldsParams.topMargin = dp(4);
        card.addView(fieldsContainer, fieldsParams);

        titleRow.setOnClickListener(v -> {
            boolean isVisible = fieldsContainer.getVisibility() == View.VISIBLE;
            fieldsContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            arrow.setText(isVisible ? "\u25BC" : "\u25B2");
        });

        TextView subtitle = new TextView(context);
        subtitle.setText("Ginagamit ito sa POS resibo, QR/reference, at checkout.");
        subtitle.setTextSize(11);
        subtitle.setTextColor(GRAY_TEXT);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dp(2);
        subParams.bottomMargin = dp(14);
        fieldsContainer.addView(subtitle, subParams);

        storeNameInput = paymentField("Store Name (lalabas sa resibo)", false);
        fieldsContainer.addView(storeNameInput);
        fieldsContainer.addView(spacer(10));

        gcashNumberInput = paymentField("GCash Number (hal. 09171234567)", false);
        fieldsContainer.addView(gcashNumberInput);
        fieldsContainer.addView(spacer(10));

        gcashNameInput = paymentField("GCash Account Name (opsyonal)", false);
        fieldsContainer.addView(gcashNameInput);
        fieldsContainer.addView(spacer(10));

        mayaNumberInput = paymentField("Maya Number", false);
        fieldsContainer.addView(mayaNumberInput);
        fieldsContainer.addView(spacer(10));

        maribankNumberInput = paymentField("Maribank Number", false);
        fieldsContainer.addView(maribankNumberInput);
        fieldsContainer.addView(spacer(10));

        cardDetailsInput = paymentField("Card Details / Notes (hal. \"Tap-to-pay terminal sa counter\")", true);
        fieldsContainer.addView(cardDetailsInput);

        TextView cardWarning = new TextView(context);
        cardWarning.setText("Para sa seguridad, huwag ilagay dito ang buong card number - "
                + "reference/notes lang.");
        cardWarning.setTextSize(10.5f);
        cardWarning.setTextColor(GRAY_TEXT);
        LinearLayout.LayoutParams warnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        warnParams.topMargin = dp(4);
        fieldsContainer.addView(cardWarning, warnParams);

        fieldsContainer.addView(spacer(14));

        LinearLayout saveBtn = new LinearLayout(context);
        saveBtn.setOrientation(LinearLayout.HORIZONTAL);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setPadding(0, dp(12), 0, dp(12));

        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(BLUE);
        saveBg.setCornerRadius(dp(14));
        saveBtn.setBackground(saveBg);

        TextView saveText = new TextView(context);
        saveText.setText("Save Payment Settings");
        saveText.setTextSize(13);
        saveText.setTypeface(null, Typeface.BOLD);
        saveText.setTextColor(Color.WHITE);
        saveBtn.addView(saveText);

        saveBtn.setOnClickListener(v -> handleSavePaymentSettings());
        fieldsContainer.addView(saveBtn);

        loadPaymentSettings();

        return card;
    }

    private EditText paymentField(String hint, boolean multiline) {

        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextSize(13);
        input.setTextColor(NAVY);
        input.setHintTextColor(GRAY_TEXT);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(246, 248, 252));
        bg.setCornerRadius(dp(10));
        input.setBackground(bg);

        if (multiline) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            input.setMinLines(2);
        } else {
            input.setSingleLine(true);
        }

        return input;
    }

    private void loadPaymentSettings() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("paymentSettings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        PaymentSettings settings = snapshot.getValue(PaymentSettings.class);
                        if (settings == null) {
                            return;
                        }

                        setIfNotNull(storeNameInput, settings.getStoreName());
                        setIfNotNull(gcashNumberInput, settings.getGcashNumber());
                        setIfNotNull(gcashNameInput, settings.getGcashName());
                        setIfNotNull(mayaNumberInput, settings.getMayaNumber());
                        setIfNotNull(maribankNumberInput, settings.getMaribankNumber());
                        setIfNotNull(cardDetailsInput, settings.getCardDetails());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Tahimik na laktawan - hindi kritikal
                    }
                });
    }

    private void setIfNotNull(EditText input, String value) {
        if (value != null) {
            input.setText(value);
        }
    }

    private void handleSavePaymentSettings() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Kailangan naka-login", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("storeName", storeNameInput.getText().toString().trim());
        settings.put("gcashNumber", gcashNumberInput.getText().toString().trim());
        settings.put("gcashName", gcashNameInput.getText().toString().trim());
        settings.put("mayaNumber", mayaNumberInput.getText().toString().trim());
        settings.put("maribankNumber", maribankNumberInput.getText().toString().trim());
        settings.put("cardDetails", cardDetailsInput.getText().toString().trim());

        FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("paymentSettings")
                .setValue(settings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Na-save ang payment settings", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(
                                context,
                                "Nabigo ang pag-save: " + (task.getException() != null
                                        ? task.getException().getMessage() : "unknown error"),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // -------------------------
    // MIGRATE LEGACY PRODUCTS (one-time utility)
    // -------------------------

    private View buildMigrateButton() {

        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(16), 0, dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(210, 225, 250));
        button.setBackground(bg);
        button.setElevation(dp(1));

        TextView text = new TextView(context);
        text.setText("Migrate Legacy Products");
        text.setTextSize(14);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(BLUE);
        button.addView(text);

        button.setOnClickListener(v -> handleMigrateLegacyProducts());

        return button;
    }

    // -------------------------
    // UTANG (customer credit)
    // -------------------------

    private View buildUtangButton() {

        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(230, 234, 240));
        button.setBackground(bg);
        button.setElevation(dp(1));

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(Color.rgb(255, 235, 230));
        iconWrap.setBackground(circleBg);

        IconViews.IconView icon = new IconViews.IconView(context, IconViews.TYPE_WALLET, RED);
        int iconSize = dp(18);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        int wrapSize = dp(36);
        button.addView(iconWrap, new LinearLayout.LayoutParams(wrapSize, wrapSize));

        TextView text = new TextView(context);
        text.setText("Utang (Customer Credits)");
        text.setTextSize(14);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(NAVY);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textParams.leftMargin = dp(12);
        button.addView(text, textParams);

        IconViews.IconView chevron = new IconViews.IconView(context, IconViews.TYPE_CHEVRON, GRAY_TEXT);
        button.addView(chevron, new LinearLayout.LayoutParams(dp(16), dp(16)));

        button.setOnClickListener(v -> context.startActivity(
                new Intent(context, UtangActivity.class)
        ));

        return button;
    }

    /**
     * Isang beses na tatakbo: kokopyahin yung mga lumang flat na
     * products (direktang nasa ilalim ng default_inventory, walang
     * kaugnay na UID) papunta sa default_inventory/<UID mo>/inventory,
     * tapos tatanggalin sa lumang lokasyon. Ligtas itong i-tap
     * ulit - kung wala nang matitirang legacy items, wala itong
     * gagawin.
     */
    private void handleMigrateLegacyProducts() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(context, "Kailangan naka-login", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = user.getUid();

        DatabaseReference rootRef = FirebaseDatabase.getInstance()
                .getReference("default_inventory");

        Toast.makeText(context, "Sinusuri ang lumang data...", Toast.LENGTH_SHORT).show();

        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {

                Map<String, Object> updates = new HashMap<>();
                int count = 0;

                for (DataSnapshot child : snapshot.getChildren()) {

                    String key = child.getKey();
                    if (key == null) {
                        continue;
                    }

                    // Legacy product = barcode key na puro numero lang.
                    // Ang UID nodes ay may letra, kaya laktawan natin sila.
                    if (!isAllDigits(key)) {
                        continue;
                    }

                    Object value = child.getValue();
                    if (value == null) {
                        continue;
                    }

                    updates.put(myUid + "/inventory/" + key, value);
                    updates.put(key, null);
                    count++;
                }

                if (count == 0) {
                    Toast.makeText(
                            context,
                            "Walang legacy products na makikita - up to date na.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                int finalCount = count;

                rootRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                context,
                                finalCount + " na produkto ang na-migrate papunta sa account mo.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                context,
                                "Nabigo ang migration: " + (task.getException() != null
                                        ? task.getException().getMessage() : "unknown error"),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(
                        context,
                        "Hindi mabasa ang lumang data: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private boolean isAllDigits(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // -------------------------
    // LOGOUT BUTTON
    // -------------------------

    private View buildLogoutButton() {

        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(16), 0, dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(250, 210, 210));
        button.setBackground(bg);
        button.setElevation(dp(1));

        TextView text = new TextView(context);
        text.setText("Log Out");
        text.setTextSize(15);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(RED);
        button.addView(text);

        button.setOnClickListener(v -> handleLogout());

        return button;
    }

    private void handleLogout() {

        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context, gso);
        googleSignInClient.signOut();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        context.startActivity(intent);
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
