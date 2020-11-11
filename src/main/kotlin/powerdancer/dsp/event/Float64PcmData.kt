package powerdancer.dsp.event

import java.nio.DoubleBuffer

data class Float64PcmData(
    val data: Array<DoubleBuffer> // separate channels in separate buffers
): Event