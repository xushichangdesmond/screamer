package powerdancer.dsp.old

import uk.me.berndporr.iirj.Butterworth
import java.nio.ByteBuffer
import java.util.function.IntPredicate
import javax.sound.sampled.AudioFormat

class LowPassFilter(val frequency: Float, val channelPredicate: IntPredicate = IntPredicate {true}):
    Worker {
    var currentFormat: AudioFormat? = null
    var filters: Array<(Double)->Double> = arrayOf()

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        if (!format.equals(currentFormat)) {
            currentFormat = format
            filters = Array(format.channels) {i->
                if (channelPredicate.test(i)) {
                    val b = Butterworth().apply { lowPass(4, format.sampleRate.toDouble(), frequency.toDouble()) }
                    return@Array {v: Double->b.filter(v)}
                } else {
                    return@Array {v: Double->v}
                }
            }
        }

        val originalPosition = buf.position()
        var p = buf.position()
        while(buf.hasRemaining()) {
            for(i in 0 until format.channels) {
                buf.putDouble(p, filters[i](buf.getDouble()))
                p += 8
            }
        }
        buf.position(originalPosition)
        return format
    }
}