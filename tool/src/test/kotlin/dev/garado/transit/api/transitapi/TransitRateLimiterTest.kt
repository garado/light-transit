package dev.garado.transit.api.transitapi

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransitRateLimiterTest {

    @AfterTest
    fun tearDown() {
        TransitRateLimiter.reset()
    }

    @Test
    fun `allows up to 5 calls within the window`() {
        repeat(5) {
            assertTrue(TransitRateLimiter.tryAcquire(nowMillis = 0L))
        }
    }

    @Test
    fun `blocks the 6th call within the same window`() {
        repeat(5) { TransitRateLimiter.tryAcquire(nowMillis = 0L) }
        assertFalse(TransitRateLimiter.tryAcquire(nowMillis = 0L))
    }

    @Test
    fun `allows calls again once the window has fully passed`() {
        repeat(5) { TransitRateLimiter.tryAcquire(nowMillis = 0L) }
        assertFalse(TransitRateLimiter.tryAcquire(nowMillis = 59_000L))
        assertTrue(TransitRateLimiter.tryAcquire(nowMillis = 60_000L))
    }

    @Test
    fun `old calls fall out of the window one at a time`() {
        repeat(5) { TransitRateLimiter.tryAcquire(nowMillis = 0L) }
        // The call at t=0 ages out at t=60_000, freeing exactly one slot.
        assertTrue(TransitRateLimiter.tryAcquire(nowMillis = 60_000L))
        assertFalse(TransitRateLimiter.tryAcquire(nowMillis = 60_000L))
    }
}
