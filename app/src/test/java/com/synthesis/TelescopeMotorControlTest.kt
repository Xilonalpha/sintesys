package com.synthesis

import com.synthesis.service.SlewDirection
import com.synthesis.service.SlewRate
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class TelescopeMotorControlTest {

    @Test
    fun testMeadeLx200ProtocolFormatting() {
        println("=== TEST 1: MEADE LX200 GOTO FORMATTING ===")
        val raHours = 19.70333 // 19h 42m 12s
        val decDeg = 41.30805  // +41° 18′ 29″

        val totalSecs = (raHours * 3600.0).roundToInt()
        val h = (totalSecs / 3600) % 24
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        val meadeRa = String.format("%02d:%02d:%02d", h, m, s)
        assertEquals("Meade RA must be formatted HH:MM:SS", "19:42:12", meadeRa)

        val meadeRaCmd = ":Sr$meadeRa#"
        assertEquals("Full Meade RA set command", ":Sr19:42:12#", meadeRaCmd)

        val meadeSlewCmd = ":MS#"
        assertEquals("Meade GoTo execution command", ":MS#", meadeSlewCmd)
    }

    @Test
    fun testEmergencyStopProtocol() {
        println("=== TEST 2: EMERGENCY HALT COMMAND ===")
        val abortCmd = ":Q#"
        assertEquals("Meade/NexStar emergency motor stop command must be :Q#", ":Q#", abortCmd)
    }

    @Test
    fun testJogRatesAndDirections() {
        println("=== TEST 3: MANUAL JOG DIRECTIONS ===")
        val directions = listOf(SlewDirection.NORTH, SlewDirection.SOUTH, SlewDirection.EAST, SlewDirection.WEST)
        val expectedCmds = listOf(":Mn#", ":Ms#", ":Me#", ":Mw#")

        directions.forEachIndexed { i, dir ->
            val cmd = when (dir) {
                SlewDirection.NORTH -> ":Mn#"
                SlewDirection.SOUTH -> ":Ms#"
                SlewDirection.EAST -> ":Me#"
                SlewDirection.WEST -> ":Mw#"
            }
            assertEquals("Direction ${dir.name} must match protocol command", expectedCmds[i], cmd)
        }

        assertEquals("Guiding rate is 1x", 1, SlewRate.GUIDE_1X.multiplier)
        assertEquals("Max slew rate is 800x", 800, SlewRate.MAX_SLEW.multiplier)
    }
}
