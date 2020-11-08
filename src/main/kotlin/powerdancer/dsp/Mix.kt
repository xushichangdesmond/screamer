package powerdancer.dsp

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

// final resultant impulse for channel N is
//    (multipliers[N][0] * incoming impulse for channel 0) +
//    (multipliers[N][1] * incoming impulse for channel 1) +
//    (multipliers[N][2] * incoming impulse for channel 2) +
// and so on. Any missing unmappable channels result in zero in above application
class Mix(vararg val multipliers: DoubleArray): DspWorker() {
    val temp = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)

    var outputFormat: AudioFormat? = null
    var inChannels: Int = 0

    override suspend fun doApply(format: AudioFormat, buf: ByteBuffer): AudioFormat {

        temp.clear()
        val originalPosition = buf.position()
        while(buf.hasRemaining()) {
            val impulses = DoubleArray(inChannels) {
                buf.getDouble()
            }
            for (arr in multipliers) {
                temp.putDouble(
                    arr.mapIndexed { index, d ->
                        if (index > inChannels) 0.toDouble()
                        else d * impulses[index]
                    }.sum()
                )
            }
        }

        temp.flip()
        buf.limit(originalPosition + temp.remaining())
        buf.position(originalPosition)
        buf.put(temp)

        buf.position(originalPosition)
        return outputFormat!!
    }

    override fun onFormatChange(newFormat: AudioFormat) {
        inChannels = newFormat.channels
        outputFormat = AudioFormat(
            AudioFormat.Encoding.PCM_FLOAT,
            newFormat.sampleRate,
            64,
            multipliers.size,
            8 * multipliers.size,
            newFormat.sampleRate,
            false
        )
    }
}