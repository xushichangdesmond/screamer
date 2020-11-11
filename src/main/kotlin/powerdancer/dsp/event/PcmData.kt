package powerdancer.dsp.event

import java.nio.ByteBuffer

data class PcmData (
    val data: ByteBuffer
): Event