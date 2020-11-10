package powerdancer.dsp.old

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

class AudioPlayer(val samplesInBuffer: Int, mixerName: String? = null): Worker {
    companion object {
        val logger = LoggerFactory.getLogger(AudioPlayer::class.java)
    }
    var output: SourceDataLine? = null
    var currentFormat: AudioFormat? = null
    val mixer: Mixer.Info? = mixerName?.let {
        AudioSystem.getMixerInfo().first {
            it.name == mixerName
        }
    }

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        if (!format.equals(currentFormat)) {
            close()

            currentFormat = format

            output = AudioSystem.getSourceDataLine(format, mixer).apply {
                open(format, format.channels * ((format.sampleSizeInBits + 7) / 8) * samplesInBuffer)
                start()
            }
            logger.info(currentFormat.toString())
        }
        output!!.write(buf.array(), buf.position(), buf.remaining())
        buf.position(buf.limit())
        return format
    }

    override fun close() {
        currentFormat = null
        output?.let {
            it.stop()
            it.close()
        }
        output = null
    }
}