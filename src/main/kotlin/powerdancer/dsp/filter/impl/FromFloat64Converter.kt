package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import org.slf4j.LoggerFactory
import powerdancer.dsp.ObjectPool
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.lang.IllegalArgumentException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

class FromFloat64Converter(val sampleSizeInBytes: Int): AbstractFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(FromFloat64Converter::class.java)
    }

    val bufferPool = ObjectPool<ByteBuffer>() { ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN) }
    val writeSample: (Double, ByteBuffer)->Unit = when(sampleSizeInBytes) {
        2-> this::write16BitSample
        else-> throw IllegalArgumentException("sample size of $sampleSizeInBytes  bytes is not supported")
    }


    override suspend fun onFormatChange(format: AudioFormat): Flow<Event> = flowOf(
        FormatChange(AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            format.sampleRate,
            sampleSizeInBytes * 8,
            format.channels,
            sampleSizeInBytes * format.channels,
            format.sampleRate,
            false
        ))
    )

    override suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> {
        val output = with(bufferPool.take().clear()) {
            if (capacity() < data.size * data[0].remaining() * sampleSizeInBytes) {
                ByteBuffer.allocate(data.size * data[0].remaining() * sampleSizeInBytes * 1.5.toInt())
                    .order(ByteOrder.LITTLE_ENDIAN)
            } else this
        }

        return flow {
            for (j in 0 until data[0].remaining()) {
                for (element in data) {
                    writeSample(element.get(), output)
                }
            }
            emit(PcmData(output.flip()))

        }.onCompletion {
            bufferPool.put(output)
        }
    }

    private fun write16BitSample(d: Double, output: ByteBuffer) {
        val v = (d * Short.MAX_VALUE).toInt()
        output.put(v.toByte())
            .put((v ushr 8).toByte())
    }

    private fun write24BitSample(d: Double, output: ByteBuffer) {
        var v = (d * 0x7fffff.toDouble()).toInt()
        if (v < 0) v += 0x1000000
        output.put(v.toByte())
            .put((v ushr 8).toByte())
            .put((v ushr 16).toByte())
    }

    private fun write32BitSample(d: Double, output: ByteBuffer) {
        val v = (d * 0x7fffffff.toDouble()).toInt()
        output.put(v.toByte())
            .put((v ushr 8).toByte())
            .put((v ushr 16).toByte())
            .put((v ushr 24).toByte())
    }
}