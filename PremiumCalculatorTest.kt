package com.premiabrygady.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumCalculatorTest {

    @Test
    fun podzial3500zlotych() {
        val result = PremiumCalculator.calculate(
            350000L,
            listOf(180L, 170L, 160L, 150L, 140L, 200L)
        )
        assertEquals(
            listOf(63000L, 59500L, 56000L, 52500L, 49000L, 70000L),
            result.amountsCents
        )
        assertEquals(350000L, result.amountsCents.sum())
    }

    @Test
    fun podzial100zlotychNaTrzech() {
        val result = PremiumCalculator.calculate(
            10000L,
            listOf(100L, 100L, 100L)
        )
        assertEquals(listOf(3334L, 3333L, 3333L), result.amountsCents)
        assertEquals(10000L, result.amountsCents.sum())
    }

    @Test
    fun zeroGodzinDajeZero() {
        val result = PremiumCalculator.calculate(10000L, listOf(0L, 100L))
        assertEquals(listOf(0L, 10000L), result.amountsCents)
    }

    @Test
    fun ujemneGodzinyJakZero() {
        val result = PremiumCalculator.calculate(10000L, listOf(-50L, 100L))
        assertEquals(listOf(0L, 10000L), result.amountsCents)
    }

    @Test
    fun sumaZawszeRownegoKwocie() {
        val result = PremiumCalculator.calculate(
            99999L,
            listOf(37L, 41L, 53L, 29L, 61L)
        )
        assertEquals(99999L, result.amountsCents.sum())
    }
}
