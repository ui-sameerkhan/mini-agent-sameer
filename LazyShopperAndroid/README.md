# Lazy Shopper — Android

Native Kotlin/Jetpack Compose Android client for the **Lazy Shopper** (VeggieKart) multi-vendor
grocery delivery marketplace. It talks directly to the existing FastAPI + MongoDB backend at
`https://www.lazyshopper.in/` — the backend was **not** rewritten; this app is a native mobile
client for it, covering all four roles the web app supports: **Customer, Shopkeeper, Delivery
Partner, and Admin**, in one app with role-based navigation decided at login.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Hilt (DI), Retrofit + kotlinx.serialization (networking), OkHttp
- DataStore (session/token persistence)
- Coil (images), osmdroid (live tracking maps — no Google Maps API key required)
- Razorpay Android Checkout SDK (payments, shop promotion)
- Accompanist Permissions, FusedLocationProviderClient (delivery rider GPS)

## Project layout

```
app/src/main/java/com/lazyshopper/app/
├── core/
│   ├── data/remote/        # DTOs (RequestDto/DomainDto/ResponseDto) + Retrofit API interfaces,
│   │                         1:1 with the backend's ~174 endpoints (see api_reference below)
│   ├── data/local/         # SessionManager (DataStore-backed auth session)
│   ├── di/                 # Hilt NetworkModule
│   ├── navigation/         # Routes.kt, RootNavGraph.kt (role dispatch)
│   ├── theme/               # "Organic & Earthy" palette ported from the web app
│   └── ui/                 # Shared UiState/ActionState + reusable Compose components
├── feature/
│   ├── auth/                # Login/Register (role toggle), forgot/reset password
│   ├── customer/            # Storefront, search, cart/checkout, orders+live tracking, chat,
│   │                         referrals, address book, account
│   ├── shopkeeper/          # KYC, shop & product management, orders, earnings + payouts
│   ├── delivery/            # KYC, availability + live GPS, active delivery, earnings
│   └── admin/                # Full ops console: catalog, orders, people, finance, support,
│                             analytics, settings (navigation-drawer shell)
└── rider/location/          # Foreground service pushing rider GPS every ~20s while online
```

Each role's screen tree is rooted at a single `<Role>NavGraph(rootNavController, onLogout)`
composable (`CustomerNavGraph`, `ShopkeeperNavGraph`, `DeliveryNavGraph`, `AdminNavGraph`), which
`RootNavGraph.kt` routes into once `/auth/login` (or register) returns the user's role.

## Building

This was built in a sandbox with **no Android SDK available**, so it has not been compiled here —
only carefully hand-written against the exact API contract and cross-checked for consistent
imports/signatures across every file. To build:

1. Open the `LazyShopperAndroid/` folder in Android Studio (Koala+ recommended), or run:
   ```
   ./gradlew assembleDebug
   ```
   (requires the Android SDK; Studio will prompt to install missing SDK/platform components).
2. First build will resolve dependencies from Google/Maven Central — no other setup needed for a
   debug build against the already-deployed backend.
3. **Razorpay key**: `app/build.gradle.kts` has a placeholder `RAZORPAY_KEY_ID` BuildConfig field
   (`rzp_test_placeholder`). Replace it with your real Razorpay key id (the *key id* only — never
   commit the key secret to the client) before testing payments.
4. **Backend URL**: set via `API_BASE_URL` in `app/build.gradle.kts` — defaults to the deployed
   `https://www.lazyshopper.in/`. Point it at `http://10.0.2.2:8000/` (already allow-listed in
   `network_security_config.xml` for cleartext) if you want to run against a local backend in the
   emulator instead.

## Test accounts

Per the backend's seeded data (see the original project's `memory/PRD.md`):
- Admin: `chemistryaqub@gmail.com` (the one true admin account — enforced server-side)
- Customer: `customer@test.com` / `Cust@12345`
- New Shopkeeper/Delivery accounts can be created via Register — they land in `pending` status
  until KYC is submitted and approved by the admin.

## Known simplifications

- **Google Sign-In**: the backend's `/auth/google` expects a session id from an Emergent-platform
  OAuth proxy, not a standard Google Identity Services token — this wasn't wired up natively since
  it needs a backend change to accept a real Google ID token. Email/password and phone-OTP-style
  flows work as-is.
- Charts (shopkeeper/admin earnings & analytics) use a small dependency-free Compose `Canvas` bar
  chart rather than a charting library, since library API surfaces couldn't be verified against a
  real compiler in this sandbox.
- KYC location capture uses device GPS ("detect my location") rather than a draggable map pin.
- Shop editing (post-creation) is limited to timings; there's no general shop-details edit beyond
  what the backend itself exposes (it only exposes create/timings/approve/delete for shops).
- PDF payout statements are generated on-device with `android.graphics.pdf.PdfDocument` (plain
  layout) and shared via a `FileProvider` intent, rather than matching the web app's styled PDF.

## Not build-verified

No Android SDK was available in the environment this was built in, so `./gradlew assembleDebug`
has not actually been run against this code. It was written carefully against the full backend API
reference and cross-checked for import/signature consistency across all ~140 files, but please
build it in Android Studio and treat the first build as a normal review/fix pass, not a formality.
