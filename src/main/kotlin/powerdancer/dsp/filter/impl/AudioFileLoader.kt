package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import powerdancer.dsp.ObjectPool
import powerdancer.dsp.event.Close
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class AudioFileLoader(file: File, samplesPerIteration: Int = 500): AbstractFilter() {

    val input: AudioInputStream = AudioSystem.getAudioInputStream(file)
    val buf =
        ByteBuffer
            .allocate(samplesPerIteration * input.format.channels * ((input.format.sampleSizeInBits + 7)/8))
            .order(if (input.format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)


    override suspend fun onInit(): Flow<Event> = flowOf(FormatChange(input.format))

    override suspend fun onBump(): Flow<Event> = flow {
        buf.clear()
        val read = input.read(buf.array())
        if (read == -1) {
            onClose()
            emit(Close)
            return@flow
        }
        buf.limit(read)

        emit(PcmData(buf))

    }


    override suspend fun onClose(): Flow<Event> {
        kotlin.runCatching { input.close() }
        return flowOf(Close)
    }
}