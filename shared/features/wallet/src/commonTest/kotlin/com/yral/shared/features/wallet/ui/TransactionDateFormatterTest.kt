package com.yral.shared.features.wallet.ui

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionDateFormatterTest {
    // region getOrdinalSuffix

    @Test
    fun `getOrdinalSuffix returns st for 1`() {
        assertEquals("st", getOrdinalSuffix(1))
    }

    @Test
    fun `getOrdinalSuffix returns nd for 2`() {
        assertEquals("nd", getOrdinalSuffix(2))
    }

    @Test
    fun `getOrdinalSuffix returns rd for 3`() {
        assertEquals("rd", getOrdinalSuffix(3))
    }

    @Test
    fun `getOrdinalSuffix returns th for 4`() {
        assertEquals("th", getOrdinalSuffix(4))
    }

    @Test
    fun `getOrdinalSuffix returns th for teens 11 through 13`() {
        assertEquals("th", getOrdinalSuffix(11))
        assertEquals("th", getOrdinalSuffix(12))
        assertEquals("th", getOrdinalSuffix(13))
    }

    @Test
    fun `getOrdinalSuffix returns st for 21`() {
        assertEquals("st", getOrdinalSuffix(21))
    }

    @Test
    fun `getOrdinalSuffix returns nd for 22`() {
        assertEquals("nd", getOrdinalSuffix(22))
    }

    @Test
    fun `getOrdinalSuffix returns rd for 23`() {
        assertEquals("rd", getOrdinalSuffix(23))
    }

    @Test
    fun `getOrdinalSuffix returns th for 30`() {
        assertEquals("th", getOrdinalSuffix(30))
    }

    // endregion

    // region formatTransactionDate

    @Test
    fun `formatTransactionDate formats ISO date correctly`() {
        val result = formatTransactionDate("2024-01-15T14:30:00Z", TimeZone.UTC)
        assertEquals("15th Jan 2024, 2:30 PM", result)
    }

    @Test
    fun `formatTransactionDate returns original string on invalid input`() {
        val input = "not-a-date"
        val result = formatTransactionDate(input, TimeZone.UTC)
        assertEquals(input, result)
    }

    @Test
    fun `formatTransactionDate handles midnight as 12 AM`() {
        val result = formatTransactionDate("2024-06-01T00:00:00Z", TimeZone.UTC)
        assertEquals("1st Jun 2024, 12:00 AM", result)
    }

    @Test
    fun `formatTransactionDate handles noon as 12 PM`() {
        val result = formatTransactionDate("2024-06-01T12:00:00Z", TimeZone.UTC)
        assertEquals("1st Jun 2024, 12:00 PM", result)
    }

    @Test
    fun `formatTransactionDate handles AM time`() {
        val result = formatTransactionDate("2024-03-22T09:05:00Z", TimeZone.UTC)
        assertEquals("22nd Mar 2024, 9:05 AM", result)
    }

    @Test
    fun `formatTransactionDate handles PM time`() {
        val result = formatTransactionDate("2024-12-03T18:45:00Z", TimeZone.UTC)
        assertEquals("3rd Dec 2024, 6:45 PM", result)
    }

    @Test
    fun `formatTransactionDate handles teen day with ordinal th`() {
        val result = formatTransactionDate("2024-07-11T10:00:00Z", TimeZone.UTC)
        assertEquals("11th Jul 2024, 10:00 AM", result)
    }

    // endregion
}
