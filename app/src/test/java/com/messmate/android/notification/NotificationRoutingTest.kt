package com.messmate.android.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRoutingTest {

    data class NotificationResolution(
        val channelId: String,
        val targetRoute: String?
    )

    private fun resolveRouting(data: Map<String, String>): NotificationResolution {
        val rawScreen = data["screen"] ?: data["route"] ?: data["target_route"]
        val eventType = data["type"] ?: "GENERAL"

        return when {
            rawScreen.equals("wallet", ignoreCase = true) -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_FINANCIAL, "student_home_wallet")
            }
            rawScreen.equals("deposits", ignoreCase = true) -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_FINANCIAL, "manager_home_deposits")
            }
            rawScreen.equals("meals", ignoreCase = true) || rawScreen.equals("dashboard", ignoreCase = true) -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_MEALS, "student_home_dashboard")
            }
            rawScreen.equals("join_requests", ignoreCase = true) -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_GENERAL, "manager_home_join_requests")
            }
            rawScreen.equals("chef", ignoreCase = true) -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_MEALS, "chef_terminal")
            }
            eventType == "DEPOSIT_APPROVED" || eventType == "DEPOSIT_REJECTED" -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_FINANCIAL, "student_home_wallet")
            }
            eventType == "NEW_JOIN_REQUEST" -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_GENERAL, "manager_home_join_requests")
            }
            eventType == "JOIN_REQUEST_APPROVED" -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_GENERAL, "student_home")
            }
            eventType == "MEAL_CUTOFF" || eventType == "CUTOFF_APPROACHING" -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_MEALS, "student_home_dashboard")
            }
            eventType == "CYCLE_CONCLUDED" || eventType == "NEW_CYCLE_STARTED" -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_GENERAL, "student_home_wallet")
            }
            !rawScreen.isNullOrBlank() -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_DEFAULT, rawScreen)
            }
            else -> {
                NotificationResolution(MessMateNotificationHelper.CHANNEL_DEFAULT, null)
            }
        }
    }

    @Test
    fun testWalletScreenRouting() {
        val resolution = resolveRouting(mapOf("screen" to "wallet"))
        assertEquals(MessMateNotificationHelper.CHANNEL_FINANCIAL, resolution.channelId)
        assertEquals("student_home_wallet", resolution.targetRoute)
    }

    @Test
    fun testDepositsScreenRouting() {
        val resolution = resolveRouting(mapOf("screen" to "deposits"))
        assertEquals(MessMateNotificationHelper.CHANNEL_FINANCIAL, resolution.channelId)
        assertEquals("manager_home_deposits", resolution.targetRoute)
    }

    @Test
    fun testMealCutoffEventTypeRouting() {
        val resolution = resolveRouting(mapOf("type" to "MEAL_CUTOFF"))
        assertEquals(MessMateNotificationHelper.CHANNEL_MEALS, resolution.channelId)
        assertEquals("student_home_dashboard", resolution.targetRoute)
    }

    @Test
    fun testJoinRequestEventTypeRouting() {
        val resolution = resolveRouting(mapOf("type" to "NEW_JOIN_REQUEST"))
        assertEquals(MessMateNotificationHelper.CHANNEL_GENERAL, resolution.channelId)
        assertEquals("manager_home_join_requests", resolution.targetRoute)
    }

    @Test
    fun testDepositApprovedEventTypeRouting() {
        val resolution = resolveRouting(mapOf("type" to "DEPOSIT_APPROVED"))
        assertEquals(MessMateNotificationHelper.CHANNEL_FINANCIAL, resolution.channelId)
        assertEquals("student_home_wallet", resolution.targetRoute)
    }

    @Test
    fun testDefaultFallbackRouting() {
        val resolution = resolveRouting(emptyMap())
        assertEquals(MessMateNotificationHelper.CHANNEL_DEFAULT, resolution.channelId)
        assertEquals(null, resolution.targetRoute)
    }
}
