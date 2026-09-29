package com.messmate.android.data.remote.dto

import com.messmate.android.domain.model.*

// ======================== AUTH ========================

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String? = null,
    val studentId: String? = null,
    val department: String? = null,
    val university: String? = null,
    val roomNumber: String? = null
)

data class UserDtoRemote(
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

data class MembershipDtoRemote(
    val id: String,
    val mealId: String? = null,
    val messId: String? = null,
    val mealName: String? = null,
    val messName: String? = null,
    val mealCode: String? = null,
    val code: String? = null,
    val role: String,
    val status: String,
    val joinDate: String
)

data class PendingJoinRequestDtoRemote(
    val id: String,
    val mealId: String? = null,
    val messId: String? = null,
    val mealName: String? = null,
    val messName: String? = null,
    val mealCode: String? = null,
    val ownerName: String,
    val status: String,
    val requestNotes: String?,
    val createdAt: String
)

data class AuthResponseDto(
    val token: String,
    val user: UserDtoRemote,
    val activeMembership: MembershipDtoRemote?,
    val pendingJoinRequest: PendingJoinRequestDtoRemote?
)

// ======================== MESS / JOIN ========================

data class VerifyMealCodeRequestDto(val code: String)

data class MealCodeVerificationResponseDto(
    val mealId: String,
    val name: String,
    val code: String,
    val address: String?,
    val ownerName: String,
    val memberCount: Int,
    val currentCycleNumber: Int,
    val currentCycleStatus: String
)

data class SubmitJoinRequestDto(
    val code: String,
    val notes: String? = null,
    val roomNumber: String? = null
)

data class MealJoinRequestDtoRemote(
    val id: String,
    val mealId: String,
    val mealName: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String? = null,
    val studentId: String? = null,
    val department: String? = null,
    val roomNumber: String? = null,
    val status: String,
    val requestNotes: String? = null,
    val reviewedByName: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String
)

// ======================== DINING CONFIG ========================

data class SessionConfigDtoRemote(
    val session: String,
    val unitValue: Double,
    val cutoffTime: String,
    val isEnabled: Boolean,
    val servingStartTime: String?,
    val servingEndTime: String?
)

data class DiningConfigDtoRemote(
    val defaultCarryForward: Boolean,
    val defaultBreakfastOn: Boolean,
    val defaultLunchOn: Boolean,
    val defaultDinnerOn: Boolean,
    val currency: String,
    val sessions: List<SessionConfigDtoRemote>
)

// ======================== MEALS ========================

data class ToggleMealRequestDto(
    val date: String,
    val session: String,
    val status: String
)

data class DailyMealStatusDtoRemote(
    val id: String,
    val date: String,
    val session: String,
    val status: String,
    val unitValue: Double,
    val isAutoCarried: Boolean
)

// ======================== DEPOSITS ========================

data class CreateDepositRequestDto(
    val amount: Double,
    val paymentMethod: String,
    val transactionRef: String?,
    val notes: String?
)

data class DepositResponseDto(
    val id: String,
    val cycleId: String,
    val messId: String,
    val userId: String,
    val userFullName: String,
    val roomNumber: String?,
    val amount: Double,
    val depositDate: String,
    val paymentMethod: String,
    val transactionRef: String?,
    val status: String,
    val approvedById: String?,
    val approvedByName: String?,
    val notes: String?,
    val createdAt: String
)

// ======================== CALCULATION ========================

data class MemberSettlementSummaryDto(
    val userId: String,
    val fullName: String,
    val activeDays: Int,
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

data class CycleCalculationResultDto(
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
    val memberSummaries: List<MemberSettlementSummaryDto>
)

// ======================== CYCLE ========================

data class PausedDayDtoRemote(
    val id: String,
    val date: String,
    val session: String?,
    val reason: String
)

data class ActiveCycleDtoRemote(
    val id: String,
    val messId: String,
    val cycleNumber: Int,
    val startDate: String,
    val targetActiveDays: Int,
    val countedActiveDays: Int,
    val remainingActiveDays: Int,
    val scheduledEndDate: String,
    val status: String,
    val isExpiringSoon: Boolean,
    val isCompleted: Boolean,
    val pausedDays: List<PausedDayDtoRemote>
)

// ======================== EXPENSES ========================

data class ExpenseDtoRemote(
    val id: String,
    val category: String,
    val title: String,
    val amount: Double,
    val expenseDate: String,
    val receiptUrl: String?,
    val recordedByName: String,
    val notes: String?,
    val targetMemberId: String?,
    val targetMemberName: String?,
    val participantIds: List<String>
)

// ======================== MENU ========================

data class MenuItemDtoRemote(
    val id: String,
    val dayOfWeek: Int,
    val session: String,
    val itemName: String,
    val description: String?,
    val category: String?,
    val dietaryTags: List<String>
)

data class WeeklyMenuDtoRemote(
    val id: String,
    val title: String,
    val effectiveFrom: String,
    val items: List<MenuItemDtoRemote>,
    val activeSessions: List<String>
)

// ======================== PHASE 2 MANAGER DTOs ========================

data class CreateExpenseRequestDto(
    val category: String,
    val title: String,
    val amount: Double,
    val expenseDate: String,
    val receiptUrl: String? = null,
    val notes: String? = null,
    val targetMemberId: String? = null,
    val participantIds: List<String>? = emptyList()
)

data class ReviewDepositRequestDto(
    val approved: Boolean,
    val rejectionReason: String? = null
)

data class UpdateDepositRequestDto(
    val paymentMethod: String? = null,
    val transactionRef: String? = null,
    val notes: String? = null
)

data class MemberDtoRemote(
    val id: String,
    val userId: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val studentId: String?,
    val department: String?,
    val university: String?,
    val roomNumber: String?,
    val role: String,
    val canonicalRole: String?,
    val status: String,
    val joinDate: String
)

data class UpdateMemberRoomRequestDto(
    val roomNumber: String?
)

data class UpdateRoleRequestDto(
    val role: String
)

data class ReviewJoinRequestDto(
    val approved: Boolean,
    val notes: String? = null
)

data class CreateMenuItemRequestDto(
    val messId: String,
    val dayOfWeek: Int,
    val session: String,
    val itemName: String,
    val description: String? = null,
    val category: String? = null,
    val dietaryTags: List<String> = emptyList()
)

data class PauseDayRequestDto(
    val messId: String,
    val pausedDate: String,
    val session: String? = null,
    val reason: String
)

data class FinalizeCycleRequestDto(
    val messId: String,
    val confirmForfeitSurplus: Boolean = false
)

data class StartNewCycleRequestDto(
    val targetActiveDays: Int = 30,
    val defaultCarryForward: Boolean = true,
    val breakfastEnabled: Boolean = true,
    val lunchEnabled: Boolean = true,
    val dinnerEnabled: Boolean = true,
    val breakfastCutoff: String = "07:00",
    val lunchCutoff: String = "12:00",
    val dinnerCutoff: String = "19:00",
    val breakfastMultiplier: Double = 0.50,
    val lunchMultiplier: Double = 1.00,
    val dinnerMultiplier: Double = 1.00,
    val combinedSessionRule: String = "SEPARATE",
    val notes: String? = null,
    val confirmForfeitSurplus: Boolean = false
)

data class PastCycleSummaryDtoRemote(
    val id: String,
    val cycleNumber: Int,
    val startDate: String,
    val endDate: String?,
    val targetActiveDays: Int,
    val totalExpenses: Double,
    val totalCountedUnits: Double,
    val mealRate: Double,
    val status: String
)

// ======================== PHASE 3 CHEF DTOs ========================

data class ChefSessionHeadcountDtoRemote(
    val session: String,
    val studentOnCount: Int,
    val guestMealCount: Int,
    val totalHeadcount: Int,
    val menuItemName: String?,
    val dietaryTags: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val servingStartTime: String? = null,
    val servingEndTime: String? = null
)

data class ChefDailyHeadcountResponseDto(
    val messId: String,
    val messName: String,
    val date: String,
    val sessions: List<ChefSessionHeadcountDtoRemote>,
    val isPaused: Boolean = false,
    val pauseReason: String? = null
)

// ======================== NOTIFICATION DTOs ========================

data class DeviceTokenRequestDto(
    val token: String,
    val deviceType: String = "ANDROID"
)


