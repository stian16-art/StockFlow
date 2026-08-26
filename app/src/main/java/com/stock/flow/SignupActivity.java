package com.stock.flow;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Sign Up screen - Name/Email/Password + Google Sign-Up, gamit
 * ang parehong navy/blue palette ng buong app.
 */
public class SignupActivity extends AppCompatActivity {

    private final int PAGE_BG = Color.rgb(238, 243, 250);
    private final int CARD_BG = Color.WHITE;
    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int BLUE_DARK = Color.rgb(30, 80, 200);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int LIGHT_BORDER = Color.rgb(225, 231, 240);

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private ProgressBar progressBar;
    private LinearLayout signUpButton;
    private TextView signUpButtonText;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private float density;

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        density = getResources().getDisplayMetrics().density;

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task =
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                }
        );

        setContentView(buildContent());
    }

    // -------------------------
    // UI
    // -------------------------

    private View buildContent() {

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(PAGE_BG);
        scrollView.setFillViewport(true);

        FrameLayout centerWrap = new FrameLayout(this);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(28), dp(48), dp(28), dp(40));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_stockflow_logo);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                dp(180), dp(54)
        );
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        column.addView(logo, logoParams);
        column.addView(spacer(24));

        TextView title = new TextView(this);
        title.setText("Create Account");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        title.setGravity(Gravity.CENTER);
        column.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Sign up to start managing your store");
        subtitle.setTextSize(13);
        subtitle.setTextColor(GRAY_TEXT);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dp(6);
        column.addView(subtitle, subParams);
        column.addView(spacer(28));

        nameInput = fieldInput("Full Name", IconViews.TYPE_PERSON, false, false);
        column.addView(inputWrap(nameInput));
        column.addView(spacer(14));

        emailInput = fieldInput("Email", IconViews.TYPE_EMAIL, false, true);
        column.addView(inputWrap(emailInput));
        column.addView(spacer(14));

        passwordInput = fieldInput("Password", IconViews.TYPE_LOCK, true, false);
        column.addView(inputWrap(passwordInput, true));
        column.addView(spacer(14));

        confirmPasswordInput = fieldInput("Confirm Password", IconViews.TYPE_LOCK, true, false);
        column.addView(inputWrap(confirmPasswordInput, true));
        column.addView(spacer(24));

        signUpButton = primaryButton("Sign Up");
        signUpButtonText = (TextView) signUpButton.getChildAt(0);
        signUpButton.setOnClickListener(v -> handleEmailSignUp());
        column.addView(signUpButton);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(12);
        progressBar.setVisibility(View.GONE);
        column.addView(progressBar, progressParams);

        column.addView(spacer(24));
        column.addView(orDivider());
        column.addView(spacer(24));

        LinearLayout googleButton = googleButton("Sign up with Google");
        googleButton.setOnClickListener(v -> startGoogleSignIn());
        column.addView(googleButton);
        column.addView(spacer(28));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);

        TextView bottomText = new TextView(this);
        bottomText.setText("Already have an account? ");
        bottomText.setTextSize(13);
        bottomText.setTextColor(GRAY_TEXT);
        bottomRow.addView(bottomText);

        TextView loginLink = new TextView(this);
        loginLink.setText("Login");
        loginLink.setTextSize(13);
        loginLink.setTypeface(null, Typeface.BOLD);
        loginLink.setTextColor(BLUE);
        loginLink.setOnClickListener(v -> finish());
        bottomRow.addView(loginLink);

        column.addView(bottomRow);

        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        centerWrap.addView(column, columnParams);
        scrollView.addView(centerWrap);

        return scrollView;
    }

    // -------------------------
    // SHARED FIELD HELPERS
    // -------------------------

    private EditText fieldInput(String hint, int iconType, boolean isPassword, boolean isEmail) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(14);
        input.setTextColor(NAVY);
        input.setHintTextColor(GRAY_TEXT);
        input.setBackground(null);
        input.setSingleLine(true);
        input.setPadding(dp(10), dp(14), dp(10), dp(14));

        if (isPassword) {
            input.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (isEmail) {
            input.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        }

        input.setTag(iconType);
        return input;
    }

    private View inputWrap(EditText input) {
        return inputWrap(input, false);
    }

    private View inputWrap(EditText input, boolean isPasswordField) {

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setGravity(Gravity.CENTER_VERTICAL);
        wrap.setPadding(dp(14), dp(2), dp(14), dp(2));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), LIGHT_BORDER);
        wrap.setBackground(bg);

        int iconType = (int) input.getTag();
        IconViews.IconView icon = new IconViews.IconView(this, iconType, GRAY_TEXT);
        wrap.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        inputParams.leftMargin = dp(10);
        wrap.addView(input, inputParams);

        if (isPasswordField) {
            IconViews.IconView eyeIcon = new IconViews.IconView(
                    this, IconViews.TYPE_EYE_OFF, GRAY_TEXT
            );
            LinearLayout.LayoutParams eyeParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            eyeParams.leftMargin = dp(8);
            wrap.addView(eyeIcon, eyeParams);

            eyeIcon.setOnClickListener(v -> {
                boolean isVisible = input.getInputType()
                        == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                if (isVisible) {
                    input.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyeIcon.setType(IconViews.TYPE_EYE_OFF);
                } else {
                    input.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyeIcon.setType(IconViews.TYPE_EYE);
                }

                input.setSelection(input.getText().length());
            });
        }

        return wrap;
    }

    private LinearLayout primaryButton(String label) {

        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(16), 0, dp(16));

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BLUE, BLUE_DARK}
        );
        bg.setCornerRadius(dp(16));
        button.setBackground(bg);
        button.setElevation(dp(3));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(15);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(Color.WHITE);
        button.addView(text);

        return button;
    }

    private View orDivider() {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        View lineLeft = new View(this);
        lineLeft.setBackgroundColor(LIGHT_BORDER);
        row.addView(lineLeft, new LinearLayout.LayoutParams(0, dp(1), 1f));

        TextView orText = new TextView(this);
        orText.setText("  or continue with  ");
        orText.setTextSize(12);
        orText.setTextColor(GRAY_TEXT);
        row.addView(orText);

        View lineRight = new View(this);
        lineRight.setBackgroundColor(LIGHT_BORDER);
        row.addView(lineRight, new LinearLayout.LayoutParams(0, dp(1), 1f));

        return row;
    }

    private LinearLayout googleButton(String label) {

        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(14), 0, dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), LIGHT_BORDER);
        button.setBackground(bg);
        button.setElevation(dp(1));

        ImageView googleLogo = new ImageView(this);
        googleLogo.setImageResource(R.drawable.ic_google_logo);
        button.addView(googleLogo, new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(14);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(NAVY);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.leftMargin = dp(10);
        button.addView(text, textParams);

        return button;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }

    // -------------------------
    // AUTH LOGIC
    // -------------------------

    private void handleEmailSignUp() {

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (name.isEmpty()) {
            nameInput.setError("Enter your name");
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("At least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords don't match");
            return;
        }

        setLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        setLoading(false);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign up failed";
                        Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdate =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                        user.updateProfile(profileUpdate)
                                .addOnCompleteListener(profileTask -> {
                                    setLoading(false);
                                    goToMain();
                                });
                    } else {
                        setLoading(false);
                        goToMain();
                    }
                });
    }

    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(
                    account.getIdToken(), null
            );

            setLoading(true);

            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, authTask -> {
                        setLoading(false);
                        if (authTask.isSuccessful()) {
                            goToMain();
                        } else {
                            Toast.makeText(
                                    SignupActivity.this,
                                    "Google sign-up failed",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

        } catch (ApiException e) {
            Toast.makeText(this, "Google sign-up cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        signUpButton.setEnabled(!loading);
        signUpButtonText.setText(loading ? "Please wait..." : "Sign Up");
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
