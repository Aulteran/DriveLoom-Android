// Display helpers. The server already converts and rounds every figure, so
// these only label. Money goes through BigDecimal; nothing passes through
// floating point on its way to the screen.
package io.github.aulteran.driveloom.core.format

import io.github.aulteran.driveloom.core.api.Quantity
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object Units {
    private val labels = mapOf(
        "km" to "km", "mi" to "mi",
        "l" to "L", "gal_us" to "US gal", "gal_imp" to "imp gal",
        "l_per_100km" to "L/100 km", "km_per_l" to "km/L", "mpg_us" to "mpg (US)", "mpg_imp" to "mpg (imp)",
    )

    val distance = listOf("km" to "Kilometres", "mi" to "Miles")
    val volume = listOf("l" to "Litres", "gal_us" to "US gallons", "gal_imp" to "Imperial gallons")
    val economy = listOf("l_per_100km" to "L/100 km", "km_per_l" to "km/L", "mpg_us" to "mpg (US)", "mpg_imp" to "mpg (imperial)")
    val currencies = listOf("USD", "EUR", "GBP", "CAD", "AUD", "CHF", "JPY", "INR", "NZD", "SEK", "NOK", "DKK", "PLN", "CZK", "ZAR", "BRL", "MXN", "SGD", "HKD", "AED")

    fun label(unit: String): String = labels[unit] ?: unit
    fun quantity(q: Quantity?): String = if (q == null) "—" else "${q.value} ${label(q.unit)}"

    /** Offered currencies, with [current] first when it is not in the common list. */
    fun currencyChoices(current: String?): List<String> =
        (listOfNotNull(current?.takeIf { it.isNotBlank() }) + currencies).distinct()

    /** For every unit except L/100 km a larger figure means better economy. */
    fun higherIsBetter(economyUnit: String): Boolean = economyUnit != "l_per_100km"
}

object Labels {
    private val roles = mapOf("owner" to "Owner", "editor" to "Editor", "viewer" to "Viewer")
    private val reasons = mapOf(
        "partial_fill" to "Partial fill",
        "no_previous_full_fill" to "First full fill",
        "missed_fill" to "Missed purchase",
        "no_distance" to "No distance",
    )
    private val costReasons = mapOf("currency_mismatch" to "different currency", "no_cost_data" to "no cost data")

    fun role(role: String): String = roles[role] ?: role
    fun intervalReason(reason: String?): String = reasons[reason] ?: ""
    fun costReason(reason: String?): String = costReasons[reason] ?: "—"
}

object Money {
    /** Formats an exact decimal string as money, or falls back to "amount CODE". */
    fun format(amount: String?, currency: String, locale: Locale = Locale.getDefault()): String {
        if (amount == null) return "—"
        return try {
            val value = BigDecimal(amount)
            val format = NumberFormat.getCurrencyInstance(locale)
            val unit = Currency.getInstance(currency)
            format.currency = unit
            format.minimumFractionDigits = maxOf(0, unit.defaultFractionDigits)
            format.maximumFractionDigits = maxOf(4, unit.defaultFractionDigits)
            format.format(value)
        } catch (_: IllegalArgumentException) {
            "$amount $currency"
        }
    }
}

object Dates {
    fun parse(iso: String?): Instant? = try {
        if (iso.isNullOrBlank()) null else Instant.parse(iso)
    } catch (_: DateTimeParseException) {
        null
    }

    fun date(iso: String?, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        val instant = parse(iso) ?: return iso.orEmpty()
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(instant.atZone(zone))
    }

    fun dateTime(iso: String?, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        val instant = parse(iso) ?: return iso.orEmpty()
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).format(instant.atZone(zone))
    }

    fun date(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)

    fun time(time: LocalTime, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)

    /** A wall-clock date and time in [zone] as the UTC instant the API expects. */
    fun toInstantString(local: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ISO_INSTANT.format(local.atZone(zone).toInstant())

    fun toLocal(iso: String?, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime? = parse(iso)?.atZone(zone)?.toLocalDateTime()

    /** The start of a rolling window ending now, as an RFC 3339 instant. */
    fun daysAgo(days: Long, now: Instant = Instant.now()): String =
        DateTimeFormatter.ISO_INSTANT.format(now.minusSeconds(days * 86_400))
}

/** Short description of a session's user agent for the signed-in devices list. */
object Agents {
    fun describe(userAgent: String): String {
        if (userAgent.isBlank()) return "Unknown device"
        if (userAgent.startsWith("DriveLoom-Android")) return "DriveLoom app on Android"
        val browser = when {
            "Edg/" in userAgent -> "Edge"
            "Firefox/" in userAgent -> "Firefox"
            "Chrome/" in userAgent -> "Chrome"
            "Safari/" in userAgent -> "Safari"
            else -> "Browser"
        }
        val os = when {
            "Android" in userAgent -> "Android"
            "iPhone" in userAgent || "iPad" in userAgent -> "iOS"
            "Mac OS X" in userAgent -> "macOS"
            "Windows" in userAgent -> "Windows"
            "Linux" in userAgent -> "Linux"
            else -> ""
        }
        return if (os.isEmpty()) browser else "$browser on $os"
    }
}
