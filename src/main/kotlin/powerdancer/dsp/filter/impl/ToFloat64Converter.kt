package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import org.slf4j.LoggerFactory
import powerdancer.dsp.ObjectPool
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Float64PcmData
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

class ToFloat64Converter: AbstractFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(ToFloat64Converter::class.java)
    }

    val bufferPool = ObjectPool<DoubleBuffer>() { DoubleBuffer.allocate(200) }
    lateinit var format: AudioFormat
    lateinit var readSample: (ByteBuffer)->Double

    override suspend fun onFormatChange(format: AudioFormat): Flow<Event> {
        this.format = format
        readSample = when (format.sampleSizeInBits) {
            16-> this::read16BitSample
            24-> this::read24BitSample
            32-> this::read32BitSample
            else-> throw IllegalStateException("sample rate of ${format.sampleSizeInBits} is not supported")
        }
        return flowOf(FormatChange(
            AudioFormat(
                AudioFormat.Encoding.PCM_FLOAT,
                format.sampleRate,
                64,
                format.channels,
                64 * format.channels,
                format.sampleRate,
                false
            )
        ))
    }

    override suspend fun onPcmData(data: ByteBuffer): Flow<Event> {
        val output = Array<DoubleBuffer>(format.channels) {
            bufferPool.take()
        }
        val requiredSizePerOutputBuffer = data.remaining() * 8 / format.sampleSizeInBits / format.channels
        output.forEachIndexed { i, b ->
            if (b.capacity() < requiredSizePerOutputBuffer) {
                output[i] = DoubleBuffer.allocate(requiredSizePerOutputBuffer * 1.5.toInt())
            }
        }

        return flow {
            while (data.hasRemaining()) {
                for (i in 0 until format.channels) {
                    output[i].put(readSample(data))
                }
            }

            output.forEach { it.flip() }
            emit(Float64PcmData(output))
        }.onCompletion {
            output.forEach { bufferPool::put }
        }
    }

    private fun read16BitSample(data: ByteBuffer): Double {
        val impulse = (data.get().toInt().and(0xFF))
            .or(data.get().toInt().shl((8))).toShort()
        return impulse.toDouble() / Short.MAX_VALUE
    }

    private fun read24BitSample(data: ByteBuffer): Double {
        var impulse = (data.get().toInt().and(0xFF))
            .or(data.get().toInt().and(0xFF).shl(8))
            .or(data.get().toInt().and(0xFF).shl(16))
        if (impulse > 0x7FFFFF)
            impulse -= 0x1000000
        return impulse.toDouble() / (0x7FFFFF).toDouble()
    }

    private fun read32BitSample(data: ByteBuffer): Double {
        var impulse = (data.get().toInt().and(0xFF))
            .or(data.get().toInt().and(0xFF).shl(8))
            .or(data.get().toInt().and(0xFF).shl(16))
            .or(data.get().toInt().and(0xFF).shl(24))
        return impulse.toDouble() / (0x7FFFFFFF).toDouble()
    }
}