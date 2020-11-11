package powerdancer.dsp.filter

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import powerdancer.dsp.event.*
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat
import kotlin.coroutines.coroutineContext

abstract class AbstractTerminalFilter: Filter {
    final override suspend fun filter(event: Event): Flow<Event> {
        when(event) {
            is Init -> onInit()
            is Bump -> onBump()
            is FormatChange -> onFormatChange(event.format)
            is PcmData -> onPcmData(event.data)
            is Float64PcmData -> onFloat64PcmData(event.data)
            is Close -> {
                onClose()
                coroutineContext.cancel()
            }
            else -> onElse(event)
        }
        return emptyFlow()
    }

    open suspend fun onElse(event: Event) = Unit

    open suspend fun onFloat64PcmData(data: Array<DoubleBuffer>) = Unit

    open suspend fun onPcmData(data: ByteBuffer) = Unit

    open suspend fun onFormatChange(format: AudioFormat) = Unit

    open suspend fun onBump() = Unit

    open suspend fun onInit() = Unit

    open suspend fun onClose() = Unit
}