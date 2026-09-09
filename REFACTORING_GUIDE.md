# StockFlow Refactoring Guide

This guide provides step-by-step instructions for reorganizing the codebase to follow the architecture defined in `ARCHITECTURE.md`.

## Current State vs. Target State

### Current Structure
```
com/stock/flow/
├── LoginActivity.java
├── SignupActivity.java
├── MainActivity.java
├── DashboardHomeView.java
├── InventoryHomeView.java
├── ... (30 classes all in root package)
```

### Target Structure
```
com/stock/flow/
├── ui/
│   ├── activities/
│   │   ├── LoginActivity.java
│   │   ├── SignupActivity.java
│   │   ├── MainActivity.java
│   │   └── ScannerActivity.java
│   ├── fragments/
│   │   ├── InventoryFragment.java
│   │   ├── POSFragment.java
│   │   ├── SalesFragment.java
│   │   ├── ProfileFragment.java
│   │   └── UtangFragment.java
│   └── views/
│       ├── DashboardHomeView.java
│       ├── InventoryHomeView.java
│       ├── POSHomeView.java
│       ├── SalesHomeView.java
│       ├── ProfileHomeView.java
│       ├── StockFlowBottomNav.java
│       └── IconViews.java
├── data/
│   ├── models/
│   │   ├── Product.java
│   │   ├── CartItem.java
│   │   ├── SaleItem.java
│   │   ├── SaleRecord.java
│   │   └── PaymentSettings.java
│   └── firebase/
│       ├── ProductRepository.java (NEW)
│       ├── SalesRepository.java (NEW)
│       ├── AuthRepository.java (NEW)
│       └── FirebaseConfig.java (NEW)
├── utils/
│   └── CloudinaryUploader.java
└── theme/
    ├── ThemeColors.java (NEW)
    └── ThemeDimens.java (NEW)
```

## Refactoring Steps

### Phase 1: Create Package Structure

Use Android Studio's package creation tools:

1. **Create `ui` package**
   - Right-click `com.stock.flow` → New → Package
   - Name: `ui`

2. **Create sub-packages inside `ui`**
   - `com.stock.flow.ui.activities`
   - `com.stock.flow.ui.fragments`
   - `com.stock.flow.ui.views`

3. **Create `data` package**
   - `com.stock.flow.data.models`
   - `com.stock.flow.data.firebase`

4. **Create `theme` package**
   - `com.stock.flow.theme`

### Phase 2: Move Activities to `ui/activities/`

Move and update package declarations:

**LoginActivity.java**
```java
// OLD
package com.stock.flow;

// NEW
package com.stock.flow.ui.activities;

import com.stock.flow.R;
import com.stock.flow.theme.ThemeColors;
import com.stock.flow.ui.views.IconViews;
```

**Steps in Android Studio:**
1. Select `LoginActivity.java`
2. Right-click → Refactor → Move
3. Select `com.stock.flow.ui.activities` as target package
4. Android Studio auto-updates imports

**Files to move:**
- LoginActivity.java
- SignupActivity.java
- MainActivity.java
- ScannerActivity.java
- PlayVsAiActivity.java

### Phase 3: Move Fragments to `ui/fragments/`

**Steps:**
1. Select each fragment
2. Right-click → Refactor → Move to `com.stock.flow.ui.fragments`
3. Verify imports update

**Files to move:**
- InventoryFragment.java
- POSFragment.java
- SalesFragment.java
- ProfileFragment.java
- UtangFragment.java

**Update MainActivity.java imports:**
```java
// OLD
showFragment(new InventoryFragment());

// NEW (same, but imports now resolve to ui.fragments package)
import com.stock.flow.ui.fragments.InventoryFragment;
```

### Phase 4: Move View Builders to `ui/views/`

**Files to move:**
- DashboardHomeView.java
- InventoryHomeView.java
- POSHomeView.java
- SalesHomeView.java
- ProfileHomeView.java
- StockFlowBottomNav.java
- IconViews.java

**Example update for DashboardHomeView:**
```java
// OLD
package com.stock.flow;

// NEW
package com.stock.flow.ui.views;

import com.stock.flow.theme.ThemeColors;
import com.stock.flow.data.models.Product;
import com.stock.flow.data.models.SaleRecord;
```

### Phase 5: Move Models to `data/models/`

**Files to move:**
- Product.java
- CartItem.java
- SaleItem.java
- SaleRecord.java
- PaymentSettings.java

**Example update:**
```java
// OLD
package com.stock.flow;

// NEW
package com.stock.flow.data.models;
```

### Phase 6: Move & Refactor Utilities to `utils/`

**Files to move:**
- CloudinaryUploader.java → `com.stock.flow.utils`

**PlayVsAiActivity.java** - Consider removal:
```bash
# Check if it's referenced anywhere
# If not, delete it (unused game feature)
grep -r "PlayVsAiActivity" .
```

### Phase 7: Create Firebase Repository Classes

Create new files in `com.stock.flow.data.firebase/`:

**ProductRepository.java**
```java
package com.stock.flow.data.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stock.flow.data.models.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository {
    
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    public interface ProductCallback {
        void onSuccess(List<Product> products);
        void onError(String error);
    }
    
    public void getProducts(String userId, ProductCallback callback) {
        DatabaseReference ref = database.getReference("default_inventory")
                .child(userId)
                .child("inventory");
        
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Product> products = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product != null) {
                        product.setKey(child.getKey());
                        products.add(product);
                    }
                }
                callback.onSuccess(products);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }
}
```

**SalesRepository.java**
```java
package com.stock.flow.data.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stock.flow.data.models.SaleRecord;

public class SalesRepository {
    
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    public interface SalesCallback {
        void onSuccess(java.util.List<SaleRecord> sales);
        void onError(String error);
    }
    
    public void addSale(String userId, SaleRecord sale, SalesCallback callback) {
        DatabaseReference ref = database.getReference("default_inventory")
                .child(userId)
                .child("sales")
                .push();
        
        ref.setValue(sale).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Success - fetch updated list
            } else {
                callback.onError(task.getException().getMessage());
            }
        });
    }
}
```

**FirebaseConfig.java**
```java
package com.stock.flow.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseConfig {
    
    private static FirebaseAuth auth;
    private static FirebaseDatabase database;
    
    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }
    
    public static FirebaseDatabase getDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
        }
        return database;
    }
}
```

### Phase 8: Update Color Usage in Existing Files

Replace hardcoded colors with `ThemeColors` constants:

**LoginActivity.java** (excerpt)
```java
// OLD
private final int NAVY = Color.rgb(27, 42, 74);
private final int BLUE = Color.rgb(45, 108, 223);

// NEW
import com.stock.flow.theme.ThemeColors;

// Use ThemeColors directly (remove color fields)
title.setTextColor(ThemeColors.TEXT_PRIMARY);
button.setBackgroundColor(ThemeColors.PRIMARY_BLUE);
```

**Files to update:**
- LoginActivity.java
- SignupActivity.java
- DashboardHomeView.java
- InventoryHomeView.java
- POSHomeView.java
- SalesHomeView.java
- ProfileHomeView.java
- StockFlowBottomNav.java

### Phase 9: Verify & Test

After moving all files:

1. **Check for import errors**
   - Build project: `./gradlew build`
   - Fix any unresolved imports in Android Studio

2. **Verify no circular dependencies**
   - `ui/` should not import from `data/`
   - `data/` should only import models
   - All layers can import from `theme/`

3. **Run the app**
   ```bash
   ./gradlew installDebug
   ```

4. **Test key flows:**
   - Login/Signup
   - Navigate through all bottom nav sections
   - Add product to cart
   - Scan barcode
   - View sales history

### Phase 10: Update AndroidManifest.xml

After moving activities, update manifest with new package paths:

```xml
<activity
    android:name=".ui.activities.LoginActivity"
    android:exported="true">
    ...
</activity>

<activity
    android:name=".ui.activities.MainActivity"
    android:exported="false" />

<activity
    android:name=".ui.activities.SignupActivity"
    android:exported="false" />

<activity
    android:name=".ui.activities.ScannerActivity"
    android:exported="false" />
```

## Common Issues & Solutions

### Issue: "Cannot resolve symbol" after moving files

**Solution:** 
1. Invalidate caches: File → Invalidate Caches
2. Rebuild project: Build → Clean Project → Rebuild Project

### Issue: Fragment not found in MainActivity

**Solution:** Verify FragmentManager can find the fragment class:
```java
// Correct full path
getSupportFragmentManager().beginTransaction()
    .replace(CONTAINER_ID, new com.stock.flow.ui.fragments.InventoryFragment())
    .commit();
```

### Issue: Circular dependency between packages

**Solution:** Ensure dependency hierarchy:
- `ui/` depends on `data/` and `theme/` ✓
- `data/` depends on nothing (only models) ✓
- `theme/` is standalone ✓
- No `data/` importing from `ui/` ✗

## Commit Strategy

Break refactoring into logical commits:

```bash
# Commit 1: Create package structure
git add app/src/main/java/com/stock/flow/{ui,data,theme}/
git commit -m "refactor: create package structure for layers"

# Commit 2: Move activities
git add -u
git commit -m "refactor: move activities to ui/activities package"

# Commit 3: Move fragments
git commit -m "refactor: move fragments to ui/fragments package"

# Commit 4: Move views
git commit -m "refactor: move view builders to ui/views package"

# Commit 5: Move models
git commit -m "refactor: move models to data/models package"

# Commit 6: Create repositories
git add app/src/main/java/com/stock/flow/data/firebase/
git commit -m "refactor: create Firebase repository layer"

# Commit 7: Update color usage
git commit -m "refactor: replace hardcoded colors with ThemeColors"

# Commit 8: Update manifest
git commit -m "refactor: update AndroidManifest with new package paths"
```

## Post-Refactoring Tasks

After completing the refactoring:

1. **Update imports in build files** - Verify no excluded packages
2. **Run lint checks** - Android Studio → Analyze → Run Inspection by Name → Unused imports
3. **Update CI/CD configuration** - If any build scripts reference old paths
4. **Create follow-up issues:**
   - Create BaseActivity/BaseFragment for code reuse
   - Implement ViewModel layer
   - Add unit tests for repositories
   - Implement LiveData for reactive updates

---

**Estimated Time:** 2-3 hours  
**Difficulty:** Medium  
**Risk Level:** Low (purely structural, no logic changes)
