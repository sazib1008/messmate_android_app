package com.messmate.android.data.remote

import com.messmate.android.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MessMateApiService {

    // ======================== AUTH ========================

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @GET("api/auth/me")
    suspend fun getMe(): Response<AuthResponseDto>

    // ======================== MESS ========================

    @POST("api/messes/verify-code")
    suspend fun verifyMessCode(@Body request: VerifyMealCodeRequestDto): Response<MealCodeVerificationResponseDto>

    @POST("api/messes/join-request")
    suspend fun submitJoinRequest(@Body request: SubmitJoinRequestDto): Response<MealJoinRequestDtoRemote>

    @GET("api/messes/my-join-request")
    suspend fun getMyJoinRequest(): Response<MealJoinRequestDtoRemote?>

    @DELETE("api/messes/join-requests/{requestId}")
    suspend fun cancelJoinRequest(@Path("requestId") requestId: String): Response<Map<String, String>>

    @GET("api/messes/{id}/config")
    suspend fun getDiningConfig(@Path("id") messId: String): Response<DiningConfigDtoRemote>

    // ======================== MEALS ========================

    @POST("api/meals/toggle")
    suspend fun toggleMeal(@Body request: ToggleMealRequestDto): Response<DailyMealStatusDtoRemote>

    @GET("api/meals/my-status")
    suspend fun getMyMealStatuses(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<DailyMealStatusDtoRemote>>

    // ======================== DEPOSITS ========================

    @POST("api/deposits")
    suspend fun submitDeposit(@Body request: CreateDepositRequestDto): Response<DepositResponseDto>

    @GET("api/deposits")
    suspend fun listDeposits(
        @Query("messId") messId: String? = null,
        @Query("userId") userId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<DepositResponseDto>>

    // ======================== CALCULATION ========================

    @GET("api/calculations/preview")
    suspend fun previewCalculation(@Query("messId") messId: String): Response<CycleCalculationResultDto>

    @GET("api/cycles/active")
    suspend fun getActiveCycle(@Query("messId") messId: String): Response<ActiveCycleDtoRemote>

    // ======================== EXPENSES ========================

    @GET("api/expenses")
    suspend fun listExpenses(@Query("messId") messId: String): Response<List<ExpenseDtoRemote>>

    // ======================== MENU ========================

    @GET("api/menus/today")
    suspend fun getTodayMenu(@Query("messId") messId: String): Response<List<MenuItemDtoRemote>>

    @GET("api/menus/weekly")
    suspend fun getWeeklyMenu(@Query("messId") messId: String): Response<WeeklyMenuDtoRemote>

    // ======================== PHASE 2 MANAGER API ========================

    // --- Expenses CRUD ---
    @POST("api/expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequestDto): Response<ExpenseDtoRemote>

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: String, @Body request: CreateExpenseRequestDto): Response<ExpenseDtoRemote>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Map<String, String>>

    // --- Deposits Review & CRUD ---
    @POST("api/deposits/{id}/approve")
    suspend fun approveDeposit(@Path("id") id: String): Response<DepositResponseDto>

    @POST("api/deposits/{id}/reject")
    suspend fun rejectDeposit(@Path("id") id: String, @Body request: ReviewDepositRequestDto? = null): Response<DepositResponseDto>

    @POST("api/deposits/{id}/review")
    suspend fun reviewDeposit(@Path("id") id: String, @Body request: ReviewDepositRequestDto): Response<DepositResponseDto>

    @PUT("api/deposits/{id}")
    suspend fun updateDeposit(@Path("id") id: String, @Body request: UpdateDepositRequestDto): Response<DepositResponseDto>

    @DELETE("api/deposits/{id}")
    suspend fun deleteDeposit(@Path("id") id: String): Response<Map<String, String>>

    // --- Members Roster ---
    @GET("api/messes/{id}/members")
    suspend fun getMembers(@Path("id") messId: String): Response<List<MemberDtoRemote>>

    @PUT("api/messes/{id}/members/{membershipId}/room")
    suspend fun updateMemberRoom(
        @Path("id") messId: String,
        @Path("membershipId") membershipId: String,
        @Body request: UpdateMemberRoomRequestDto
    ): Response<Map<String, Any?>>

    @PUT("api/messes/{id}/members/{membershipId}/role")
    suspend fun updateMemberRole(
        @Path("id") messId: String,
        @Path("membershipId") membershipId: String,
        @Body request: UpdateRoleRequestDto
    ): Response<Map<String, Any?>>

    @DELETE("api/messes/{id}/members/{membershipId}")
    suspend fun removeMember(
        @Path("id") messId: String,
        @Path("membershipId") membershipId: String
    ): Response<Map<String, Any?>>

    // --- Join Requests ---
    @GET("api/messes/{id}/join-requests")
    suspend fun listJoinRequests(@Path("id") messId: String): Response<List<MealJoinRequestDtoRemote>>

    @POST("api/messes/{id}/join-requests/{requestId}/review")
    suspend fun reviewJoinRequest(
        @Path("id") messId: String,
        @Path("requestId") requestId: String,
        @Body request: ReviewJoinRequestDto
    ): Response<MealJoinRequestDtoRemote>

    // --- Menu Planner CRUD ---
    @POST("api/menus/items")
    suspend fun createMenuItem(@Body request: CreateMenuItemRequestDto): Response<MenuItemDtoRemote>

    @DELETE("api/menus/items/{id}")
    suspend fun deleteMenuItem(@Path("id") id: String): Response<Map<String, String>>

    // --- Cycle & Settings ---
    @PUT("api/messes/{id}/config")
    suspend fun updateDiningConfig(
        @Path("id") messId: String,
        @Body request: DiningConfigDtoRemote
    ): Response<DiningConfigDtoRemote>

    @POST("api/cycles/pause-day")
    suspend fun pauseDay(@Body request: PauseDayRequestDto): Response<ActiveCycleDtoRemote>

    @POST("api/calculations/finalize")
    suspend fun finalizeCalculation(@Body request: FinalizeCycleRequestDto): Response<CycleCalculationResultDto>

    @POST("api/cycles/new")
    suspend fun startNewCycle(
        @Query("messId") messId: String,
        @Body request: StartNewCycleRequestDto
    ): Response<ActiveCycleDtoRemote>

    @GET("api/cycles/history")
    suspend fun getCycleHistory(@Query("messId") messId: String): Response<List<PastCycleSummaryDtoRemote>>

    // --- Chef Terminal ---
    @GET("api/chef/headcount")
    suspend fun getChefHeadcount(
        @Query("messId") messId: String,
        @Query("date") date: String?
    ): Response<ChefDailyHeadcountResponseDto>

    // --- Device Token / Push Notifications ---
    @POST("api/notifications/device-token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequestDto): Response<Map<String, String>>

    @DELETE("api/notifications/device-token")
    suspend fun unregisterDeviceToken(@Query("token") token: String): Response<Map<String, String>>
}
