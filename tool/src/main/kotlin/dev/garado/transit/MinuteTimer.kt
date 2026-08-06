package dev.garado.transit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MINUTE_TICK_MS = 60_000L

/** Process-wide ticking clock (updates once a minute), shared by anything that needs to show a live-updating time. */
object MinuteTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentTimeSeconds = MutableStateFlow(System.currentTimeMillis() / 1000)
    val currentTimeSeconds: StateFlow<Long> = _currentTimeSeconds.asStateFlow()

    init {
        scope.launch {
            while (true) {
                delay(MINUTE_TICK_MS)
                _currentTimeSeconds.value = System.currentTimeMillis() / 1000
            }
        }
    }
}
