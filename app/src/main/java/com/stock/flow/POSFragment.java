package com.stock.flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class POSFragment extends Fragment {

    private POSHomeView posHomeView;
    private DatabaseReference inventoryRef;
    private ValueEventListener inventoryListener;
    private DatabaseReference paymentSettingsRef;
    private ValueEventListener paymentSettingsListener;
    private ActivityResultLauncher<Intent> scannerLauncher;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onScannerResult
        );

        posHomeView = new POSHomeView(requireContext());

        posHomeView.setOnScannerClickListener(() -> {
            Intent intent = new Intent(requireContext(), ScannerActivity.class);
            scannerLauncher.launch(intent);
        });

        View view = posHomeView.build();

        startListeningToInventory();
        startListeningToPaymentSettings();

        return view;
    }

    private void onScannerResult(androidx.activity.result.ActivityResult result) {

        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
            return;
        }

        List<String> barcodes = result.getData().getStringArrayListExtra(ScannerActivity.EXTRA_BARCODES);
        List<Integer> quantities = result.getData().getIntegerArrayListExtra(ScannerActivity.EXTRA_QUANTITIES);

        if (posHomeView != null) {
            posHomeView.addScannedItems(barcodes, quantities);
        }
    }

    private void startListeningToInventory() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
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

                if (posHomeView != null) {
                    posHomeView.setProducts(products);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Unable to load products: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        };

        inventoryRef.addValueEventListener(inventoryListener);
    }

    private void startListeningToPaymentSettings() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        paymentSettingsRef = FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid())
                .child("paymentSettings");

        paymentSettingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PaymentSettings settings = snapshot.getValue(PaymentSettings.class);
                if (posHomeView != null) {
                    posHomeView.setPaymentSettings(settings);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Tahimik na laktawan - hindi kritikal
            }
        };

        paymentSettingsRef.addValueEventListener(paymentSettingsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (inventoryRef != null && inventoryListener != null) {
            inventoryRef.removeEventListener(inventoryListener);
        }
        if (paymentSettingsRef != null && paymentSettingsListener != null) {
            paymentSettingsRef.removeEventListener(paymentSettingsListener);
        }
        posHomeView = null;
    }
}
