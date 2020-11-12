package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Float64PcmData
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.filter.AbstractFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

// final resultant impulse for channel N is
//    (multipliers[N][0] * incoming impulse for channel 0) +
//    (multipliers[N][1] * incoming impulse for channel 1) +
//    (multipliers[N][2] * incoming impulse for channel 2) +
// and so on. Any missing unmappable channels result in zero in above application
class Mix(vararg val multipliers: DoubleArray): AbstractFilter() {

    override suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> {
        val output = Array<DoubleBuffer>(multipliers.size) { i->
            Float64PcmData.takeBufferFromPool(data[i].remaining())
                .clear()
        }

        while (data[0].hasRemaining()) {
            val impulses = DoubleArray(data.size) { i->
                data[i].get()
            }
            multipliers.forEachIndexed { targetChannel, matrix ->
                output[targetChannel].put(
                    matrix.mapIndexed { srcChannel, multiple ->
                        if ((srcChannel > data.size) || (srcChannel > multipliers.size)) 0.toDouble()
                        else multiple * impulses[srcChannel]
                    }.sum()
                )
            }
        }
        output.forEach { it.flip() }

        return flowOf(Float64PcmData(output))
            .onCompletion {
                output.forEach { Float64PcmData.putBufferIntoPool(it) }
            }
    }


    override suspend fun onFormatChange(newFormat: AudioFormat) = flowOf(
        FormatChange(AudioFormat(
            AudioFormat.Encoding.PCM_FLOAT,
            newFormat.sampleRate,
            64,
            multipliers.size,
            8 * multipliers.size,
            newFormat.sampleRate,
            false
        ))
    )
}