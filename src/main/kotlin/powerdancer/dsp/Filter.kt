package powerdancer.dsp

import java.nio.ByteBuffer

interface Filter {
    suspend fun filter(b: Array<ByteBuffer>)
}