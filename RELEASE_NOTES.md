# MessMate Android — Release Notes (v1.0.0)

**Version:** 1.0.0 (versionCode 1)  
**Target Platform:** Android 8.0+ (API 26–35)  
**Build Variant:** Release (Minified with R8, Signed with v2 APK signature)  
**Backend Target:** Production (`https://messmate-backend-iwhm.onrender.com/`)

---

## 🍽️ Overview

MessMate Android v1.0.0 brings the complete mess management platform to native Android with Jetpack Compose, Material 3, and Kotlin Coroutines. It unifies operations for Students, Mess Managers, and Kitchen Staff (Chefs) into a single cohesive, high-performance application.

---

## 🚀 Key Modules & Features

### 1. Student Module (Phase 1)
- **Today & Tomorrow Meal Management:** One-tap toggle for Breakfast, Lunch, and Dinner with strict cutoff timer enforcement.
- **Deposit Requests:** Submit cash/bKash/Nagad deposit requests with transaction IDs; view real-time approval status.
- **Cycle & Ledger:** Track monthly meal count, guest counts, mess fee balances, and calculated cycle costs.
- **Join Mess Flow:** Search and join mess halls by invite code; wait for manager approval.

### 2. Manager Module (Phase 2)
- **Overview & Metrics:** Live mess status, active members, balance summaries, and cycle health.
- **Deposit Approvals:** Approve or reject deposit requests with receipt review and audit tracking.
- **Expense Logging:** Categorized grocery and utility expense management with per-item action isolation.
- **Join Request Triage:** Accept or decline student enrollment requests.
- **Member Directory:** Room assignment, role management, and active status controls.
- **Weekly Menu Planner:** Schedule daily dishes for Breakfast, Lunch, and Dinner.
- **Cycle Management:** Conclude billing cycles, freeze meal tallies, and start new billing periods.

### 3. Chef Prep Station (Phase 3)
- **Real-Time Headcounts:** Live meal tallies (regular + guest count) for upcoming sessions.
- **Date Navigation:** Browse today and tomorrow prep schedules; automatic pause state detection.
- **Menu & Dietary Notes:** Display scheduled dishes and member special dietary requests per meal session.

---

## 🛡️ Reliability & Resilience (Phase 4)

- **Offline Support & Timestamped Cache:**
  - Fallback cache backed by Jetpack DataStore preserves last-known meal statuses, headcount, and manager dashboards.
  - Informative banner: `"Showing cached data from [time] (Offline)"`.
  - Immediate offline rejection on mutating actions: `"You're offline — meal changes require a connection"`. Never displays false-positive optimistic updates.
- **Connectivity Monitoring:**
  - Global `ConnectivityManager` listener with a top status bar warning on network drop.
  - Safe exponential retry backoff on idempotent GET queries; zero auto-retries on financial actions.
- **Global 401 Session Interceptor:**
  - Automatic token clearing and navigation back to Login when JWT expires.
  - Meaningful HTTP 403 / 409 backend error messages surfaced directly in snackbars/banners.
- **Timezone Uniformity:**
  - All dates and cutoff timestamps throughout the app are normalized to `Asia/Dhaka` via `DhakaDateUtils`.

---

## 🔔 Push Notifications (FCM)

- **Client Implementation:**
  - Integrated Firebase Cloud Messaging (FCM) using the official `com.google.gms.google-services` plugin and Firebase BoM (`33.10.0`) with `firebase-messaging-ktx`.
  - Android 13+ runtime permissions (`POST_NOTIFICATIONS`) requested transparently on startup.
  - Dedicated notification channels initialized at startup via `MessMateApp`:
    - 🔔 **Default Notifications** (`messmate_channel_default`): General alerts and background Firebase console dispatches.
    - 💰 **Financial Alerts** (`messmate_channel_financial`): High-priority deposit approvals and balance alerts.
    - 🍽️ **Meal Alerts** (`messmate_channel_meals`): Cutoff countdown nudges and meal updates.
    - 📢 **General & System** (`messmate_channel_general`): Join request notices and cycle conclusions.
  - Foreground HUD banners + background notifications with tap-to-navigate routing to relevant screens via `screen` / `target_route` data payloads (e.g. `screen: "wallet"`, `screen: "deposits"`, `screen: "meals"`).
  - Immediate token registration upon login (`POST /api/notifications/device-token`), token refresh (`onNewToken`), and token unregistration on logout (`DELETE /api/notifications/device-token`).

### ⚠️ Known Limitation / Backend Follow-Up:
The Android client and backend device-token registration endpoints are fully implemented. Live push triggers require connecting backend business services to the FCM dispatch pipeline:
1. `DepositService.updateDepositStatus` → trigger `DEPOSIT_APPROVED` / `DEPOSIT_REJECTED`.
2. `MessService.reviewJoinRequest` → trigger `JOIN_REQUEST_APPROVED` / `JOIN_REQUEST_REJECTED`.
3. `MessService.createJoinRequest` → trigger `NEW_JOIN_REQUEST` to mess managers.
4. Scheduled background worker → trigger cutoff warnings 30 minutes before meal cutoffs.
5. `CycleService` → trigger `CYCLE_CONCLUDED` / `NEW_CYCLE_STARTED`.

---

## 📦 Distribution & Packaging

- **Release Build:** Minified with R8 shrinking and obfuscation (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **Signing:** Release APK signed with Android APK Signature Scheme v2.
- **ProGuard / R8 Rules:** Configured keep rules for Retrofit, OkHttp, Gson, Kotlin Coroutines, Hilt, and Firebase.
- **APK Size:** ~2.9 MB.
