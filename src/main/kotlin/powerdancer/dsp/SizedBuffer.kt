package powerdancer.dsp

import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class SizedBuffer(val threshold: Int): Worker {
    val buf = ByteBuffer.allocate(threshold * 2).limit(0)
    var currentFormat: AudioFormat = AudioFormat(
        44100F,
        16,
        1,
        true,
        false
    )

    override suspend fun apply(format: AudioFormat, b: ByteBuffer): AudioFormat {
        if (!format.equals(currentFormat)) {
            val initialBPos = b.position()
            val newBPos = b.limit()
            b.position(newBPos)
            val newLimit = b.position() + buf.position()
            b.limit(newLimit)
            buf.flip()
            b.put(buf)
                .position(initialBPos)
            buf.clear()
            b.limit(newBPos)
            buf.put(b)
            b.limit(newLimit)
            val oldFormat = currentFormat
            currentFormat = format
            return oldFormat
        }
        if (buf.position() > threshold) {
            val initialBPos = b.position()
            val newBPos = b.limit()
            b.position(newBPos)
            val newLimit = b.position() + buf.position()
            b.limit(newLimit)
            buf.flip()
            b.put(buf)
                .position(initialBPos)
            buf.clear()
            b.limit(newBPos)
            buf.put(b)
            b.limit(newLimit)
        } else {
            buf.put(b)
        }
        return format
    }
}