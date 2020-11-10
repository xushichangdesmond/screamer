package powerdancer.dsp.old

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

object Processor {
    val logger = LoggerFactory.getLogger(Processor::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.dsp.old.Processor"))

    fun process(vararg workers: Worker, bufferSize: Int = 100000, compactThreshold: Int = 32780) = scope.launch {
        val seedFormat = AudioFormat(
                48000F,
                16,
                2,
                false,
                false
        )
        val buf = ByteBuffer.allocate(bufferSize).limit(0)
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
            if (buf.capacity() - buf.position() < compactThreshold) {
                buf.compact().position(0).limit(0)
            }
        }
    }.apply {
        invokeOnCompletion { workers.forEach { it.close() } }
    }

}