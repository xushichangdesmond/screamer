package powerdancer.screamer

import io.netty.buffer.ByteBuf
import java.util.function.Consumer
import javax.sound.sampled.AudioFormat

interface Filter {
    fun buildFilterFunc(inputAudioFormat: AudioFormat): Pair<AudioFormat, Consumer<ByteBuf>>?
}