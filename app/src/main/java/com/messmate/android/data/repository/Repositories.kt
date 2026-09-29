package com.messmate.android.data.repository

import com.messmate.android.data.local.TokenDataStore
import com.messmate.android.data.remote.MessMateApiService
import com.messmate.android.data.remote.dto.*
import com.messmate.android.domain.model.*
import kotlinx.coroutines.flow.first
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

private suspend fun <T, R> Response<T>.toResult(transform: (T) -> R): ApiResult<R> {
    return if (isSuccessful && body() != null) {
        ApiResult.Success(transform(body()!!))
    } else {
        val statusCode = code()
        val rawError = errorBody()?.string() ?: ""
        val parsedMessage = try {
            val json = org.json.JSONObject(rawError)
            json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }

        val finalMessage = parsedMessage
            ?: if (rawError.isNotBlank() && !rawError.trim().startsWith("<")) rawError.trim()
            else when (statusCode) {
                409 -> "Action could not be completed due to a conflict or lock"
                403 -> "You are not authorized to perform this action"
                404 -> "Resource not found"
                400 -> "Invalid request"
                else -> "Error ($statusCode)"
            }

        ApiResult.Error(
            message = finalMessage,
            code = statusCode
        )
    }
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: MessMateApiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun login(email: String, password: String): ApiResult<AuthState> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            response.toResult { it.toDomain() }.also { result ->
                if (result is ApiResult.Success) saveAuth(result.data)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(
        email: String, password: String, fullName: String, phone: String?,
        studentId: String?, department: String?, university: String?, roomNumber: String?
    ): ApiResult<AuthState> {
        return try {
            val response = api.register(
                RegisterRequestDto(email, password, fullName, phone, studentId, department, university, roomNumber)
            )
            response.toResult { it.toDomain() }.also { result ->
                if (result is ApiResult.Success) saveAuth(result.data)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMe(): ApiResult<AuthState> {
        return try {
            val response = api.getMe()
            response.toResult { it.toDomain() }.also { result ->
                if (result is ApiResult.Success) saveAuth(result.data)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() = tokenDataStore.clear()

    suspend fun getSavedToken(): String? = tokenDataStore.token.first()
    suspend fun getSavedMessId(): String? = tokenDataStore.messId.first()?.takeIf { it.isNotBlank() }
    suspend fun getSavedUserId(): String? = tokenDataStore.userId.first()

    private suspend fun saveAuth(state: AuthState) {
        tokenDataStore.saveAuth(
            token = state.token,
            userId = state.user.id,
            messId = state.membership?.messId,
            role = state.membership?.role?.name ?: "STUDENT",
            name = state.user.fullName
        )
    }
}

@Singleton
class MessRepository @Inject constructor(
    private val api: MessMateApiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun verifyCode(code: String): ApiResult<MealCodeVerificationResponseDto> {
        return try {
            api.verifyMessCode(VerifyMealCodeRequestDto(code)).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun submitJoinRequest(code: String, notes: String?, roomNumber: String?): ApiResult<MealJoinRequestDtoRemote> {
        return try {
            api.submitJoinRequest(SubmitJoinRequestDto(code, notes, roomNumber)).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMyJoinRequest(): ApiResult<MealJoinRequestDtoRemote?> {
        return try {
            val response = api.getMyJoinRequest()
            if (response.isSuccessful) ApiResult.Success(response.body())
            else ApiResult.Error(response.errorBody()?.string() ?: "Error", response.code())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun cancelJoinRequest(requestId: String): ApiResult<Unit> {
        return try {
            api.cancelJoinRequest(requestId).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getDiningConfig(messId: String): ApiResult<DiningConfig> {
        return try {
            api.getDiningConfig(messId).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateDiningConfig(messId: String, config: DiningConfig): ApiResult<DiningConfig> {
        return try {
            api.updateDiningConfig(messId, config.toDto()).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMembers(messId: String): ApiResult<List<MemberInfo>> {
        return try {
            api.getMembers(messId).toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateMemberRoom(messId: String, membershipId: String, roomNumber: String?): ApiResult<Unit> {
        return try {
            api.updateMemberRoom(messId, membershipId, UpdateMemberRoomRequestDto(roomNumber)).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateMemberRole(messId: String, membershipId: String, role: UserRole): ApiResult<Unit> {
        return try {
            api.updateMemberRole(messId, membershipId, UpdateRoleRequestDto(role.name)).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun removeMember(messId: String, membershipId: String): ApiResult<Unit> {
        return try {
            api.removeMember(messId, membershipId).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun listJoinRequests(messId: String): ApiResult<List<ManagerJoinRequest>> {
        return try {
            api.listJoinRequests(messId).toResult { list -> list.map { it.toDomainManager() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun reviewJoinRequest(messId: String, requestId: String, approved: Boolean, notes: String? = null): ApiResult<ManagerJoinRequest> {
        return try {
            api.reviewJoinRequest(messId, requestId, ReviewJoinRequestDto(approved, notes)).toResult { it.toDomainManager() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class MealRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun toggleMeal(date: String, session: MealSession, status: MealStatus): ApiResult<DailyMealStatus> {
        return try {
            api.toggleMeal(ToggleMealRequestDto(date, session.name, status.name))
                .toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMyMealStatuses(startDate: String, endDate: String): ApiResult<List<DailyMealStatus>> {
        return try {
            api.getMyMealStatuses(startDate, endDate).toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class DepositRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun submitDeposit(
        amount: Double, paymentMethod: PaymentMethod,
        transactionRef: String?, notes: String?
    ): ApiResult<Deposit> {
        return try {
            api.submitDeposit(CreateDepositRequestDto(amount, paymentMethod.name, transactionRef, notes))
                .toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun listDeposits(messId: String?, userId: String?): ApiResult<List<Deposit>> {
        return try {
            api.listDeposits(messId, userId).toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun approveDeposit(id: String): ApiResult<Deposit> {
        return try {
            api.approveDeposit(id).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun rejectDeposit(id: String, reason: String? = null): ApiResult<Deposit> {
        return try {
            api.rejectDeposit(id, ReviewDepositRequestDto(approved = false, rejectionReason = reason ?: "Rejected by manager")).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateDeposit(id: String, paymentMethod: PaymentMethod?, transactionRef: String?, notes: String?): ApiResult<Deposit> {
        return try {
            api.updateDeposit(id, UpdateDepositRequestDto(paymentMethod?.name, transactionRef, notes)).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteDeposit(id: String): ApiResult<Unit> {
        return try {
            api.deleteDeposit(id).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class CalculationRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun previewCalculation(messId: String): ApiResult<CycleCalculationResultDto> {
        return try {
            api.previewCalculation(messId).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getActiveCycle(messId: String): ApiResult<ActiveCycleDtoRemote> {
        return try {
            api.getActiveCycle(messId).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun pauseDay(messId: String, pausedDate: String, session: MealSession?, reason: String): ApiResult<ActiveCycleDtoRemote> {
        return try {
            api.pauseDay(PauseDayRequestDto(messId, pausedDate, session?.name, reason)).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun finalizeCalculation(messId: String, confirmForfeitSurplus: Boolean = false): ApiResult<CycleCalculationResultDto> {
        return try {
            api.finalizeCalculation(FinalizeCycleRequestDto(messId, confirmForfeitSurplus)).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun startNewCycle(messId: String, request: StartNewCycleRequestDto): ApiResult<ActiveCycleDtoRemote> {
        return try {
            api.startNewCycle(messId, request).toResult { it }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getCycleHistory(messId: String): ApiResult<List<PastCycleSummary>> {
        return try {
            api.getCycleHistory(messId).toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class ExpenseRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun listExpenses(messId: String): ApiResult<List<Expense>> {
        return try {
            api.listExpenses(messId).toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun createExpense(
        category: ExpenseCategory, title: String, amount: Double,
        expenseDate: String, receiptUrl: String? = null, notes: String? = null,
        targetMemberId: String? = null, participantIds: List<String> = emptyList()
    ): ApiResult<Expense> {
        return try {
            api.createExpense(
                CreateExpenseRequestDto(
                    category = category.name,
                    title = title,
                    amount = amount,
                    expenseDate = expenseDate,
                    receiptUrl = receiptUrl,
                    notes = notes,
                    targetMemberId = targetMemberId,
                    participantIds = participantIds
                )
            ).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateExpense(
        id: String, category: ExpenseCategory, title: String, amount: Double,
        expenseDate: String, receiptUrl: String? = null, notes: String? = null,
        targetMemberId: String? = null, participantIds: List<String> = emptyList()
    ): ApiResult<Expense> {
        return try {
            api.updateExpense(
                id,
                CreateExpenseRequestDto(
                    category = category.name,
                    title = title,
                    amount = amount,
                    expenseDate = expenseDate,
                    receiptUrl = receiptUrl,
                    notes = notes,
                    targetMemberId = targetMemberId,
                    participantIds = participantIds
                )
            ).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteExpense(id: String): ApiResult<Unit> {
        return try {
            api.deleteExpense(id).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class MenuRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun getTodayMenu(messId: String): ApiResult<List<MenuItem>> {
        return try {
            api.getTodayMenu(messId).toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getWeeklyMenu(messId: String): ApiResult<WeeklyMenu> {
        return try {
            api.getWeeklyMenu(messId).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun createMenuItem(
        messId: String, dayOfWeek: Int, session: MealSession,
        itemName: String, description: String? = null, category: String? = null,
        dietaryTags: List<String> = emptyList()
    ): ApiResult<MenuItem> {
        return try {
            api.createMenuItem(
                CreateMenuItemRequestDto(
                    messId = messId,
                    dayOfWeek = dayOfWeek,
                    session = session.name,
                    itemName = itemName,
                    description = description,
                    category = category,
                    dietaryTags = dietaryTags
                )
            ).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteMenuItem(id: String): ApiResult<Unit> {
        return try {
            api.deleteMenuItem(id).toResult { Unit }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

// ======================== MAPPING ========================

private fun AuthResponseDto.toDomain(): AuthState = AuthState(
    token = token,
    user = user.toDomain(),
    membership = activeMembership?.toDomain(),
    pendingJoinRequest = pendingJoinRequest?.toDomain()
)

private fun UserDtoRemote.toDomain() = com.messmate.android.domain.model.User(
    id, email, fullName, phone, avatarUrl, studentId, department, university, roomNumber, isProfileComplete
)

private fun MembershipDtoRemote.toDomain() = Membership(
    id = id,
    messId = messId ?: mealId ?: "",
    messName = messName ?: mealName ?: "",
    messCode = code ?: mealCode ?: "",
    role = UserRole.values().firstOrNull { it.name == role } ?: UserRole.MEMBER,
    status = MembershipStatus.values().firstOrNull { it.name == status } ?: MembershipStatus.ACTIVE,
    joinDate = joinDate
)

private fun PendingJoinRequestDtoRemote.toDomain() = PendingJoinRequest(
    id = id,
    messId = messId ?: mealId ?: "",
    messName = messName ?: mealName ?: "",
    messCode = mealCode ?: "",
    ownerName = ownerName,
    status = JoinRequestStatus.values().firstOrNull { it.name == status } ?: JoinRequestStatus.PENDING,
    notes = requestNotes,
    createdAt = createdAt
)

private fun DiningConfigDtoRemote.toDomain() = DiningConfig(
    defaultCarryForward, defaultBreakfastOn, defaultLunchOn, defaultDinnerOn, currency,
    sessions.map { s ->
        SessionConfig(
            session = MealSession.values().first { it.name == s.session },
            unitValue = s.unitValue,
            cutoffTime = s.cutoffTime,
            isEnabled = s.isEnabled,
            servingStartTime = s.servingStartTime,
            servingEndTime = s.servingEndTime
        )
    }
)

private fun DailyMealStatusDtoRemote.toDomain() = DailyMealStatus(
    id, date,
    session = MealSession.values().first { it.name == session },
    status = MealStatus.values().first { it.name == status },
    unitValue, isAutoCarried
)

private fun DepositResponseDto.toDomain() = Deposit(
    id, cycleId, messId, userId, userFullName, roomNumber, amount, depositDate,
    paymentMethod = PaymentMethod.values().firstOrNull { it.name == paymentMethod } ?: PaymentMethod.CASH,
    transactionRef, status = DepositStatus.values().firstOrNull { it.name == status } ?: DepositStatus.PENDING,
    approvedByName, notes, createdAt
)

private fun ExpenseDtoRemote.toDomain() = Expense(
    id = id,
    category = ExpenseCategory.values().firstOrNull { it.name == category } ?: ExpenseCategory.MEAL_VARIABLE,
    title = title,
    amount = amount,
    expenseDate = expenseDate,
    receiptUrl = receiptUrl,
    recordedByName = recordedByName,
    notes = notes,
    targetMemberId = targetMemberId,
    targetMemberName = targetMemberName,
    participantIds = participantIds ?: emptyList()
)

private fun MealJoinRequestDtoRemote.toDomainManager() = ManagerJoinRequest(
    id = id,
    messId = mealId,
    messName = mealName,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    userPhone = userPhone,
    roomNumber = roomNumber,
    status = JoinRequestStatus.values().firstOrNull { it.name == status } ?: JoinRequestStatus.PENDING,
    requestNotes = requestNotes,
    createdAt = createdAt
)

private fun MenuItemDtoRemote.toDomain() = MenuItem(
    id, dayOfWeek,
    session = MealSession.values().firstOrNull { it.name == session } ?: MealSession.LUNCH,
    itemName, description, category, dietaryTags
)

private fun WeeklyMenuDtoRemote.toDomain() = WeeklyMenu(
    id, title, effectiveFrom,
    items = items.map { it.toDomain() },
    activeSessions = activeSessions.mapNotNull { s -> MealSession.values().firstOrNull { it.name == s } }
)

private fun MemberDtoRemote.toDomain() = MemberInfo(
    id = id,
    userId = userId,
    fullName = fullName ?: "Member",
    email = email ?: "",
    phone = phone,
    studentId = studentId,
    department = department,
    university = university,
    roomNumber = roomNumber,
    role = UserRole.values().firstOrNull { it.name == role } ?: UserRole.MEMBER,
    status = MembershipStatus.values().firstOrNull { it.name == status } ?: MembershipStatus.ACTIVE,
    joinDate = joinDate
)

private fun PastCycleSummaryDtoRemote.toDomain() = PastCycleSummary(
    id = id,
    cycleNumber = cycleNumber,
    startDate = startDate,
    endDate = endDate,
    targetActiveDays = targetActiveDays,
    totalExpenses = totalExpenses,
    totalCountedUnits = totalCountedUnits,
    mealRate = mealRate,
    status = CycleStatus.values().firstOrNull { it.name == status } ?: CycleStatus.COMPLETED
)

private fun DiningConfig.toDto() = DiningConfigDtoRemote(
    defaultCarryForward = defaultCarryForward,
    defaultBreakfastOn = defaultBreakfastOn,
    defaultLunchOn = defaultLunchOn,
    defaultDinnerOn = defaultDinnerOn,
    currency = currency,
    sessions = sessions.map {
        SessionConfigDtoRemote(
            session = it.session.name,
            unitValue = it.unitValue,
            cutoffTime = it.cutoffTime,
            isEnabled = it.isEnabled,
            servingStartTime = it.servingStartTime,
            servingEndTime = it.servingEndTime
        )
    }
)

private fun ChefSessionHeadcountDtoRemote.toDomain() = ChefSessionHeadcount(
    session = MealSession.values().firstOrNull { it.name == session } ?: MealSession.LUNCH,
    studentOnCount = studentOnCount,
    guestMealCount = guestMealCount,
    totalHeadcount = totalHeadcount,
    menuItemName = menuItemName,
    dietaryTags = dietaryTags,
    notes = notes,
    isEnabled = isEnabled,
    servingStartTime = servingStartTime,
    servingEndTime = servingEndTime
)

private fun ChefDailyHeadcountResponseDto.toDomain() = ChefDailyHeadcount(
    messId = messId,
    messName = messName,
    date = date,
    sessions = sessions.map { it.toDomain() },
    isPaused = isPaused,
    pauseReason = pauseReason
)

@Singleton
class ChefRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun getDailyHeadcount(messId: String, date: String?): ApiResult<ChefDailyHeadcount> {
        return try {
            api.getChefHeadcount(messId, date).toResult { it.toDomain() }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class NotificationRepository @Inject constructor(
    private val api: MessMateApiService
) {
    suspend fun registerDeviceToken(token: String): ApiResult<Unit> {
        return try {
            api.registerDeviceToken(com.messmate.android.data.remote.dto.DeviceTokenRequestDto(token)).toResult { }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to register notification token")
        }
    }

    suspend fun unregisterDeviceToken(token: String): ApiResult<Unit> {
        return try {
            api.unregisterDeviceToken(token).toResult { }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to unregister notification token")
        }
    }
}


