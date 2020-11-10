package powerdancer.dsp.old

import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class VolumeMultiplier(val multiplier: Double): Worker {

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        assert(format.encoding == AudioFormat.Encoding.PCM_FLOAT)
        assert(format.sampleSizeInBits == 64)

        val originalPosition = buf.position()
        var p = buf.position()
        while(buf.hasRemaining()) {
            buf.putDouble(p, buf.getDouble() * multiplier)
            p += 8
        }
        buf.position(originalPosition)
        return format
    }

}