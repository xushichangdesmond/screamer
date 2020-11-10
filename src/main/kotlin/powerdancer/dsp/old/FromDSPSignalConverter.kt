package powerdancer.dsp.old

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

class FromDSPSignalConverter(val sampleSizeInBits: Int): Worker {
    companion object {
        val logger = LoggerFactory.getLogger(FromDSPSignalConverter::class.java)
    }

    var currentInputFormat: AudioFormat? = null
    var currentOutputFormat: AudioFormat? = null

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        assert(!format.isBigEndian)
        assert(buf.order() == ByteOrder.LITTLE_ENDIAN)

        if (!format.equals(currentInputFormat)) {
            currentInputFormat = format
            if (format.encoding == AudioFormat.Encoding.PCM_FLOAT) {
                currentOutputFormat = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.sampleRate,
                        sampleSizeInBits,
                        format.channels,
                        ((sampleSizeInBits + 7) / 8) * format.channels,
                        format.sampleRate,
                        false
                )
            } else {
                return format
            }
        }
        if (sampleSizeInBits == 16) {
            val originalPosition = buf.position()
            val middleGround = ByteBuffer.allocate(buf.remaining() / 4)
                .order(ByteOrder.LITTLE_ENDIAN)
            val db = buf.asDoubleBuffer()
            while (db.hasRemaining()) {
                val from = db.get()
                val v = (from * Short.MAX_VALUE).toInt()
                middleGround.put(v.toByte())
                        .put((v ushr 8).toByte())
            }
            middleGround.position(0)
                    .limit(middleGround.capacity())
            buf.position(originalPosition)
                    .limit(originalPosition+middleGround.limit())
                    .put(middleGround)
                    .position(originalPosition)
            return currentOutputFormat!!
        } else if (sampleSizeInBits == 24) {
            val originalPosition = buf.position()
            val middleGround = ByteBuffer.allocate(buf.remaining() / 8 * 3)
                .order(ByteOrder.LITTLE_ENDIAN)
            val db = buf.asDoubleBuffer()
            while (db.hasRemaining()) {
                val from = db.get()
                var v = (from * 0x7fffff.toDouble()).toInt()
                if (v < 0) v += 0x1000000
                middleGround.put(v.toByte())
                    .put((v ushr 8).toByte())
                    .put((v ushr 16).toByte())
            }
            middleGround.position(0)
                .limit(middleGround.capacity())
            buf.position(originalPosition)
                .limit(originalPosition+middleGround.limit())
                .put(middleGround)
                .position(originalPosition)
            return currentOutputFormat!!
        } else if (sampleSizeInBits == 32) {
            val originalPosition = buf.position()
            val middleGround = ByteBuffer.allocate(buf.remaining() / 2)
                .order(ByteOrder.LITTLE_ENDIAN)
            val db = buf.asDoubleBuffer()
            while (db.hasRemaining()) {
                val from = db.get()
                val v = (from * 0x7fffffff.toDouble()).toInt()
                middleGround.put(v.toByte())
                    .put((v ushr 8).toByte())
                    .put((v ushr 16).toByte())
                    .put((v ushr 24).toByte())
            }
            middleGround.position(0)
                .limit(middleGround.capacity())
            buf.position(originalPosition)
                .limit(originalPosition+middleGround.limit())
                .put(middleGround)
                .position(originalPosition)
            return currentOutputFormat!!
        } else {
            return format
        }

    }
}