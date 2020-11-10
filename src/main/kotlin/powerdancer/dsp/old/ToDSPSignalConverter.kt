package powerdancer.dsp.old

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

class ToDSPSignalConverter: Worker {
    companion object {
        val logger = LoggerFactory.getLogger(ToDSPSignalConverter::class.java)
    }

    var currentInputFormat: AudioFormat? = null
    var currentOutputFormat: AudioFormat? = null

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        assert(!format.isBigEndian)
//        logger.info("sampleRate {}, bitSize{}, channels{}, frameSize{}", format.sampleRate, format.sampleSizeInBits, format.channels, format.frameSize)
        if (!format.equals(currentInputFormat)) {
            currentInputFormat = format
            if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) {
                currentOutputFormat = AudioFormat(
                        AudioFormat.Encoding.PCM_FLOAT,
                        format.sampleRate,
                        64,
                        format.channels,
                        8 * format.channels,
                        format.sampleRate,
                        false
                )
            } else {
                return format
            }
        }
        if (format.sampleSizeInBits == 16) {
            val originalPosition = buf.position()
            val middleGround = ByteBuffer.allocate(buf.remaining() * 4)
            middleGround.order(ByteOrder.LITTLE_ENDIAN)
            val db = middleGround.asDoubleBuffer()

            while (buf.hasRemaining()) {
                val impulse = (buf.get().toInt().and(0xFF))
                    .or(buf.get().toInt().shl((8))).toShort()
                val converted = impulse.toDouble() / Short.MAX_VALUE
                db.put(
                    converted
                )
            }

            middleGround.position(0)
                    .limit(middleGround.capacity())
            buf.position(originalPosition)
                    .limit(originalPosition+middleGround.limit())
                    .put(middleGround)
                    .position(originalPosition)
            return currentOutputFormat!!
        } else if (format.sampleSizeInBits == 24) {
            val originalPosition = buf.position()
            val middleGround = ByteBuffer.allocate(buf.remaining() / 3 * 8)
            middleGround.order(ByteOrder.LITTLE_ENDIAN)
            val db = middleGround.asDoubleBuffer()

            while (buf.hasRemaining()) {
                var impulse = (buf.get().toInt().and(0xFF))
                            .or(buf.get().toInt().and(0xFF).shl(8))
                            .or(buf.get().toInt().and(0xFF).shl(16))
                if (impulse > 0x7FFFFF)
                    impulse -= 0x1000000
                val converted = impulse.toDouble() / (0x7FFFFF).toDouble()
                db.put(
                    converted
                )
            }

            middleGround.position(0)
                .limit(middleGround.capacity())
            buf.position(originalPosition)
                .limit(originalPosition+middleGround.limit())
                .put(middleGround)
                .position(originalPosition)
            return currentOutputFormat!!
        } else if (format.sampleSizeInBits == 32) {
            val originalPosition = buf.position()
            val middleGround = ByteBuffer.allocate(buf.remaining() * 2)
            middleGround.order(ByteOrder.LITTLE_ENDIAN)
            val db = middleGround.asDoubleBuffer()

            while (buf.hasRemaining()) {
                var impulse = (buf.get().toInt().and(0xFF))
                    .or(buf.get().toInt().and(0xFF).shl(8))
                    .or(buf.get().toInt().and(0xFF).shl(16))
                    .or(buf.get().toInt().and(0xFF).shl(24))
                val converted = impulse.toDouble() / (0x7FFFFFFF).toDouble()
                db.put(
                    converted
                )
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