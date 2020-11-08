package powerdancer.dsp

import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

object Processor {
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.dsp.Processor"))

    fun process(vararg workers: Worker) = scope.launch {
        val seedFormat = AudioFormat(
                48000F,
                16,
                2,
                false,
                false
        )
        val buf = ByteBuffer.allocate(1152000)
        while (true) {
            yield()
            sequenceOf(*workers)
                    .fold(
                            seedFormat
                    ) { f: AudioFormat, w: Worker ->
                        w.apply(f, buf)
                    }
            buf.position(buf.limit())
            if (buf.capacity() - buf.position() < 8192) buf.compact()
        }
    }.apply {
        invokeOnCompletion { workers.forEach { it.close() } }
    }

}