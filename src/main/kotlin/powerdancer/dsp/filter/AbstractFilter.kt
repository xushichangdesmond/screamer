package powerdancer.dsp.filter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import powerdancer.dsp.event.*
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

abstract class AbstractFilter: Filter {

    final override suspend fun filter(event: Event): Flow<Event> {
        return when(event) {
            is Init -> onInit()
            is Bump -> onBump()
            is FormatChange -> onFormatChange(event.format)
            is PcmData -> onPcmData(event.data)
            is Float64PcmData -> onFloat64PcmData(event.data)
            is Close -> onClose()
            else -> onElse(event)
        }
    }

    open suspend fun onElse(event: Event): Flow<Event> = flowOf(event)

    open suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> = flowOf(Float64PcmData(data))

    open suspend fun onPcmData(data: ByteBuffer): Flow<Event> = flowOf(PcmData(data))

    open suspend fun onFormatChange(format: AudioFormat): Flow<Event> = flowOf(FormatChange(format))

    open suspend fun onBump(): Flow<Event> = flowOf(Bump)

    open suspend fun onInit(): Flow<Event> = flowOf(Init)

    open suspend fun onClose(): Flow<Event> = flowOf(Close)

}