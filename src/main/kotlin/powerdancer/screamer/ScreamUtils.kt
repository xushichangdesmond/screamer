package powerdancer.screamer

import org.slf4j.LoggerFactory
import javax.sound.sampled.AudioFormat

object ScreamUtils {
    val logger = LoggerFactory.getLogger(ScreamUtils::class.java)

    fun decodeSampleRate(encodedSampleRate: Byte): Int {
        return (encodedSampleRate.toInt() and 0x7f) * if (encodedSampleRate.toInt() and 0x80 == 0) 48000 else 44100
    }

    fun audioFormat(sampleRate: Int, bitSize: Int, channels: Int): AudioFormat {
        return AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(),
            bitSize,
            channels,
            (bitSize + 7) / 8 * channels,
            sampleRate.toFloat(),
            false
        ).apply {
            logger.info("sampleRate {}, bitSize{}, channels{}", sampleRate, bitSize, channels)
        }
    }

    fun encodeSampleRate(sampleRate: Int): Byte {
        return (if (sampleRate == 44100) 0x81.toByte()
        else (sampleRate / 48000).toByte())
    }
}