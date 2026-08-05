package dev.garado.transit.api.transit

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
    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        while (callTimestamps.isNotEmpty() && now - callTimestamps.first() >= WINDOW_MILLIS) {
            callTimestamps.removeFirst()
        }
        if (callTimestamps.size >= MAX_CALLS_PER_WINDOW) return false
        callTimestamps.addLast(now)
        return true
    }
}
