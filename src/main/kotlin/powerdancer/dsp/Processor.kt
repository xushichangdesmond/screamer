package powerdancer.dsp

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat

object Processor {
    val logger = LoggerFactory.getLogger(Processor::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.dsp.Processor"))

    fun process(vararg workers: Worker) = scope.launch {
        val seedFormat = AudioFormat(
                48000F,
                16,
                2,
                false,
                false
        )
        val buf = ByteBuffer.allocate(100000).limit(0)
        while (true) {
            yield()
//            logger.info("next interation")
            sequenceOf(*workers)
                    .fold(
                            seedFormat
                    ) { f: AudioFormat, w: Worker ->
                        w.apply(f, buf)
                    }
            buf.position(buf.limit())
            if (buf.capacity() - buf.position() < 32768) {
                buf.compact().position(0).limit(0)
            }
        }
    }.apply {
        invokeOnCompletion { workers.forEach { it.close() } }
    }

}