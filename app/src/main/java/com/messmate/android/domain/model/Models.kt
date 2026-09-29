package com.messmate.android.domain.model

// Mirrors backend UserRole enum — exact serialized string values
enum class UserRole {
    OWNER, MANAGER, MEMBER, CHEF, PRIMARY_MANAGER, STUDENT;

    val isManager: Boolean get() = this == OWNER || this == PRIMARY_MANAGER || this == MANAGER
}

enum class JoinRequestStatus { PENDING, APPROVED, REJECTED }
enum class MembershipStatus { ACTIVE, INACTIVE, SUSPENDED, LEFT }
enum class MealSession { BREAKFAST, LUNCH, DINNER }
enum class MealStatus { ON, OFF }
enum class CycleStatus { ACTIVE, EXPIRING, FINALIZING, COMPLETED, CANCELLED }
enum class ExpenseCategory {
    MEAL_VARIABLE, FIXED_OVERHEAD, INDIVIDUAL_DIRECT, AD_HOC_SPECIAL,
    GROCERY, VEGETABLE, GAS, MEAT_FISH, UTILITY, CHEF_SALARY, SPICE, OTHER
}
enum class PaymentMethod { CASH, BKASH, NAGAD, BANK_TRANSFER, OTHER }
enum class DepositStatus { PENDING, APPROVED, REJECTED }

// Domain models
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val avatarUrl: String?,
    val studentId: String?,
    val department: String?,
    val university: String?,
    val roomNumber: String?,
    val isProfileComplete: Boolean
)

data class Membership(
    val id: String,
    val messId: String,
    val messName: String,
    val messCode: String,
    val role: UserRole,
    val status: MembershipStatus,
    val joinDate: String
)

data class PendingJoinRequest(
    val id: String,
    val messId: String,
    val messName: String,
    val messCode: String,
    val ownerName: String,
    val status: JoinRequestStatus,
    val notes: String?,
    val createdAt: String
)

data class AuthState(
    val token: String,
    val user: User,
    val membership: Membership?,
    val pendingJoinRequest: PendingJoinRequest?
)

data class SessionConfig(
    val session: MealSession = MealSession.LUNCH,
    val unitValue: Double = 1.0,
    val cutoffTime: String = "12:00",   // "HH:mm" in Asia/Dhaka
    val isEnabled: Boolean = true,
    val servingStartTime: String? = null,
    val servingEndTime: String? = null
)

data class DiningConfig(
    val defaultCarryForward: Boolean = true,
    val defaultBreakfastOn: Boolean = true,
    val defaultLunchOn: Boolean = true,
    val defaultDinnerOn: Boolean = true,
    val currency: String = "BDT",
    val sessions: List<SessionConfig> = listOf(
        SessionConfig(session = MealSession.BREAKFAST, unitValue = 0.5, cutoffTime = "07:00", isEnabled = true),
        SessionConfig(session = MealSession.LUNCH, unitValue = 1.0, cutoffTime = "12:00", isEnabled = true),
        SessionConfig(session = MealSession.DINNER, unitValue = 1.0, cutoffTime = "19:00", isEnabled = true)
    )
)

data class DailyMealStatus(
    val id: String,
    val date: String,         // "YYYY-MM-DD"
    val session: MealSession,
    val status: MealStatus,
    val unitValue: Double,
    val isAutoCarried: Boolean
)

data class Deposit(
    val id: String,
    val cycleId: String,
    val messId: String,
    val userId: String,
    val userFullName: String,
    val roomNumber: String?,
    val amount: Double,
    val depositDate: String,
    val paymentMethod: PaymentMethod,
    val transactionRef: String?,
    val status: DepositStatus,
    val approvedByName: String?,
    val notes: String?,
    val createdAt: String
)

data class MemberSummary(
    val userId: String,
    val fullName: String,
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

data class CycleCalculation(
    val totalExpenses: Double,
    val mealVariableExpenses: Double,
    val fixedOverheadExpenses: Double,
    val mealRate: Double,
    val isFinal: Boolean,
    val isRateVolatile: Boolean,
    val volatilityReason: String?,
    val memberSummaries: List<MemberSummary>
)

data class ActiveCycle(
    val id: String,
    val messId: String,
    val cycleNumber: Int,
    val startDate: String,
    val targetActiveDays: Int,
    val countedActiveDays: Int,
    val remainingActiveDays: Int,
    val scheduledEndDate: String,
    val status: CycleStatus,
    val isExpiringSoon: Boolean
)

data class Expense(
    val id: String,
    val category: ExpenseCategory,
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

data class MenuItem(
    val id: String,
    val dayOfWeek: Int,    // 1 = Monday, 7 = Sunday
    val session: MealSession,
    val itemName: String,
    val description: String?,
    val category: String?,
    val dietaryTags: List<String>
)

data class WeeklyMenu(
    val id: String,
    val title: String,
    val effectiveFrom: String,
    val items: List<MenuItem>,
    val activeSessions: List<MealSession>
)

val MealSession.displayName: String
    get() = when (this) {
        MealSession.BREAKFAST -> "Breakfast"
        MealSession.LUNCH -> "Lunch"
        MealSession.DINNER -> "Dinner"
    }

val PaymentMethod.displayName: String
    get() = when (this) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.BKASH -> "bKash"
        PaymentMethod.NAGAD -> "Nagad"
        PaymentMethod.BANK_TRANSFER -> "Bank Transfer"
        PaymentMethod.OTHER -> "Other"
    }

val ExpenseCategory.displayName: String
    get() = name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

fun DiningConfig.getActiveSessionList(): List<MealSession> {
    val list = mutableListOf<MealSession>()
    if (defaultBreakfastOn || sessions.any { it.session == MealSession.BREAKFAST && it.isEnabled }) list.add(MealSession.BREAKFAST)
    if (defaultLunchOn || sessions.any { it.session == MealSession.LUNCH && it.isEnabled }) list.add(MealSession.LUNCH)
    if (defaultDinnerOn || sessions.any { it.session == MealSession.DINNER && it.isEnabled }) list.add(MealSession.DINNER)
    return if (list.isNotEmpty()) list else listOf(MealSession.BREAKFAST, MealSession.LUNCH, MealSession.DINNER)
}

data class MemberInfo(
    val id: String, // membershipId
    val userId: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val studentId: String?,
    val department: String?,
    val university: String?,
    val roomNumber: String?,
    val role: UserRole,
    val status: MembershipStatus,
    val joinDate: String,
    val netBalance: Double = 0.0
) {
    val displayNameWithRoom: String
        get() = if (!roomNumber.isNullOrBlank()) "$fullName — Room $roomNumber" else fullName
}

data class PastCycleSummary(
    val id: String,
    val cycleNumber: Int,
    val startDate: String,
    val endDate: String?,
    val targetActiveDays: Int,
    val totalExpenses: Double,
    val totalCountedUnits: Double,
    val mealRate: Double,
    val status: CycleStatus
)

val Deposit.displayNameWithRoom: String
    get() = if (!roomNumber.isNullOrBlank()) "$userFullName — Room $roomNumber" else userFullName

data class ManagerJoinRequest(
    val id: String,
    val messId: String,
    val messName: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String? = null,
    val roomNumber: String? = null,
    val status: JoinRequestStatus,
    val requestNotes: String? = null,
    val createdAt: String
) {
    val displayNameWithRoom: String
        get() = if (!roomNumber.isNullOrBlank()) "$userName — Room $roomNumber" else userName
}

data class ChefSessionHeadcount(
    val session: MealSession,
    val studentOnCount: Int,
    val guestMealCount: Int,
    val totalHeadcount: Int,
    val menuItemName: String?,
    val dietaryTags: List<String>,
    val notes: List<String>,
    val isEnabled: Boolean,
    val servingStartTime: String?,
    val servingEndTime: String?
)

data class ChefDailyHeadcount(
    val messId: String,
    val messName: String,
    val date: String,
    val sessions: List<ChefSessionHeadcount>,
    val isPaused: Boolean,
    val pauseReason: String?
)




