package dev.garado.transit.api.transitapi

/**
 * Singleton clientside throttle for the Transit API
 * 1500calls/month; 5calls/min
 */
object TransitRateLimiter {
    private const val MAX_CALLS_PER_WINDOW = 5
    private const val WINDOW_MILLIS = 60_000L

    private val callTimestamps = ArrayDeque<Long>()

    /** Returns true if a call is allowed right now, and records it if so */
    @Synchronized
    fun tryAcquire(nowMillis: Long = System.currentTimeMillis()): Boolean {
        while (callTimestamps.isNotEmpty() && nowMillis - callTimestamps.first() >= WINDOW_MILLIS) {
            callTimestamps.removeFirst()
        }
        if (callTimestamps.size >= MAX_CALLS_PER_WINDOW) return false
        callTimestamps.addLast(nowMillis)
        return true
    }

    /** Clears recorded calls */
    @Synchronized
    internal fun reset() {
        callTimestamps.clear()
    }
}
