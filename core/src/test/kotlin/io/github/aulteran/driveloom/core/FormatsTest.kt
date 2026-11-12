package io.github.aulteran.driveloom.core

import io.github.aulteran.driveloom.core.api.DriveLoomApi
import io.github.aulteran.driveloom.core.api.Quantity
import io.github.aulteran.driveloom.core.format.Agents
import io.github.aulteran.driveloom.core.format.Dates
import io.github.aulteran.driveloom.core.format.Money
import io.github.aulteran.driveloom.core.format.Units
import io.github.aulteran.driveloom.core.fuel.ImportPayload
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class FormatsTest {
    @Test fun `quantities show the server value untouched`() {
        assertEquals("6.80 L/100 km", Units.quantity(Quantity("6.80", "l_per_100km")))
        assertEquals("12.0 furlong", Units.quantity(Quantity("12.0", "furlong")))
        assertEquals("—", Units.quantity(null))
    }

    @Test fun `money is formatted from the exact decimal`() {
        assertEquals("$1,234.50", Money.format("1234.50", "USD", Locale.US))
        assertEquals("$0.1234", Money.format("0.1234", "USD", Locale.US))
        // A value a double cannot hold exactly keeps every digit.
        assertEquals("$9,007,199,254,740,993.10", Money.format("9007199254740993.10", "USD", Locale.US))
        assertEquals("12 ???", Money.format("12", "???", Locale.US))
        assertEquals("—", Money.format(null, "USD", Locale.US))
    }

    @Test fun `the current currency is offered first and once`() {
        assertEquals("ISK", Units.currencyChoices("ISK").first())
        assertEquals(Units.currencies.size, Units.currencyChoices("EUR").size)
    }

    @Test fun `dates are shown in the device zone and bad input is passed through`() {
        val zone = ZoneId.of("Pacific/Auckland")
        assertEquals("Nov 12, 2026", Dates.date("2026-11-11T12:30:00Z", zone, Locale.US))
        assertEquals("not a date", Dates.date("not a date", zone, Locale.US))
        assertEquals("2026-11-02T00:00:00Z", Dates.daysAgo(10, Instant.parse("2026-11-12T00:00:00Z")))
    }

    @Test fun `user agents are summarised`() {
        assertEquals("DriveLoom app on Android", Agents.describe("DriveLoom-Android/0.1.0"))
        assertEquals("Firefox on Linux", Agents.describe("Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"))
        assertEquals("Unknown device", Agents.describe(""))
    }

    @Test fun `import files are classified without being interpreted`() {
        val csv = ImportPayload.from("log.csv", "text/csv", "occurred_at,odometer\n=1+1,2".toByteArray()) as ImportPayload.Companion.Result.Ok
        assertEquals("csv", csv.payload.format)
        assertEquals(JsonPrimitive("occurred_at,odometer\n=1+1,2"), csv.payload.data)
        val json = ImportPayload.from("car.json", "application/octet-stream", "﻿{\"format\":\"driveloom.vehicle\"}".toByteArray()) as ImportPayload.Companion.Result.Ok
        assertEquals("json", json.payload.format)
        assertTrue(json.payload.data is JsonObject)
        assertTrue(ImportPayload.from("car.json", "application/json", "{nope".toByteArray()) is ImportPayload.Companion.Result.Invalid)
        assertTrue(ImportPayload.from("car.json", "application/json", "[1]".toByteArray()) is ImportPayload.Companion.Result.Invalid)
        assertTrue(ImportPayload.from("x.csv", "text/csv", ByteArray(0)) is ImportPayload.Companion.Result.Invalid)
        assertTrue(ImportPayload.from("x.csv", "text/csv", "a,b".toByteArray(), requireJson = true) is ImportPayload.Companion.Result.Invalid)
        assertTrue(ImportPayload.from("x.csv", "text/csv", ByteArray(ImportPayload.MAX_BYTES + 1) { 'a'.code.toByte() }) is ImportPayload.Companion.Result.Invalid)
    }

    @Test fun `file names from the server cannot escape a folder`() {
        assertEquals("passwd", DriveLoomApi.safeFileName("../../etc/passwd", "x.csv"))
        assertEquals("golf-2026.csv", DriveLoomApi.safeFileName("golf-2026.csv", "x.csv"))
        assertEquals("x.csv", DriveLoomApi.safeFileName(null, "x.csv"))
        assertEquals("x.csv", DriveLoomApi.safeFileName("///", "x.csv"))
        assertEquals("hidden", DriveLoomApi.safeFileName("..hidden", "x.csv"))
    }
}
