package powerdancer.screamer


import kotlinx.coroutines.*
import powerdancer.dsp.Worker
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

class Forker(vararg val children: Array<Worker>): Worker {
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.screamer.Forker"))
    val buffers: Array<ByteBuffer> = Array(children.size) { _->
        ByteBuffer.allocate(100000).order(ByteOrder.LITTLE_ENDIAN)
    }

    override suspend fun apply(format: AudioFormat, src: ByteBuffer): AudioFormat {
        assert(src.order() == ByteOrder.LITTLE_ENDIAN)

        children.mapIndexed { i, pipeline->
            scope.launch {
                val copy = buffers[i]
                System.arraycopy(
                    src.array(),
                    src.position(),
                    copy.array(),
                    copy.position(),
                    src.remaining()
                )
                copy.limit(copy.position() + src.remaining())
                sequenceOf(*pipeline)
                        .fold(
                            format
                        ) { f: AudioFormat, w: Worker ->
                            w.apply(f, copy)
                        }
                copy.position(copy.limit())
                if (copy.capacity() - copy.position() < 32768) {
                    copy.compact().position(0).limit(0)
                }
            }
        }.joinAll()

        src.position(src.limit())
        return format
    }
}