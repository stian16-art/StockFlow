# StockFlow Architecture

## Overview

StockFlow follows a **layered architecture** with clear separation of concerns across three main layers:

```
┌─────────────────────────────────────────┐
│         PRESENTATION LAYER              │
│  (Activities, Fragments, ViewBuilders)  │
└──────────────┬──────────────────────────┘
               │ (depends on)
┌──────────────▼──────────────────────────┐
│         DATA/REPOSITORY LAYER           │
│  (Firebase access, data transformation) │
└──────────────┬──────────────────────────┘
               │ (depends on)
┌──────────────▼──────────────────────────┐
│         MODEL LAYER                     │
│  (Data classes: Product, SaleRecord)    │
└─────────────────────────────────────────┘
```

## Directory Structure

### `ui/activities/`
**Purpose:** Main app flow entry points  
**Responsibilities:** 
- Manage activity lifecycle
- Handle system events (permissions, back navigation)
- Delegate to view builders for UI creation
- Communicate with repository layer for data

**Files:**
- `LoginActivity.java` - Authentication screen
- `SignupActivity.java` - User registration
- `MainActivity.java` - Main app container
- `ScannerActivity.java` - Barcode scanning interface

### `ui/fragments/`
**Purpose:** Reusable screen components  
**Responsibilities:**
- Manage fragment lifecycle
- Display data from repositories
- Handle user interactions
- Update UI in response to data changes

**Files:**
- `InventoryFragment.java` - Inventory list display
- `POSFragment.java` - Point-of-sale entry point
- `SalesFragment.java` - Sales history display
- `ProfileFragment.java` - User profile
- `UtangFragment.java` - Customer debt tracking

### `ui/views/`
**Purpose:** Complex view builders  
**Responsibilities:**
- Construct UI hierarchies programmatically
- Apply styling and layout logic
- Handle view-specific event listeners
- Return complete View objects ready for display

**Files:**
- `DashboardHomeView.java` - Home screen dashboard
- `InventoryHomeView.java` - Inventory management UI
- `POSHomeView.java` - Point-of-sale UI
- `SalesHomeView.java` - Sales management UI
- `ProfileHomeView.java` - Profile UI
- `StockFlowBottomNav.java` - Bottom navigation bar
- `IconViews.java` - Custom icon rendering system

### `data/models/`
**Purpose:** Data transfer objects and entities  
**Responsibilities:**
- Define data structures
- Provide getters/setters for properties
- Implement serialization (Firebase compatibility)

**Files:**
- `Product.java` - Product entity
- `CartItem.java` - Shopping cart item
- `SaleItem.java` - Individual sale line item
- `SaleRecord.java` - Complete transaction record
- `PaymentSettings.java` - Payment configuration

### `data/firebase/`
**Purpose:** Data access and Firebase integration  
**Responsibilities:**
- Query Firebase Realtime Database
- Handle authentication flows
- Transform Firebase snapshots to model objects
- Manage listeners and real-time updates

**Files (to be refactored):**
- `ProductRepository.java` - Product CRUD operations
- `SalesRepository.java` - Sales transaction handling
- `AuthRepository.java` - User authentication
- `FirebaseConfig.java` - Shared Firebase configuration

### `utils/`
**Purpose:** Reusable utility classes  
**Responsibilities:**
- Provide helper functions
- Handle cross-cutting concerns
- Support common operations

**Files:**
- `CloudinaryUploader.java` - Image upload service
- `PlayVsAiActivity.java` - (Remove: unused game feature)

### `theme/`
**Purpose:** UI theming and styling  
**Responsibilities:**
- Centralize color definitions
- Provide reusable styling helpers
- Enforce design consistency

**Files:**
- `ThemeColors.java` - Color palette constants
- `ThemeDimens.java` - Spacing and sizing constants (to be created)
- `ThemeTypography.java` - Font styling (to be created)

## Data Flow

### User Login Flow
```
LoginActivity 
  → Firebase Auth.signInWithEmailAndPassword() 
  → Success → MainActivity (Dashboard)
  → Failure → Show error toast
```

### Dashboard Data Load
```
MainActivity.onCreate()
  → addValueEventListener(dashboardInventoryRef)
  → Firebase returns DataSnapshot
  → Transform to List<Product>
  → DashboardHomeView.setData()
  → Render charts and metrics
```

### Add to Cart (POS)
```
POSHomeView.addToCart()
  → Create CartItem
  → Add to local cartItems list
  → Update cart UI
  → On checkout: create SaleRecord
  → Firebase.push(saleRecord)
```

### Fetch Sales History
```
SalesFragment
  → ProductRepository.getSalesHistory()
  → Firebase query with date range filter
  → Transform DataSnapshot to List<SaleRecord>
  → Display in adapter/list view
```

## Firebase Database Schema

```json
{
  "default_inventory": {
    "{userId}": {
      "inventory": {
        "{productId}": {
          "name": "Laptop",
          "price": 50000,
          "stock": 15,
          "category": "Electronics",
          "imageUrl": "https://cloudinary.com/...",
          "key": "{productId}"
        }
      },
      "sales": {
        "{saleId}": {
          "timestamp": 1694721600000,
          "total": 125000,
          "paymentMethod": "CASH",
          "items": [
            {
              "productId": "prod123",
              "quantity": 2,
              "pricePerUnit": 50000,
              "subtotal": 100000
            }
          ]
        }
      },
      "customers": {
        "{customerId}": {
          "name": "John Doe",
          "phone": "09123456789",
          "balance": 25000
        }
      }
    }
  }
}
```

## Key Design Patterns

### 1. Repository Pattern
**Use Cases:** ProductRepository, SalesRepository  
**Benefits:** Centralizes data access, testable, decouples UI from Firebase

```java
// Example (to be implemented)
public class ProductRepository {
    public void getProducts(Callback<List<Product>> callback) {
        // Firebase query logic
    }
}
```

### 2. View Builder Pattern
**Use Cases:** DashboardHomeView, POSHomeView  
**Benefits:** Programmatic UI, reusable view components

```java
public class DashboardHomeView {
    private View build() {
        // Construct and return complete UI hierarchy
    }
}
```

### 3. Fragment for Screens
**Use Cases:** InventoryFragment, SalesFragment  
**Benefits:** Reusable, lifecycle-aware, supports back navigation

### 4. Listener Pattern
**Use Cases:** StockFlowBottomNav.OnNavigationSelectedListener  
**Benefits:** Loose coupling, event-driven communication

## Refactoring Roadmap

### Phase 1: Foundation (Current)
- [x] Add README with setup instructions
- [x] Create ThemeColors utility
- [x] Document architecture
- [ ] Reorganize packages
- [ ] Extract common BaseActivity/BaseFragment

### Phase 2: Data Layer
- [ ] Create ProductRepository
- [ ] Create SalesRepository
- [ ] Create AuthRepository
- [ ] Centralize Firebase configuration

### Phase 3: Presentation Layer
- [ ] Convert Activities to use repositories
- [ ] Implement ViewModel for state management
- [ ] Add LiveData for reactive updates

### Phase 4: Testing & Polish
- [ ] Add unit tests (repositories, models)
- [ ] Add instrumentation tests (UI flows)
- [ ] Implement error handling and logging
- [ ] Add analytics tracking

## Best Practices

### Colors
✅ Use ThemeColors constants:
```java
button.setBackgroundColor(ThemeColors.PRIMARY_BLUE);
```

❌ Avoid hardcoded RGB:
```java
button.setBackgroundColor(Color.rgb(45, 108, 223));
```

### Firebase Queries
✅ Centralize in repository:
```java
productRepository.getProducts(callback);
```

❌ Scatter across activities:
```java
FirebaseDatabase.getInstance().getReference(...);
```

### UI Construction
✅ Use view builders for complex layouts:
```java
View dashboard = new DashboardHomeView(context).build();
```

❌ Build UI directly in activities:
```java
// 300+ lines of view creation in onCreate()
```

### Fragment Navigation
✅ Use FragmentManager:
```java
getSupportFragmentManager().beginTransaction()
    .replace(containerId, fragment)
    .commit();
```

## Performance Considerations

1. **Database Listeners** - Remove listeners in onDestroy() to prevent memory leaks
2. **Image Loading** - Use Glide for efficient caching and lazy loading
3. **Real-time Updates** - Limit listeners to essential data (don't listen to all inventory)
4. **Barcode Scanning** - Release camera resources when not in use

## Security Guidelines

1. Never commit `google-services.json` with real Firebase keys
2. Use Firebase Security Rules to restrict database access
3. Validate all user inputs before Firebase operations
4. Implement rate limiting for sensitive operations
5. Use HTTPS for all external API calls (Cloudinary)

## Testing Strategy

### Unit Tests
- Model validation (Product, SaleRecord)
- Repository methods (mock Firebase)
- Utility functions (color transformations)

### Integration Tests
- Firebase read/write operations
- Authentication flows
- Complete data flow scenarios

### UI Tests
- Navigation between fragments
- Button interactions
- Form validation

## Monitoring & Debugging

Use Android Logcat with tags:
```java
Log.d("ProductRepo", "Fetching products for user: " + userId);
Log.e("FirebaseError", "Database error: " + error.getMessage());
```

Future: Integrate Timber for structured logging
```java
Timber.d("Fetching products for user: %s", userId);
```

---

**Last Updated:** September 2026  
**Architecture Version:** 1.0
