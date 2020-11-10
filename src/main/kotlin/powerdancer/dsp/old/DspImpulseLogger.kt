package powerdancer.dsp.old

import org.slf4j.Logger
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class DspImpulseLogger(val logger: Logger): Worker {
    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        val originalPosition = buf.position()

        while(buf.hasRemaining()) {
            if (format.encoding == AudioFormat.Encoding.PCM_FLOAT && format.sampleSizeInBits == 64)
                logger.info("double impulse ${buf.getDouble()}")
            else if (format.encoding == AudioFormat.Encoding.PCM_FLOAT && format.sampleSizeInBits == 32)
                logger.info("float impulse ${buf.getFloat()}")
            else if (format.sampleSizeInBits == 16)
                logger.info("short impulse ${buf.getShort()}")
            else if (format.sampleSizeInBits == 32)
                logger.info("int impulse ${buf.getInt()}")
        }

        buf.position(originalPosition)
        return format
    }
}