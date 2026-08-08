package dev.garado.transit.navigation

import dev.garado.transit.api.models.TripLeg

/**
 * Transit's API has no text shorthand for color-named lines (e.g. BART's "Blue" line) — the
 * shortening they intend is a branded SVG icon, which we haven't built support for. As a stopgap,
 * scoped only to this navigation screen: an allowlist of known route names we're confident are
 * safe to abbreviate for a compact badge. Anything not on the list passes through unchanged.
 */
private val KNOWN_ROUTE_NAME_SHORTHANDS = mapOf(
    "red" to "R",
    "blue" to "B",
    "green" to "G",
    "orange" to "O",
    "yellow" to "Y",
    "purple" to "P",
)

/** Returns this leg with its route name shortened, if it's on the known-safe allowlist. */
fun TripLeg.shortened(): TripLeg = when (this) {
    is TripLeg.Walk -> this
    is TripLeg.Transit -> copy(routeName = KNOWN_ROUTE_NAME_SHORTHANDS[routeName.trim().lowercase()] ?: routeName)
}
