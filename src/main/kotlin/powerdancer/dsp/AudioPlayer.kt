package powerdancer.dsp

import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class AudioPlayer(val samplesInBuffer: Int): Worker {
    var output: SourceDataLine? = null
    var currentFormat: AudioFormat? = null

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        if (!format.equals(currentFormat)) {
            close()
            currentFormat = format

            output = AudioSystem.getSourceDataLine(format).apply {
                open(format, format.channels * ((format.sampleSizeInBits + 7) / 8) * samplesInBuffer)
                start()
            }
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