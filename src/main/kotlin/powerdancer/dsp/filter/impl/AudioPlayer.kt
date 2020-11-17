package powerdancer.dsp.filter.impl

import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

class AudioPlayer(val samplesInBuffer: Int, mixerName: String? = null, val configKey: String = "audioPlayer"): AbstractTerminalFilter() {
    companion object {
        val logger = LoggerFactory.getLogger(AudioPlayer::class.java)
    }

    val mixer: Mixer.Info? = mixerName?.let {
        AudioSystem.getMixerInfo().first {
            it.name == mixerName
        }
    }

    var output: SourceDataLine? = null
    var playing = true
    lateinit var format: AudioFormat

    override suspend fun onFormatChange(format: AudioFormat) {
        this.format = format

        if (output != null) {
            onClose()
        }
        output = AudioSystem.getSourceDataLine(format, mixer).apply {
            if (playing) {
                open(format, format.channels * ((format.sampleSizeInBits + 7) / 8) * samplesInBuffer)
                start()
            }
        }
        logger.info(format.toString())
    }

    override suspend fun onPcmData(data: ByteBuffer) {
        if (playing) {
            output!!.write(data.array(), data.position() + data.arrayOffset(), data.remaining())
        }
    }

    override suspend fun onClose() {
        runCatching {
            output!!.stop()
            output!!.close()
        }
    }

    override suspend fun onConfigPush(key: String, value: String) {
        if (key == configKey) {
            when(value) {
                "stop" -> {
                    playing = false
                    onClose()
                }
                "play" -> {
                    playing = true
                    output!!.open(format, format.channels * ((format.sampleSizeInBits + 7) / 8) * samplesInBuffer)
                    output!!.start()
                }
            }
        }
    }
}