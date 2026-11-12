package io.github.aulteran.driveloom.core.fuel

import io.github.aulteran.driveloom.core.api.FillUp
import io.github.aulteran.driveloom.core.format.Dates
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Decimal text as people type it, turned into the exact string form the API
 * accepts (`^(0|[1-9][0-9]*)(\.[0-9]+)?$`). The value is never parsed into a
 * floating-point number.
 */
object DecimalInput {
    private val pattern = Regex("^(0|[1-9][0-9]*)(\\.[0-9]+)?$")

    /** Returns the normalised decimal, or null when [text] is not a non-negative decimal. */
    fun normalize(text: String): String? {
        var s = text.trim().replace(" ", "")
        if (s.isEmpty()) return null
        // A single comma is a decimal separator on many keyboards.
        if (s.count { it == ',' } == 1 && '.' !in s) s = s.replace(',', '.')
        if (s.startsWith(".")) s = "0$s"
        if (s.endsWith(".")) s = s.dropLast(1)
        val whole = s.substringBefore('.')
        val trimmed = whole.trimStart('0').ifEmpty { "0" }
        s = if ('.' in s) "$trimmed.${s.substringAfter('.')}" else trimmed
        return s.takeIf { pattern.matches(it) }
    }

    fun isZero(decimal: String): Boolean = decimal.all { it == '0' || it == '.' }
}

/** The fill-up form: everything as entered, validated into a request body. */
data class FillUpDraft(
    val occurredAt: LocalDateTime,
    val odometer: String = "",
    val odometerUnit: String,
    val volume: String = "",
    val volumeUnit: String,
    val full: Boolean = true,
    val missedPrevious: Boolean = false,
    val amount: String = "",
    val costCurrency: String,
    val exchangeRate: String = "",
    val reportingAmount: String = "",
    val notes: String = "",
) {
    enum class Field { ODOMETER, VOLUME, AMOUNT, EXCHANGE_RATE, REPORTING_AMOUNT, NOTES, OCCURRED_AT }

    sealed interface Result {
        data class Valid(val body: JsonObject) : Result
        data class Invalid(val errors: Map<Field, String>) : Result
    }

    fun needsConversion(vehicleCurrency: String): Boolean = amount.isNotBlank() && costCurrency != vehicleCurrency

    fun validate(vehicleCurrency: String, zone: ZoneId = ZoneId.systemDefault(), now: LocalDateTime = LocalDateTime.now(zone)): Result {
        val errors = LinkedHashMap<Field, String>()
        val odo = DecimalInput.normalize(odometer)
        if (odo == null) errors[Field.ODOMETER] = "Enter the odometer reading as a number."
        val vol = DecimalInput.normalize(volume)
        if (vol == null) errors[Field.VOLUME] = "Enter the amount of fuel as a number."
        else if (DecimalInput.isZero(vol)) errors[Field.VOLUME] = "The amount of fuel must be more than zero."
        if (occurredAt.isAfter(now.plusDays(1))) errors[Field.OCCURRED_AT] = "The date is in the future."
        if (notes.length > NOTES_MAX) errors[Field.NOTES] = "Notes can be at most $NOTES_MAX characters."

        var cost: JsonObject? = null
        if (amount.isNotBlank()) {
            val amt = DecimalInput.normalize(amount)
            if (amt == null) errors[Field.AMOUNT] = "Enter the amount paid as a number, or leave it empty."
            val foreign = costCurrency != vehicleCurrency
            var rate: String? = null
            var reporting: String? = null
            if (foreign) {
                if (exchangeRate.isNotBlank()) {
                    rate = DecimalInput.normalize(exchangeRate)
                    if (rate == null || DecimalInput.isZero(rate)) errors[Field.EXCHANGE_RATE] = "Enter the exchange rate as a number above zero."
                }
                if (reportingAmount.isNotBlank()) {
                    reporting = DecimalInput.normalize(reportingAmount)
                    if (reporting == null) errors[Field.REPORTING_AMOUNT] = "Enter the amount in $vehicleCurrency as a number."
                }
                if (exchangeRate.isBlank() && reportingAmount.isBlank()) {
                    errors[Field.EXCHANGE_RATE] = "A purchase in another currency needs a rate or the amount in $vehicleCurrency. No rate is ever assumed."
                }
            }
            if (amt != null) {
                cost = buildJsonObject {
                    put("amount", amt)
                    put("currency", costCurrency)
                    if (foreign) {
                        put("exchangeRate", rate?.let(::JsonPrimitive) ?: JsonNull)
                        put("reportingAmount", reporting?.let(::JsonPrimitive) ?: JsonNull)
                    }
                }
            }
        }
        if (errors.isNotEmpty() || odo == null || vol == null) return Result.Invalid(errors)

        return Result.Valid(buildJsonObject {
            put("occurredAt", Dates.toInstantString(occurredAt, zone))
            putJsonObject("odometer") { put("value", odo); put("unit", odometerUnit) }
            putJsonObject("volume") { put("value", vol); put("unit", volumeUnit) }
            put("full", full)
            put("missedPrevious", missedPrevious)
            put("cost", cost ?: JsonNull)
            put("notes", notes.trim())
        })
    }

    companion object {
        const val NOTES_MAX = 500

        fun blank(odometerUnit: String, volumeUnit: String, currency: String, now: LocalDateTime): FillUpDraft =
            FillUpDraft(occurredAt = now.withSecond(0).withNano(0), odometerUnit = odometerUnit, volumeUnit = volumeUnit, costCurrency = currency)

        /** Pre-fills the form with a stored fill-up, using the values exactly as they were entered. */
        fun from(f: FillUp, vehicleCurrency: String, zone: ZoneId = ZoneId.systemDefault()): FillUpDraft {
            val cost = f.cost
            val foreign = cost != null && cost.currency != vehicleCurrency
            return FillUpDraft(
                occurredAt = Dates.toLocal(f.occurredAt, zone) ?: LocalDateTime.now(zone),
                odometer = f.odometer.value,
                odometerUnit = f.odometer.unit,
                volume = f.volume.value,
                volumeUnit = f.volume.unit,
                full = f.full,
                missedPrevious = f.missedPrevious,
                amount = cost?.amount.orEmpty(),
                costCurrency = cost?.currency ?: vehicleCurrency,
                exchangeRate = if (foreign) cost?.exchangeRate.orEmpty() else "",
                reportingAmount = if (foreign && cost?.exchangeRate == null) cost?.reportingAmount.orEmpty() else "",
                notes = f.notes,
            )
        }
    }
}
