package io.github.aulteran.driveloom.core

import io.github.aulteran.driveloom.core.api.FillUp
import io.github.aulteran.driveloom.core.api.FillUpCost
import io.github.aulteran.driveloom.core.api.FillUpDisplay
import io.github.aulteran.driveloom.core.api.Quantity
import io.github.aulteran.driveloom.core.fuel.DecimalInput
import io.github.aulteran.driveloom.core.fuel.FillUpDraft
import io.github.aulteran.driveloom.core.fuel.FillUpDraft.Field
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class FillUpDraftTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = LocalDateTime.of(2026, 11, 12, 10, 0)
    private fun draft() = FillUpDraft.blank("km", "l", "EUR", now).copy(odometer = "45210.5", volume = "41.37")
    private fun valid(d: FillUpDraft) = (d.validate("EUR", zone, now) as FillUpDraft.Result.Valid).body
    private fun errors(d: FillUpDraft) = (d.validate("EUR", zone, now) as FillUpDraft.Result.Invalid).errors

    @Test fun `decimals stay exact strings`() {
        assertEquals("42.375", DecimalInput.normalize(" 42.375 "))
        assertEquals("42.5", DecimalInput.normalize("42,5"))
        assertEquals("0.5", DecimalInput.normalize(".5"))
        assertEquals("7", DecimalInput.normalize("007"))
        assertEquals("12", DecimalInput.normalize("12."))
        assertEquals("0.10", DecimalInput.normalize("0.10"))
        // 0.1 + 0.2 style values must not be rewritten by a float round trip.
        assertEquals("123456789.123456789", DecimalInput.normalize("123456789.123456789"))
        for (bad in listOf("", "abc", "-1", "1.2.3", "1,2,3", "1e3", "+4", "١٢")) assertNull(bad, DecimalInput.normalize(bad))
    }

    @Test fun `a plain fill-up becomes the contract body`() {
        val body = valid(draft().copy(notes = "  motorway  "))
        assertEquals("2026-11-12T09:00:00Z", body["occurredAt"]!!.jsonPrimitive.content)
        assertEquals("45210.5", body["odometer"]!!.jsonObject["value"]!!.jsonPrimitive.content)
        assertTrue(body["odometer"]!!.jsonObject["value"]!!.jsonPrimitive.isString)
        assertEquals("km", body["odometer"]!!.jsonObject["unit"]!!.jsonPrimitive.content)
        assertEquals("41.37", body["volume"]!!.jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, body["cost"])
        assertEquals("motorway", body["notes"]!!.jsonPrimitive.content)
        assertEquals(setOf("occurredAt", "odometer", "volume", "full", "missedPrevious", "cost", "notes"), body.keys)
    }

    @Test fun `a cost in the vehicle currency carries no rate fields`() {
        val cost = valid(draft().copy(amount = "72,40"))["cost"]!!.jsonObject
        assertEquals("72.40", cost["amount"]!!.jsonPrimitive.content)
        assertEquals(setOf("amount", "currency"), cost.keys)
    }

    @Test fun `a foreign purchase needs a rate or a reporting amount`() {
        val foreign = draft().copy(amount = "50", costCurrency = "CHF")
        assertTrue(Field.EXCHANGE_RATE in errors(foreign))
        val withRate = valid(foreign.copy(exchangeRate = "1.05"))["cost"]!!.jsonObject
        assertEquals("1.05", withRate["exchangeRate"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, withRate["reportingAmount"])
        val withAmount = valid(foreign.copy(reportingAmount = "52.50"))["cost"]!!.jsonObject
        assertEquals(JsonNull, withAmount["exchangeRate"])
        assertEquals("52.50", withAmount["reportingAmount"]!!.jsonPrimitive.content)
        assertTrue(Field.EXCHANGE_RATE in errors(foreign.copy(exchangeRate = "0")))
    }

    @Test fun `bad input is reported per field`() {
        val e = errors(draft().copy(odometer = "", volume = "0", amount = "x", notes = "n".repeat(501), occurredAt = now.plusDays(3)))
        assertEquals(setOf(Field.ODOMETER, Field.VOLUME, Field.AMOUNT, Field.NOTES, Field.OCCURRED_AT), e.keys)
    }

    @Test fun `editing keeps the values exactly as entered`() {
        val stored = FillUp(
            id = "1", occurredAt = "2026-11-01T07:30:00Z",
            odometer = Quantity("28100.0", "mi"), volume = Quantity("10.500", "gal_us"),
            full = false, missedPrevious = true,
            cost = FillUpCost("40.00", "USD", "0.92", "36.80", "EUR"), notes = "trip",
            display = FillUpDisplay(Quantity("45222.6", "km"), Quantity("39.75", "l")),
        )
        val d = FillUpDraft.from(stored, "EUR", zone)
        assertEquals("28100.0", d.odometer)
        assertEquals("mi", d.odometerUnit)
        assertEquals("10.500", d.volume)
        assertEquals("gal_us", d.volumeUnit)
        assertEquals("0.92", d.exchangeRate)
        assertEquals("", d.reportingAmount)
        assertEquals(LocalDateTime.of(2026, 11, 1, 8, 30), d.occurredAt)
        assertEquals("2026-11-01T07:30:00Z", valid(d)["occurredAt"]!!.jsonPrimitive.content)
    }
}
