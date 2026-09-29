# MessMate Phase 1: API Endpoint Reference (Code-Verified)

> Extracted directly from backend source (`SecurityConfig.kt`, `controller/*.kt`, `dto/*.kt`, `model/enums/Enums.kt`).
> Base URL:
> - Android Emulator: `http://10.0.2.2:8080`
> - Physical Device (Local Wi-Fi): `http://<HOST_IP>:8080`

---

## 1. Authentication (`/api/auth`)

| Method | Path | Auth Required | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/auth/register` | No | `RegisterRequest` | `AuthResponse` | Creates user and returns JWT token |
| `POST` | `/api/auth/login` | No | `LoginRequest` | `AuthResponse` | Authenticates and returns JWT token |
| `GET` | `/api/auth/me` | Bearer Token | None | `AuthResponse` | Fetches authenticated user, active membership & pending join requests |
| `PUT` | `/api/auth/profile` | Bearer Token | `UpdateProfileRequest` | `UserDto` | Updates user details |

### DTOs:
```kotlin
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String? = null,
    val studentId: String? = null,
    val department: String? = null,
    val university: String? = null,
    val roomNumber: String? = null
)

data class UpdateProfileRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val studentId: String? = null,
    val department: String? = null,
    val university: String? = null,
    val roomNumber: String
)

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val avatarUrl: String?,
    val studentId: String? = null,
    val department: String? = null,
    val university: String? = null,
    val roomNumber: String? = null,
    val isProfileComplete: Boolean = false
)

data class MembershipDto(
    val id: String,
    val mealId: String,
    val mealName: String,
    val mealCode: String = "",
    val role: UserRole, // OWNER, PRIMARY_MANAGER, MANAGER, MEMBER, CHEF, STUDENT
    val status: MembershipStatus, // ACTIVE, INACTIVE, SUSPENDED, LEFT
    val joinDate: String, // ISO-8601 LocalDateTime
    val messId: String = mealId,
    val messName: String = mealName,
    val code: String = mealCode
)

data class PendingJoinRequestDto(
    val id: String,
    val mealId: String,
    val mealName: String,
    val mealCode: String,
    val ownerName: String,
    val status: JoinRequestStatus, // PENDING, APPROVED, REJECTED
    val requestNotes: String?,
    val createdAt: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto,
    val activeMembership: MembershipDto?,
    val pendingJoinRequest: PendingJoinRequestDto? = null
)
```

---

## 2. Mess & Membership (`/api/messes` / `/api/meals`)

| Method | Path | Auth Required | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/messes/verify-code` | Bearer Token / Public | `VerifyMealCodeRequest` | `MealCodeVerificationResponse` | Previews mess info before joining |
| `POST` | `/api/messes/join-request` | Bearer Token | `SubmitJoinRequest` | `MealJoinRequestDto` | Submits student join request (HTTP 201) |
| `GET` | `/api/messes/my-join-request` | Bearer Token | None | `MealJoinRequestDto` or empty `{}` | Checks pending request status |
| `DELETE` | `/api/messes/join-requests/{requestId}` | Bearer Token | None | `Map<String, String>` | Cancels pending join request |
| `GET` | `/api/messes/{id}/config` | Bearer Token | None | `DiningConfigDto` | Dining config (sessions, cutoffs, multipliers) |
| `GET` | `/api/messes/{id}` | Bearer Token | None | `MealDto` | Mess details & member count |

### DTOs:
```kotlin
data class VerifyMealCodeRequest(val code: String)

data class MealCodeVerificationResponse(
    val mealId: String,
    val name: String,
    val code: String,
    val address: String?,
    val ownerName: String,
    val memberCount: Int,
    val currentCycleNumber: Int,
    val currentCycleStatus: String
)

data class SubmitJoinRequest(
    val code: String,
    val notes: String? = null,
    val roomNumber: String? = null
)

data class MealJoinRequestDto(
    val id: String,
    val mealId: String,
    val mealName: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String?,
    val studentId: String?,
    val department: String?,
    val roomNumber: String?,
    val status: JoinRequestStatus,
    val requestNotes: String?,
    val reviewedByName: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String
)

data class SessionConfigDto(
    val session: MealSession, // BREAKFAST, LUNCH, DINNER
    val unitValue: Double,
    val cutoffTime: String, // "07:00", "12:00", "19:00"
    val isEnabled: Boolean,
    val servingStartTime: String? = null,
    val servingEndTime: String? = null
)

data class DiningConfigDto(
    val defaultCarryForward: Boolean,
    val defaultBreakfastOn: Boolean,
    val defaultLunchOn: Boolean,
    val defaultDinnerOn: Boolean,
    val currency: String,
    val sessions: List<SessionConfigDto>
)

data class MealDto(
    val id: String,
    val name: String,
    val code: String,
    val address: String?,
    val createdById: String,
    val memberCount: Int = 1,
    val currentCycleNumber: Int = 1,
    val currentCycleStatus: String = "ACTIVE"
)
```

---

## 3. Meal Status & Toggles (`/api/meals`)

| Method | Path | Auth Required | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/meals/toggle` | Bearer Token | `ToggleMealRequest` | `DailyMealStatusDto` | Toggles student meal status ON/OFF |
| `GET` | `/api/meals/my-status` | Bearer Token | Query: `startDate`, `endDate` | `List<DailyMealStatusDto>` | Status history / calendar window |

### DTOs:
```kotlin
data class ToggleMealRequest(
    val date: String, // "YYYY-MM-DD"
    val session: MealSession,
    val status: MealStatus // ON, OFF
)

data class DailyMealStatusDto(
    val id: String,
    val date: String, // "YYYY-MM-DD"
    val session: MealSession,
    val status: MealStatus,
    val unitValue: Double,
    val isAutoCarried: Boolean
)
```

---

## 4. Deposits & Wallet (`/api/deposits`, `/api/calculations`, `/api/cycles`)

| Method | Path | Auth Required | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/deposits` | Bearer Token | `CreateDepositRequest` | `DepositResponse` | Submit deposit top-up (HTTP 201) |
| `GET` | `/api/deposits` | Bearer Token | Query: `messId`, `userId`, `status` | `List<DepositResponse>` | Read student's deposit history |
| `GET` | `/api/calculations/preview` | Bearer Token | Query: `messId` | `CycleCalculationResult` | Read cycle meal rate & individual student breakdown |
| `GET` | `/api/cycles/active` | Bearer Token | Query: `messId` | `ActiveCycleDto` | Read active cycle days, progress, dates |

### DTOs:
```kotlin
data class CreateDepositRequest(
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // CASH, BKASH, NAGAD, BANK_TRANSFER, OTHER
    val transactionRef: String? = null,
    val notes: String? = null
)

data class DepositResponse(
    val id: String,
    val cycleId: String,
    val messId: String,
    val userId: String,
    val userFullName: String,
    val roomNumber: String? = null,
    val amount: Double,
    val depositDate: String, // ISO LocalDateTime
    val paymentMethod: PaymentMethod,
    val transactionRef: String?,
    val status: DepositStatus, // PENDING, APPROVED, REJECTED
    val approvedById: String?,
    val approvedByName: String?,
    val notes: String?,
    val createdAt: String
)

data class MemberSettlementSummary(
    val userId: String,
    val fullName: String,
    val activeDays: Int,
    val isProrated: Boolean,
    val proratedJoinDate: String?,
    val proratedLeaveDate: String?,
    val totalMemberUnits: Double,
    val totalGuestUnits: Double,
    val mealCost: Double,
    val guestCost: Double,
    val fixedOverheadCost: Double,
    val individualDirectCost: Double,
    val adHocSpecialCost: Double,
    val totalCost: Double,
    val totalDeposits: Double,
    val netBalance: Double,
    val hasNegativeBalance: Boolean
)

data class CycleCalculationResult(
    val totalExpenses: Double,
    val mealVariableExpenses: Double,
    val fixedOverheadExpenses: Double,
    val individualDirectExpenses: Double,
    val adHocSpecialExpenses: Double,
    val totalMemberUnits: Double,
    val totalGuestUnits: Double,
    val totalCountedUnits: Double,
    val mealRate: Double,
    val isFinal: Boolean,
    val isRateVolatile: Boolean,
    val volatilityReason: String?,
    val memberSummaries: List<MemberSettlementSummary>
)

data class PausedDayDto(
    val id: String,
    val date: String,
    val session: MealSession?,
    val reason: String
)

data class ActiveCycleDto(
    val id: String,
    val messId: String,
    val cycleNumber: Int,
    val startDate: String,
    val targetActiveDays: Int,
    val countedActiveDays: Int,
    val remainingActiveDays: Int,
    val scheduledEndDate: String,
    val status: CycleStatus,
    val isExpiringSoon: Boolean,
    val isCompleted: Boolean,
    val pausedDays: List<PausedDayDto>
)
```

---

## 5. Weekly Menu & Expenses (`/api/menus`, `/api/expenses`)

| Method | Path | Auth Required | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/menus/today` | Bearer Token | Query: `messId` | `List<MenuItemDto>` | Today's dishes for dashboard |
| `GET` | `/api/menus/weekly` | Bearer Token | Query: `messId` | `WeeklyMenuDto` | 7-day schedule for menu screen |
| `GET` | `/api/expenses` | Bearer Token | Query: `messId` | `List<ExpenseDto>` | Itemized expenses (read-only for student) |

### DTOs:
```kotlin
data class MenuItemDto(
    val id: String,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val session: MealSession,
    val itemName: String,
    val description: String?,
    val category: String?,
    val dietaryTags: List<String>
)

data class WeeklyMenuDto(
    val id: String,
    val title: String,
    val effectiveFrom: String,
    val items: List<MenuItemDto>,
    val activeSessions: List<MealSession> = emptyList()
)

data class ExpenseDto(
    val id: String,
    val category: ExpenseCategory, // MEAL_VARIABLE, FIXED_OVERHEAD, INDIVIDUAL_DIRECT, AD_HOC_SPECIAL, OTHER
    val title: String,
    val amount: Double,
    val expenseDate: String,
    val receiptUrl: String?,
    val recordedByName: String,
    val notes: String?,
    val targetMemberId: String? = null,
    val targetMemberName: String? = null,
    val participantIds: List<String> = emptyList()
)
```

---

## 6. Shared Enums (Exact Serialized Names)

```kotlin
enum class UserRole { OWNER, MANAGER, MEMBER, CHEF, PRIMARY_MANAGER, STUDENT }
enum class JoinRequestStatus { PENDING, APPROVED, REJECTED }
enum class MembershipStatus { ACTIVE, INACTIVE, SUSPENDED, LEFT }
enum class MealSession { BREAKFAST, LUNCH, DINNER }
enum class MealStatus { ON, OFF }
enum class CycleStatus { ACTIVE, EXPIRING, FINALIZING, COMPLETED, CANCELLED }
enum class ExpenseCategory { MEAL_VARIABLE, FIXED_OVERHEAD, INDIVIDUAL_DIRECT, AD_HOC_SPECIAL, OTHER }
enum class PaymentMethod { CASH, BKASH, NAGAD, BANK_TRANSFER, OTHER }
enum class DepositStatus { PENDING, APPROVED, REJECTED }
```

---

## 7. Manager Endpoints (Phase 2 Code-Verified)

### A. Expenses Management (`/api/expenses`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/expenses` | `OWNER, PRIMARY_MANAGER, MANAGER` | `CreateExpenseRequest` | `ExpenseDto` | Creates expense record |
| `PUT` | `/api/expenses/{id}` | `OWNER, PRIMARY_MANAGER, MANAGER` | `CreateExpenseRequest` | `ExpenseDto` | Updates expense (returns 409 if cycle settled) |
| `DELETE` | `/api/expenses/{id}` | `OWNER, PRIMARY_MANAGER, MANAGER` | None | `Map<String, String>` | Deletes expense (returns 409 if cycle settled) |

### B. Deposits Review (`/api/deposits`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/deposits/{id}/approve` | `OWNER, PRIMARY_MANAGER, MANAGER` | None | `DepositResponse` | Approves student deposit |
| `POST` | `/api/deposits/{id}/reject` | `OWNER, PRIMARY_MANAGER, MANAGER` | `ReviewDepositRequest` (optional) | `DepositResponse` | Rejects student deposit |
| `POST` | `/api/deposits/{id}/review` | `OWNER, PRIMARY_MANAGER, MANAGER` | `ReviewDepositRequest` | `DepositResponse` | Explicit review (approved boolean + rejectionReason) |
| `PUT` | `/api/deposits/{id}` | `OWNER, PRIMARY_MANAGER, MANAGER` | `UpdateDepositRequest` | `DepositResponse` | Modifies deposit details |
| `DELETE` | `/api/deposits/{id}` | `OWNER, PRIMARY_MANAGER, MANAGER` | None | `Map<String, String>` | Deletes deposit |

### C. Members Roster (`/api/messes` / `/api/meals`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/messes/{id}/members` | Authenticated | None | `List<MemberResponseDto>` | Fetches roster with room, role, phone, etc. |
| `PUT` | `/api/messes/{id}/members/{membershipId}/room` | `OWNER, PRIMARY_MANAGER, MANAGER` | `UpdateMemberRoomRequest` | `Map<String, Any?>` | Updates member's room number |
| `PUT` | `/api/messes/{id}/members/{membershipId}/role` | `OWNER, PRIMARY_MANAGER, MANAGER` | `UpdateRoleRequest` | `Map<String, Any?>` | Changes member role |
| `DELETE` | `/api/messes/{id}/members/{membershipId}` | `OWNER, PRIMARY_MANAGER, MANAGER` | None | `Map<String, Any?>` | Removes member from mess |

### D. Join Requests (`/api/messes` / `/api/meals`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/messes/{id}/join-requests` | `OWNER, PRIMARY_MANAGER, MANAGER` | None | `List<MealJoinRequestDto>` | List all join requests |
| `POST` | `/api/messes/{id}/join-requests/{reqId}/review` | `OWNER, PRIMARY_MANAGER, MANAGER` | `ReviewJoinRequest` | `MealJoinRequestDto` | Approve/reject with notes |

### E. Menu Planner CRUD (`/api/menus`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/menus/items` | `OWNER, PRIMARY_MANAGER, MANAGER` | `CreateMenuItemRequest` | `MenuItemDto` | Adds dish to 7-day schedule |
| `DELETE` | `/api/menus/items/{id}` | `OWNER, PRIMARY_MANAGER, MANAGER` | None | `Map<String, String>` | Removes scheduled dish |

### F. Cycle & Settings (`/api/cycles`, `/api/calculations`, `/api/messes`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `PUT` | `/api/messes/{id}/config` | `OWNER, PRIMARY_MANAGER, MANAGER` | `DiningConfigDto` | `DiningConfigDto` | Updates dining sessions & cutoffs |
| `POST` | `/api/cycles/pause-day` | `OWNER, PRIMARY_MANAGER, MANAGER` | `PauseDayRequest` | `ActiveCycleDto` | Pauses holiday/emergency date |
| `POST` | `/api/calculations/finalize` | `OWNER, PRIMARY_MANAGER, MANAGER` | `FinalizeCycleRequest` | `CycleCalculationResult` | Concludes cycle (confirmForfeitSurplus if surplus & no carry-forward) |
| `POST` | `/api/cycles/new` | `OWNER, PRIMARY_MANAGER, MANAGER` | Query: `messId`, Body: `StartNewCycleRequest` | `ActiveCycleDto` | Starts next billing cycle |
| `GET` | `/api/cycles/history` | Authenticated | Query: `messId` | `List<PastCycleSummaryDto>` | Historical cycle records |

---

## 8. Chef Kitchen Prep Station Endpoints (Phase 3 Code-Verified)

### A. Chef Headcount Endpoint (`/api/chef/headcount`)
| Method | Path | Auth / Role | Query Params | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/chef/headcount` | `CHEF, OWNER, PRIMARY_MANAGER, MANAGER` | `messId: String`, `date: LocalDate?` (ISO "YYYY-MM-DD", defaults to today in Asia/Dhaka) | `ChefDailyHeadcountResponse` | Headcounts, dish names, dietary notes & pause flags |

### B. Chef DTOs (`ManagerAndChefDtos.kt`)
```kotlin
data class ChefDailyHeadcountResponse(
    val messId: String,
    val messName: String,
    val date: LocalDate, // ISO "YYYY-MM-DD"
    val sessions: List<ChefSessionHeadcount>,
    val isPaused: Boolean = false,
    val pauseReason: String? = null
)

data class ChefSessionHeadcount(
    val session: MealSession, // BREAKFAST, LUNCH, DINNER
    val studentOnCount: Int,
    val guestMealCount: Int,
    val totalHeadcount: Int,
    val menuItemName: String?,
    val dietaryTags: List<String>,
    val notes: List<String>,
    val isEnabled: Boolean = true,
    val servingStartTime: String? = null,
    val servingEndTime: String? = null
)
```

### C. Kitchen Terminal Business Rules & Guarantees:
1. **Aggregated Headcounts**: Backend computes `studentOnCount` (active members default-ON + explicit status records) + `guestMealCount`. The app displays the backend number directly without client-side recalculation.
2. **Holiday / Paused Day (`isPaused`)**: Verified to exist on the backend! If a date is paused, `isPaused = true`, `pauseReason` contains the holiday reason, `isEnabled = false`, and `totalHeadcount = 0`.
3. **No Financial Data**: Strictly zero expense amounts, student balances, or deposit information are ever returned or displayed on the kitchen prep station.
4. **Anonymized Dietary Notes**: Aggregated notes list from guest bookings (e.g. "Low spice", "Vegetarian portion") without student identity.
5. **Periodic Auto-Refresh**: Polls every 60 seconds while in foreground to reflect locking of portions as cutoffs pass.

---

## 9. Push Notifications & FCM Device Token Management (Phase 4 Code-Verified)

### A. Device Token Endpoints (`/api/notifications`)
| Method | Path | Auth / Role | Request DTO | Response DTO | Notes |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/notifications/device-token` | Authenticated (Any role) | `{"token": String, "platform": String?}` | `{"status": "success", "message": "..."}` | Upserts FCM token for authenticated user (`userId` from JWT). Deduplicates tokens and handles device transfer/rotation. |
| `DELETE` | `/api/notifications/device-token` | Authenticated (Any role) | Query: `token: String` | `{"status": "success", "message": "..."}` | Unregisters FCM token on logout |

### B. Server-Side Push Notification Triggers (Firebase Admin SDK)
| Event | Trigger Location | Target Audience | Payload `screen` | Payload `type` | Description |
|:---|:---|:---|:---|:---|:---|
| **Deposit Reviewed** | `DepositService.reviewDeposit()` | Depositing Student | `wallet` | `DEPOSIT_APPROVED` / `DEPOSIT_REJECTED` | Triggered immediately when manager approves or rejects deposit. Title & body display amount and status. |
| **New Join Request** | `MessService.submitJoinRequest()` | Mess Managers & Owner | `join_requests` | `NEW_JOIN_REQUEST` | Alert sent to all active managers/owner when a new student requests to join. |
| **Join Request Reviewed** | `MessService.reviewJoinRequest()` | Applicant Student | `student_home` / `join_mess` | `JOIN_REQUEST_APPROVED` / `JOIN_REQUEST_REJECTED` | Alert sent when manager approves or declines enrollment. |
| **Meal Cutoff Approaching** | `MealCutoffScheduler.checkCutoffApproaching()` | Students without status | `meals` | `MEAL_CUTOFF` | Periodic `@Scheduled` check (~30 min before session cutoff) reminding students who haven't selected a meal. |
| **Cycle Concluded** | `CycleCalculationService.finalizeCycle()` | All active members | `wallet` | `CYCLE_CONCLUDED` | Notifies all members with final meal rate and link to balance ledger. |
| **New Cycle Started** | `CycleCalculationService.startNewCycle()` | All active members | `wallet` | `NEW_CYCLE_STARTED` | Notifies all members of the new billing cycle start. |

### C. Server-Side Safety & Stale Token Pruning
1. **Non-Blocking Execution**: Notification dispatch calls are wrapped in non-blocking try/catch blocks; an FCM failure will **never** roll back an approved deposit, join request, or cycle finalization.
2. **Invalid Token Pruning**: When Firebase Admin SDK returns `UNREGISTERED` or `INVALID_ARGUMENT`, `NotificationService` automatically deletes the stale token from the `device_tokens` table.
3. **Simulation Mode**: If Firebase credentials are not supplied via env vars, the backend operates in simulation mode with diagnostic logging rather than failing startup.


