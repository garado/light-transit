package dev.garado.transit.api

import dev.garado.transit.api.models.UserLocation
import kotlinx.coroutines.flow.Flow

/** Backend for obtaining user's current location */
interface UserLocationInterface {
    val location: Flow<UserLocation?>
}
