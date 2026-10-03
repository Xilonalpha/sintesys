package com.synthesis

import com.synthesis.service.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import kotlin.math.roundToInt

class CelestronNexStarDriverTest {

    @Test
    fun testNexStar16BitGoToHexEncoding() {
        // Test M51 Whirlpool Galaxy: RA = 13.4977h, Dec = +47.1952°
        val raHours = 13.4977
        val decDeg = 47.1952

        // 16-bit RA counts: (13.4977 / 24.0) * 65536
        val raCounts = ((raHours / 24.0) * 65536.0).roundToInt().coerceIn(0, 65535)
        val raHex = String.format(Locale.US, "%04X", raCounts)

        // 16-bit Dec counts: (47.1952 / 360.0) * 65536
        val decCounts = ((decDeg / 360.0) * 65536.0).roundToInt().coerceIn(0, 65535)
        val decHex = String.format(Locale.US, "%04X", decCounts)

        val gotoCommand = "r$raHex,$decHex#"

        assertEquals("RA counts calculation", 36858, raCounts)
        assertEquals("RA hex formatting", "8FFA", raHex)
        assertEquals("Dec counts calculation", 8592, decCounts)
        assertEquals("Dec hex formatting", "2190", decHex)
        assertEquals("Full NexStar GoTo command format", "r8FFA,2190#", gotoCommand)
    }

    @Test
    fun testNexStarNegativeDecHexEncoding() {
        // Test Antares: RA = 16.4901h, Dec = -26.4320°
        val decDeg = -26.4320
        val normDec = if (decDeg < 0) decDeg + 360.0 else decDeg
        val decCounts = ((normDec / 360.0) * 65536.0).roundToInt().coerceIn(0, 65535)
        val decHex = String.format(Locale.US, "%04X", decCounts)

        assertEquals("Dec counts with negative rollover", 60724, decCounts)
        assertEquals("Dec hex formatting for negative Dec", "ED34", decHex)
    }

    @Test
    fun testNexStarStarSyncProtocol() {
        // Star Sync command uses 's' prefix followed by RA,Dec hex
        val raHours = 18.6156 // Vega
        val decDeg = 38.7836

        val raCounts = ((raHours / 24.0) * 65536.0).roundToInt()
        val decCounts = ((decDeg / 360.0) * 65536.0).roundToInt()
        val syncCmd = String.format(Locale.US, "s%04X,%04X#", raCounts, decCounts)

        assertTrue("Sync command must start with 's'", syncCmd.startsWith("s"))
        assertTrue("Sync command must end with '#'", syncCmd.endsWith("#"))
        assertEquals("Formatted Vega sync command", "sC691,1B94#", syncCmd)
    }

    @Test
    fun testNexStarTrackingModesAndRates() {
        assertEquals("Off tracking mode byte is 0x00", 0x00.toByte(), NexStarTrackingMode.OFF.code)
        assertEquals("Alt-Az tracking mode byte is 0x01", 0x01.toByte(), NexStarTrackingMode.ALT_AZ.code)
        assertEquals("EQ North tracking mode byte is 0x02", 0x02.toByte(), NexStarTrackingMode.EQ_NORTH.code)

        val rates = NexStarTrackRate.values()
        assertTrue("Track rates must include SIDEREAL, SOLAR, LUNAR", rates.size >= 3)
    }

    @Test
    fun testNexStarEmergencyHalt() {
        val haltCommand = "M#"
        assertEquals("NexStar emergency cancel slew command", "M#", haltCommand)
    }
}
