package com.example.pillpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderValidationTest {

    @Test
    fun validFutureDateReturnsTimestamp() {
        val result = ReminderValidation.parseFutureTriggerTime(
            date = "01/01/2099",
            time = "10:00",
            nowMillis = 0L
        )

        assertNotNull(result)
    }

    @Test
    fun invalidCalendarDateIsRejected() {
        val result = ReminderValidation.parseFutureTriggerTime(
            date = "31/02/2099",
            time = "10:00",
            nowMillis = 0L
        )

        assertNull(result)
    }

    @Test
    fun invalidTimeIsRejected() {
        val result = ReminderValidation.parseFutureTriggerTime(
            date = "01/01/2099",
            time = "25:70",
            nowMillis = 0L
        )

        assertNull(result)
    }

    @Test
    fun pastDateIsRejected() {
        val result = ReminderValidation.parseFutureTriggerTime(
            date = "01/01/2000",
            time = "10:00",
            nowMillis = System.currentTimeMillis()
        )

        assertNull(result)
    }

    @Test
    fun onceDailyRepeatsEvery24Hours() {
        assertEquals(
            86_400_000L,
            ReminderValidation.repeatIntervalMillis("Once Daily")
        )
    }

    @Test
    fun twiceDailyRepeatsEvery12Hours() {
        assertEquals(
            43_200_000L,
            ReminderValidation.repeatIntervalMillis("Twice Daily")
        )
    }

    @Test
    fun weeklyRepeatsEverySevenDays() {
        assertEquals(
            604_800_000L,
            ReminderValidation.repeatIntervalMillis("Weekly")
        )
    }

    @Test
    fun asNeededDoesNotRepeat() {
        assertNull(
            ReminderValidation.repeatIntervalMillis("As Needed")
        )
    }

    @Test
    fun unknownFrequencyDoesNotRepeat() {
        assertNull(
            ReminderValidation.repeatIntervalMillis("Unknown")
        )
    }
}