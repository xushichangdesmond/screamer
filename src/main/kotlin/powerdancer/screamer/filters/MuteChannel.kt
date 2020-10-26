package powerdancer.screamer.filters

import io.netty.buffer.ByteBuf
import powerdancer.screamer.Filter
import java.util.function.Consumer
import javax.sound.sampled.AudioFormat

class MuteChannel(val toMute: Int): Filter {
    override fun buildFilterFunc(inputAudioFormat: AudioFormat): Pair<AudioFormat, Consumer<ByteBuf>>? {
        if (toMute >= inputAudioFormat.channels) return null
        val numberOfBytesPerSample = (inputAudioFormat.sampleSizeInBits+7)/8
        val skipSize= (inputAudioFormat.channels - 1) * numberOfBytesPerSample
        return inputAudioFormat to
                Consumer { buf->
                    var i = buf.readerIndex() + toMute * numberOfBytesPerSample
                    val end = buf.writerIndex()
                    while (i < end) {
                        buf.writerIndex(i)
                        buf.writeZero(numberOfBytesPerSample)
                    }
                    buf.writerIndex(end)
                }
    }

}