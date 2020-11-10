package powerdancer.dsp.old

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class AudioFileLoader(file: File, samplesPerIteration: Int): Worker {

    val input = AudioSystem.getAudioInputStream(file)
    val bytesPerRead = samplesPerIteration * input.format.channels * ((input.format.sampleSizeInBits + 7)/8)

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        val read = input.readNBytes(buf.array(), buf.position(), bytesPerRead)
        buf.limit(buf.position() + read)
        if (input.format.isBigEndian) {
            buf.order(ByteOrder.BIG_ENDIAN)
        } else {
            buf.order(ByteOrder.LITTLE_ENDIAN)
        }
        return input.format
    }

    override fun close() {
        input.close()
    }
}