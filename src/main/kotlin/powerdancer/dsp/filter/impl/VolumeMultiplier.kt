package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Float64PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

class VolumeMultiplier(val multiplier: Double): AbstractFilter() {

    override suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> {
        data.forEach { b ->
            for (i in b.position() until b.limit()) {
                b.put(i, b.get(i) * multiplier)
            }
        }

        return flowOf(Float64PcmData(data))
    }

}