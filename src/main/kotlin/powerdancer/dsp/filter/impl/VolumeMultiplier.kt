package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import powerdancer.dsp.event.ConfigPush
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Float64PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

class VolumeMultiplier(var multipliers: DoubleArray = doubleArrayOf(1.0,1.0), val controlKey: String = "vol"): AbstractFilter() {

    override suspend fun onFloat64PcmData(data: Array<DoubleBuffer>): Flow<Event> {
        data.forEachIndexed { channel, b ->
            for (i in b.position() until b.limit()) {
                b.put(i, b.get(i) * multipliers[channel])
            }
        }

        return flowOf(Float64PcmData(data))
    }

    override suspend fun onConfigPush(key: String, value: String): Flow<Event> {
        if (key == "vol") {
            multipliers = value.split(",").map { it.toDoubleOrNull()?: 0.0 }.toDoubleArray()
        }
        return flowOf(ConfigPush(key, value))
    }

}