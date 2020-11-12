package powerdancer.dsp.filter.impl

import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

class AudioPlayer(val samplesInBuffer: Int, mixerName: String? = null): AbstractTerminalFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(AudioPlayer::class.java)
    }

    val mixer: Mixer.Info? = mixerName?.let {
        AudioSystem.getMixerInfo().first {
            it.name == mixerName
        }
    }

    lateinit var output: SourceDataLine

    override suspend fun onFormatChange(format: AudioFormat) {
        output = AudioSystem.getSourceDataLine(format, mixer).apply {
            open(format, format.channels * ((format.sampleSizeInBits + 7) / 8) * samplesInBuffer)
            start()
        }
        logger.info(format.toString())
    }

    override suspend fun onPcmData(data: ByteBuffer) {
        output.write(data.array(), data.position(), data.remaining())
    }

    override suspend fun onClose() {
        runCatching {
            output.stop()
            output.close()
        }
    }
}