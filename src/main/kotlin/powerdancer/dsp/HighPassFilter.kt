package powerdancer.dsp

import uk.me.berndporr.iirj.Butterworth
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class HighPassFilter(val frequency: Float): Worker {
    var currentFormat: AudioFormat? = null
    var filters: Array<Butterworth> = arrayOf()

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        if (!format.equals(currentFormat)) {
            currentFormat = format
            filters = Array(format.channels) {_->
                Butterworth().apply { highPass(4, format.sampleRate.toDouble(), frequency.toDouble()) }
            }
        }

        val originalPosition = buf.position()
        var p = buf.position()
        while(buf.hasRemaining()) {
            for(i in 0 until format.channels) {
                val v = buf.getDouble()
                val v2 = filters[i].filter(v)
//                println("$v $v2")
                buf.putDouble(p, v2)
                p += 8
            }
        }
        buf.position(originalPosition)
        return format
    }
}