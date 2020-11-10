package powerdancer.dsp.old

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

abstract class DspWorker: Worker {

    var currentFormat: AudioFormat? = null

    final override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        assert(!format.isBigEndian)
        assert(buf.order() == ByteOrder.LITTLE_ENDIAN)
        assert(format.encoding == AudioFormat.Encoding.PCM_FLOAT)
        assert(format.sampleSizeInBits == 64)

        if (!format.equals(currentFormat)) {
            currentFormat = format
            onFormatChange(format)
        }
        return doApply(format, buf)
    }

    abstract suspend fun doApply(format: AudioFormat, buf: ByteBuffer): AudioFormat

    open fun onFormatChange(newFormat: AudioFormat) = Unit
}