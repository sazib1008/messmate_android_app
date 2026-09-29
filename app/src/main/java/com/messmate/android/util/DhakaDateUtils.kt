package com.messmate.android.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * All date/time logic is routed through this object.
 * NEVER use default system clock, UTC, or LocalDate.now() directly.
 */
object DhakaDateUtils {

    val DHAKA_ZONE: ZoneId = ZoneId.of("Asia/Dhaka")

    /** The current date in Asia/Dhaka */
    fun today(): LocalDate = LocalDate.now(DHAKA_ZONE)

    /** The current date as ISO-8601 "YYYY-MM-DD" */
    fun todayDateString(): String = today().format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** The current time in Asia/Dhaka */
    fun nowTime(): LocalTime = LocalTime.now(DHAKA_ZONE)

    /** The current date+time in Asia/Dhaka */
    fun nowDateTime(): ZonedDateTime = ZonedDateTime.now(DHAKA_ZONE)

    /** 1 = Monday, ..., 7 = Sunday */
    fun todayDayOfWeekNumber(): Int = today().dayOfWeek.value

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val shortDayFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM")

    fun formatForApi(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun formatDisplay(date: LocalDate): String = date.format(displayFormatter)

    fun formatShortDay(date: LocalDate): String = date.format(shortDayFormatter)

    fun formatDhakaDateHeader(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr)
            formatDisplay(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun isToday(dateStr: String): Boolean {
        return try {
            LocalDate.parse(dateStr).isEqual(today())
        } catch (e: Exception) {
            false
        }
    }

    fun isPastDate(dateStr: String): Boolean {
        return try {
            LocalDate.parse(dateStr).isBefore(today())
        } catch (e: Exception) {
            false
        }
    }

    fun isMealCutoffPassed(cutoffTimeStr: String, dateStr: String): Boolean {
        return try {
            val targetDate = LocalDate.parse(dateStr)
            !isMealToggleAllowed(targetDate, cutoffTimeStr)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Given a cutoff time string "HH:mm" (Asia/Dhaka), returns true if we are BEFORE the cutoff.
     * If we are on a future date, always returns true (not yet past cutoff).
     * If we are on a past date, always returns false (already past).
     */
    fun isMealToggleAllowed(targetDate: LocalDate, cutoffTimeStr: String): Boolean {
        val todayDhaka = today()
        return when {
            targetDate.isAfter(todayDhaka) -> true
            targetDate.isBefore(todayDhaka) -> false
            else -> {
                try {
                    val parts = cutoffTimeStr.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts.getOrNull(1)?.toInt() ?: 0
                    val cutoff = LocalTime.of(hour, minute)
                    nowTime().isBefore(cutoff)
                } catch (e: Exception) {
                    true
                }
            }
        }
    }

    fun dateWithOffset(days: Int): String {
        return today().plusDays(days.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun addDays(dateIso: String, days: Long): String {
        return try {
            LocalDate.parse(dateIso).plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            dateIso
        }
    }

    fun formatTimeNow(): String {
        return nowTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    fun generateDateRange(startIso: String, endIso: String): List<String> {
        val start = LocalDate.parse(startIso)
        val end = LocalDate.parse(endIso)
        val dates = mutableListOf<String>()
        var cur = start
        while (!cur.isAfter(end)) {
            dates.add(cur.format(DateTimeFormatter.ISO_LOCAL_DATE))
            cur = cur.plusDays(1)
        }
        return dates
    }
}
