package powerdancer.dsp

import org.slf4j.Logger
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class DspImpulseLogger(val logger: Logger): Worker {
    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        val originalPosition = buf.position()

        while(buf.hasRemaining()) {
            logger.info("impulse ${buf.getDouble()}")
        }

        buf.position(originalPosition)
        return format
    }
}