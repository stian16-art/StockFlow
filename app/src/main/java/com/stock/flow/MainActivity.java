package com.stock.flow;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FrameLayout root;
    private FrameLayout homeView;
    private FrameLayout fragmentContainer;

    private DashboardHomeView dashboardHomeView;

    private DatabaseReference dashboardInventoryRef;
    private ValueEventListener dashboardInventoryListener;
    private DatabaseReference dashboardSalesRef;
    private ValueEventListener dashboardSalesListener;

    private List<Product> dashboardProducts = new ArrayList<>();
    private List<SaleRecord> dashboardSales = new ArrayList<>();

    private static final int CONTAINER_ID = 0x1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Safety net - kung walang session (hal. direktang binuksan
        // itong Activity, o na-clear ang app data), balik sa Login.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(245, 245, 245));

        createHomeContent();
        createFragmentContainer();
        createBottomNavigation();

        setContentView(root);

        startListeningForDashboardData();
    }

    /**
     * Ito na mismo ang Dashboard/Home - hindi na
     * hiwalay na Fragment, laman na ng MainActivity.
     */
    private void createHomeContent() {

        homeView = new FrameLayout(this);

        dashboardHomeView = new DashboardHomeView(this);
        View dashboardView = dashboardHomeView.build();

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

        homeView.addView(dashboardView, params);

        FrameLayout.LayoutParams rootParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

        root.addView(homeView, rootParams);
    }

    /**
     * Kinukuha ang parehong inventory (para sa product lookup/category/
     * imahe) at sales (para sa totoong stats/charts) at pinapasa
     * pareho papunta sa DashboardHomeView tuwing may pagbabago.
     */
    private void startListeningForDashboardData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("default_inventory")
                .child(user.getUid());

        dashboardInventoryRef = userRef.child("inventory");
        dashboardInventoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {

                List<Product> products = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product != null) {
                        product.setKey(child.getKey());
                        products.add(product);
                    }
                }

                dashboardProducts = products;
                dashboardHomeView.setData(dashboardProducts, dashboardSales);
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                // Tahimik na laktawan - hindi kritikal kung minsang
                // hindi ma-refresh ang dashboard.
            }
        };
        dashboardInventoryRef.addValueEventListener(dashboardInventoryListener);

        dashboardSalesRef = userRef.child("sales");
        dashboardSalesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {

                List<SaleRecord> sales = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SaleRecord sale = child.getValue(SaleRecord.class);
                    if (sale != null) {
                        sales.add(sale);
                    }
                }

                dashboardSales = sales;
                dashboardHomeView.setData(dashboardProducts, dashboardSales);
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                // Tahimik na laktawan
            }
        };
        dashboardSalesRef.addValueEventListener(dashboardSalesListener);
    }

    /**
     * Naka-overlay ito sa ibabaw ng homeView. Dito
     * ipapakita ang Inventory / POS / Sales / Utang
     * fragments. GONE muna habang nasa Home.
     */
    private void createFragmentContainer() {

        fragmentContainer = new FrameLayout(this);
        fragmentContainer.setId(CONTAINER_ID);
        fragmentContainer.setVisibility(View.GONE);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

        root.addView(fragmentContainer, params);
    }

    private void createBottomNavigation() {

        StockFlowBottomNav bottomNav =
                new StockFlowBottomNav(this);

        bottomNav.setOnNavigationSelectedListener(
                new StockFlowBottomNav.OnNavigationSelectedListener() {

                    @Override
                    public void onNavigationSelected(int position) {

                        switch (position) {

                            case 0:
                                showHome();
                                break;

                            case 1:
                                showFragment(new InventoryFragment());
                                break;

                            case 2:
                                showFragment(new POSFragment());
                                break;

                            case 3:
                                showFragment(new SalesFragment());
                                break;

                            case 4:
                                showFragment(new ProfileFragment());
                                break;
                        }
                    }
                }
        );

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(110)
                );

        params.gravity = Gravity.BOTTOM;

        root.addView(bottomNav, params);
    }

    private void showHome() {

        fragmentContainer.setVisibility(View.GONE);
        homeView.setVisibility(View.VISIBLE);
    }

    private void showFragment(Fragment fragment) {

        homeView.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        transaction.replace(CONTAINER_ID, fragment);
        transaction.commit();
    }

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dashboardInventoryRef != null && dashboardInventoryListener != null) {
            dashboardInventoryRef.removeEventListener(dashboardInventoryListener);
        }
        if (dashboardSalesRef != null && dashboardSalesListener != null) {
            dashboardSalesRef.removeEventListener(dashboardSalesListener);
        }
    }
}
