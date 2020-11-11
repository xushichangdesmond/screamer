package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
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

class AudioFileLoader(file: File, samplesPerIteration: Int = 200): AbstractFilter() {

    val input: AudioInputStream = AudioSystem.getAudioInputStream(file)
    val bufferPool = ObjectPool<ByteBuffer>(50) {
        ByteBuffer
            .allocate(samplesPerIteration * input.format.channels * ((input.format.sampleSizeInBits + 7)/8))
            .order(if (input.format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
    }

    override suspend fun onInit(): Flow<Event> = flowOf(FormatChange(input.format))

    override suspend fun onBump(): Flow<Event> {
        val buf = bufferPool.take().clear()

        val read = input.read(buf.array())
        if (read == -1) {
            onClose()
            return flowOf(Close)
        }
        buf.limit(read)

        return flowOf(PcmData(buf))
            .onCompletion {
                bufferPool.put(buf)
            }
    }

    override suspend fun onClose(): Flow<Event> {
        input.close()
        return flowOf(Close)
    }
}