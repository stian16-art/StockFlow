package com.stock.flow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

public class SalesFragment extends Fragment {

    private SalesHomeView salesHomeView;
    private DatabaseReference salesRef;
    private ValueEventListener salesListener;
    private DatabaseReference paymentSettingsRef;
    private ValueEventListener paymentSettingsListener;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        salesHomeView = new SalesHomeView(requireContext());
        View view = salesHomeView.build();

        startListeningToSales();
        startListeningToPaymentSettings();

        return view;
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
                if (salesHomeView != null) {
                    salesHomeView.setPaymentSettings(settings);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Tahimik na laktawan - hindi kritikal
            }
        };

        paymentSettingsRef.addValueEventListener(paymentSettingsListener);
    }

    private void startListeningToSales() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
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

                if (salesHomeView != null) {
                    salesHomeView.setSales(sales);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Unable to load sales: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        };

        salesRef.addValueEventListener(salesListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (salesRef != null && salesListener != null) {
            salesRef.removeEventListener(salesListener);
        }
        if (paymentSettingsRef != null && paymentSettingsListener != null) {
            paymentSettingsRef.removeEventListener(paymentSettingsListener);
        }
        salesHomeView = null;
    }
}
