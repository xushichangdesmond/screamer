package powerdancer.dsp

import java.nio.ByteBuffer
import java.util.concurrent.CyclicBarrier
import javax.sound.sampled.AudioFormat

class CBarrier(parties: Int) :Worker {
    val b = CyclicBarrier(parties)

    override suspend fun apply(format: AudioFormat, buf: ByteBuffer): AudioFormat {
        b.await()
        return format
    }
}