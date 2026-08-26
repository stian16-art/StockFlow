package com.stock.flow;

import android.net.Uri;
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

public class InventoryFragment extends Fragment {

    private InventoryHomeView inventoryHomeView;
    private DatabaseReference inventoryRef;
    private ValueEventListener inventoryListener;

    private Product productBeingEdited;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImagePicked
        );

        inventoryHomeView = new InventoryHomeView(requireContext());

        inventoryHomeView.setOnProductImageClickListener(product -> {
            productBeingEdited = product;
            imagePickerLauncher.launch("image/*");
        });

        View view = inventoryHomeView.build();

        startListeningToInventory();

        return view;
    }

    private void onImagePicked(Uri uri) {

        if (uri == null || productBeingEdited == null) {
            return;
        }

        Product product = productBeingEdited;

        Toast.makeText(getContext(), "Ina-upload ang larawan...", Toast.LENGTH_SHORT).show();

        CloudinaryUploader.upload(requireContext(), uri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String secureUrl) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null || getContext() == null) {
                    return;
                }

                FirebaseDatabase.getInstance()
                        .getReference("default_inventory")
                        .child(user.getUid())
                        .child("inventory")
                        .child(product.getBarcode())
                        .child("imagePath")
                        .setValue(secureUrl)
                        .addOnCompleteListener(task -> {
                            if (getContext() == null) {
                                return;
                            }
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Na-update ang larawan", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Hindi na-save ang larawan", Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Upload error: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
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

                if (inventoryHomeView != null) {
                    inventoryHomeView.setProducts(products);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Unable to load inventory: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        };

        inventoryRef.addValueEventListener(inventoryListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (inventoryRef != null && inventoryListener != null) {
            inventoryRef.removeEventListener(inventoryListener);
        }
        inventoryHomeView = null;
    }
}
