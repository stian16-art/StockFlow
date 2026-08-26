package com.stock.flow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Barcode scanner screen - 40% camera preview sa taas, 60%
 * listahan ng na-scan na items sa ibaba. Nagbabalik ng resulta
 * (barcode + quantity bawat isa) papunta sa POS via setResult().
 */
public class ScannerActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODES = "extra_barcodes";
    public static final String EXTRA_QUANTITIES = "extra_quantities";

    private final int NAVY = Color.rgb(27, 42, 74);
    private final int BLUE = Color.rgb(45, 108, 223);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int GRAY_TEXT = Color.rgb(130, 138, 150);
    private final int PAGE_BG = Color.rgb(238, 243, 250);

    private float density;

    private PreviewView previewView;
    private LinearLayout scannedListContainer;
    private TextView countLabel;

    private final Map<String, Integer> scannedCounts = new LinkedHashMap<>();
    private final Map<String, Long> lastScanTime = new LinkedHashMap<>();
    private static final long SCAN_COOLDOWN_MS = 1500;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private ActivityResultLauncher<String> permissionLauncher;

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        density = getResources().getDisplayMetrics().density;
        cameraExecutor = Executors.newSingleThreadExecutor();
        barcodeScanner = BarcodeScanning.getClient();

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startCamera();
                    } else {
                        Toast.makeText(this, "Kailangan ng camera permission para mag-scan", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        );

        setContentView(buildContent());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // -------------------------
    // UI (40% camera / 60% list)
    // -------------------------

    private View buildContent() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE_BG);

        // TOP 40% - camera preview
        FrameLayout cameraWrap = new FrameLayout(this);
        LinearLayout.LayoutParams cameraParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.4f
        );
        cameraWrap.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        cameraWrap.addView(previewView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Close button sa taas ng camera
        TextView closeBtn = new TextView(this);
        closeBtn.setText("\u2715");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(Color.parseColor("#66000000"));
        closeBtn.setBackground(closeBg);
        closeBtn.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.topMargin = dp(16);
        closeParams.rightMargin = dp(16);
        cameraWrap.addView(closeBtn, closeParams);

        // Guide frame sa gitna ng camera preview
        View guideBox = new View(this);
        GradientDrawable guideBg = new GradientDrawable();
        guideBg.setStroke(dp(2), Color.WHITE);
        guideBg.setCornerRadius(dp(12));
        guideBox.setBackground(guideBg);
        FrameLayout.LayoutParams guideParams = new FrameLayout.LayoutParams(
                dp(220), dp(120)
        );
        guideParams.gravity = Gravity.CENTER;
        cameraWrap.addView(guideBox, guideParams);

        root.addView(cameraWrap, cameraParams);

        // BOTTOM 60% - scanned list
        LinearLayout bottomSection = new LinearLayout(this);
        bottomSection.setOrientation(LinearLayout.VERTICAL);
        bottomSection.setPadding(dp(20), dp(16), dp(20), dp(16));
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.6f
        );

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Scanned Items");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(NAVY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        headerRow.addView(title, titleParams);

        countLabel = new TextView(this);
        countLabel.setText("0 items");
        countLabel.setTextSize(13);
        countLabel.setTextColor(GRAY_TEXT);
        headerRow.addView(countLabel);

        bottomSection.addView(headerRow);
        bottomSection.addView(spacer(4));

        TextView hint = new TextView(this);
        hint.setText("Ituon ang camera sa barcode - awtomatikong madadagdag dito.");
        hint.setTextSize(11.5f);
        hint.setTextColor(GRAY_TEXT);
        bottomSection.addView(hint);
        bottomSection.addView(spacer(12));

        ScrollView scrollView = new ScrollView(this);
        scannedListContainer = new LinearLayout(this);
        scannedListContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(scannedListContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        );
        bottomSection.addView(scrollView, scrollParams);

        bottomSection.addView(spacer(12));

        LinearLayout doneBtn = new LinearLayout(this);
        doneBtn.setGravity(Gravity.CENTER);
        doneBtn.setPadding(0, dp(16), 0, dp(16));
        GradientDrawable doneBg = new GradientDrawable();
        doneBg.setColor(GREEN);
        doneBg.setCornerRadius(dp(16));
        doneBtn.setBackground(doneBg);

        TextView doneText = new TextView(this);
        doneText.setText("Done - Add to Cart");
        doneText.setTextSize(15);
        doneText.setTypeface(null, Typeface.BOLD);
        doneText.setTextColor(Color.WHITE);
        doneBtn.addView(doneText);

        doneBtn.setOnClickListener(v -> finishWithResult());
        bottomSection.addView(doneBtn);

        root.addView(bottomSection, bottomParams);

        return root;
    }

    private void refreshScannedList() {

        scannedListContainer.removeAllViews();

        int totalQty = 0;

        for (Map.Entry<String, Integer> entry : scannedCounts.entrySet()) {
            totalQty += entry.getValue();
            scannedListContainer.addView(scannedRow(entry.getKey(), entry.getValue()));
        }

        countLabel.setText(totalQty + " items");
    }

    private View scannedRow(String barcode, int qty) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        row.setBackground(bg);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);

        IconViews.IconView icon = new IconViews.IconView(this, IconViews.TYPE_BARCODE, BLUE);
        row.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView barcodeText = new TextView(this);
        barcodeText.setText(barcode);
        barcodeText.setTextSize(13);
        barcodeText.setTypeface(null, Typeface.BOLD);
        barcodeText.setTextColor(NAVY);
        LinearLayout.LayoutParams barcodeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        barcodeParams.leftMargin = dp(10);
        row.addView(barcodeText, barcodeParams);

        TextView qtyText = new TextView(this);
        qtyText.setText("x" + qty);
        qtyText.setTextSize(14);
        qtyText.setTypeface(null, Typeface.BOLD);
        qtyText.setTextColor(BLUE);
        row.addView(qtyText);

        return row;
    }

    private void finishWithResult() {

        if (scannedCounts.isEmpty()) {
            Toast.makeText(this, "Wala pang na-scan", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> barcodes = new ArrayList<>(scannedCounts.keySet());
        ArrayList<Integer> quantities = new ArrayList<>();
        for (String b : barcodes) {
            quantities.add(scannedCounts.get(b));
        }

        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_BARCODES, barcodes);
        result.putIntegerArrayListExtra(EXTRA_QUANTITIES, quantities);
        setResult(RESULT_OK, result);
        finish();
    }

    // -------------------------
    // CAMERA / ML KIT
    // -------------------------

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis
                );

            } catch (Exception e) {
                Toast.makeText(this, "Hindi ma-start ang camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees()
        );

        Task<java.util.List<Barcode>> task = barcodeScanner.process(image);

        task.addOnSuccessListener(barcodes -> {
            for (Barcode barcode : barcodes) {
                String value = barcode.getRawValue();
                if (value != null && !value.isEmpty()) {
                    runOnUiThread(() -> onBarcodeScanned(value));
                }
            }
        }).addOnCompleteListener(t -> imageProxy.close());
    }

    private void onBarcodeScanned(String barcode) {

        long now = System.currentTimeMillis();
        Long last = lastScanTime.get(barcode);

        if (last != null && (now - last) < SCAN_COOLDOWN_MS) {
            return;
        }

        lastScanTime.put(barcode, now);
        int currentQty = scannedCounts.getOrDefault(barcode, 0);
        scannedCounts.put(barcode, currentQty + 1);

        refreshScannedList();
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
        ));
        return v;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}
