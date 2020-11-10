package powerdancer.dsp.old

import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

interface Worker: AutoCloseable {
    suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat

    override fun close() = Unit
}