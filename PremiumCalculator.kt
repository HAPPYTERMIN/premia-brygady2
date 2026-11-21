package com.premiabrygady.app

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Czysta logika dzielenia premii — bez zadnych zaleznosci od UI.
 *
 * Wzor: premia osoby = kwota x (godziny osoby / suma godzin wszystkich).
 * Liczenie w groszach na liczbach calkowitych (metoda najwiekszej reszty):
 * kazda osoba dostaje czesc calkowita, brakujace grosze trafiaja do osob
 * z najwieksza reszta. Suma wyplat ZAWSZE rowna sie dokladnie kwocie premii.
 * Osoba z 0 godzin dostaje 0 zl i nigdy dodatkowego grosza.
 * Ujemne godziny traktowane sa jak 0, puste pole jak 0.
 */
object PremiumCalculator {

    data class Result(val amountsCents: List<Long>, val totalHours: Long)

    fun calculate(amountCents: Long, hours: List<Long>): Result {
        val safeHours = hours.map { if (it < 0L) 0L else it }
        val total = safeHours.sum()
        if (amountCents <= 0L || total <= 0L) {
            return Result(List(safeHours.size) { 0L }, total)
        }
        val floors = LongArray(safeHours.size)
        val remainders = LongArray(safeHours.size)
        var distributed = 0L
        for (i in safeHours.indices) {
            if (safeHours[i] == 0L) continue
            val numerator = amountCents * safeHours[i]
            floors[i] = numerator / total
            remainders[i] = numerator % total
            distributed += floors[i]
        }
        var remaining = amountCents - distributed
        val order = safeHours.indices
            .filter { safeHours[it] > 0L }
            .sortedWith(compareByDescending<Long> { remainders[it] }.thenBy { it })
        var idx = 0
        while (remaining > 0L && order.isNotEmpty()) {
            floors[order[idx % order.size]] += 1L
            remaining -= 1L
            idx += 1
        }
        return Result(floors.toList(), total)
    }

    /** "1 234,50" / "1234.50" / "1234" -> grosze. Puste/ujemne/bledne -> 0. */
    fun parseAmountToCents(text: String): Long {
        val cleaned = text.trim().replace(" ", "").replace(",", ".")
        if (cleaned.isEmpty()) return 0L
        return try {
            val value = BigDecimal(cleaned)
            if (value <= BigDecimal.ZERO) 0L
            else value.multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    /** Godziny: przecinek/kropka, ujemne -> 0, puste -> 0. */
    fun parseHours(text: String): Long {
        val cleaned = text.trim().replace(" ", "").replace(",", ".")
        if (cleaned.isEmpty()) return 0L
        return try {
            val value = BigDecimal(cleaned)
            if (value <= BigDecimal.ZERO) 0L
            else value.setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    /** Polski format kwoty: 123450 groszy -> "1 234,50 zl" (bez ogonkow w kodzie). */
    fun formatPln(cents: Long): String {
        val sign = if (cents < 0L) "-" else ""
        val abs = kotlin.math.abs(cents)
        val zlote = abs / 100L
        val grosze = abs % 100L
        val zloteText = zlote.toString()
        val grouped = StringBuilder()
        for (i in zloteText.indices) {
            if (i > 0 && (zloteText.length - i) % 3 == 0) grouped.append(" ")
            grouped.append(zloteText[i])
        }
        val groszeText = if (grosze < 10L) "0" + grosze else grosze.toString()
        return sign + grouped.toString() + "," + groszeText + " z\u0142"
    }
}
