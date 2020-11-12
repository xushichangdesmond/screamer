package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import powerdancer.dsp.ObjectPool
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Float64PcmData
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.filter.AbstractFilter
import uk.me.berndporr.iirj.Butterworth
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.util.function.IntPredicate
import javax.sound.sampled.AudioFormat

class HighPassFilter(val frequency: Int, val channelPredicate: IntPredicate = IntPredicate {true}): AbstractFilter() {

    lateinit var channelFilters: Array<Pair<Int, (Double)->Double>>

    override suspend fun onFormatChange(format: AudioFormat): Flow<Event> {
        channelFilters = (0 until format.channels).map { i ->
            if (channelPredicate.test(i)) {
                val b = Butterworth().apply { highPass(4, format.sampleRate.toDouble(), frequency.toDouble()) }
                i to { v: Double -> b.filter(v) }
            } else null
        }
            .filterNotNull()
            .toTypedArray()
        return flowOf(FormatChange(format))
    }


    override suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> {
        channelFilters.forEach {p->
            val b = data[p.first]
            for(i in b.position() until b.limit()) {
                b.put(i, p.second(b.get(i)))
            }
        }
        return flowOf(Float64PcmData(data))
    }
}