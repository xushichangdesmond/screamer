package powerdancer.dsp

import org.slf4j.Logger
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class DspImpulseLogger(val logger: Logger): Worker {
    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        val originalPosition = buf.position()

        while(buf.hasRemaining()) {
            if (format.encoding == AudioFormat.Encoding.PCM_FLOAT)
                logger.info("double impulse ${buf.getDouble()}")
            else if (format.sampleSizeInBits == 16)
                logger.info("short impulse ${buf.getShort()}")
        }

        buf.position(originalPosition)
        return format
    }
}