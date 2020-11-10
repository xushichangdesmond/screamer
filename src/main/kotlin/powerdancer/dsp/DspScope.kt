package powerdancer.dsp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val DspScope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.dsp.Scope"))