package powerdancer.screamer.old

import com.xenomachina.argparser.mainBody
import io.netty.buffer.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient
import reactor.util.retry.Retry
import java.lang.IllegalArgumentException
import java.util.function.Consumer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

class ScreamerClient(val host: String, val port: Int, expectedMixerName: String?, val filters: Array<Filter>): AutoCloseable {
    companion object {
        val logger = LoggerFactory.getLogger(ScreamerClient::class.java)
    }

    val mixer: Mixer.Info? = expectedMixerName?.let { mixerName->
        AudioSystem.getMixerInfo().firstOrNull {
            it.name.equals(mixerName)
        }
    }

    val handler = connect(host, port)
        .doOnSuccess {
            logger.info("connected")
        }
        .doOnError {
            logger.error("error", it)
        }
        .retryWhen(object : Retry() {
            override fun generateCompanion(retrySignals: Flux<RetrySignal>): Publisher<Long> {
                return retrySignals.map { 3000L }
            }
        })

    val runner = handler.subscribe()

    init {
        logger.info("specified mixer (null means default) - {}", mixer?.description)
    }

    fun connect(host: String, port: Int): Mono<out Connection> {
        return TcpClient.create()
            .host(host)
            .port(port)
            .handle(this::handle)
            .connect()
    }
    data class State(
        val buffer: ByteBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(3000),
        var line: SourceDataLine? = null,
        var encodedSampleRate: Byte = 0,
        var bitSize: Byte = 0,
        var channels: Byte = 0,
        var filterFunc: Consumer<ByteBuf> = Consumer{}
    )

    fun handle(inbound: NettyInbound, outbound: NettyOutbound): Mono<Void> {

        val state = State()
        return inbound.receive().reduce(state) { state, newBuffer ->
                state.buffer.writeBytes(newBuffer)

                var readable = state.buffer.readableBytes()
                while (readable > 1) {
                    val initialReadIndex = state.buffer.readerIndex()
                    val frameSize = state.buffer.readShort().toInt()
                    if (readable >= frameSize + 2) {
                        val msg = ByteArray(frameSize)
                        state.buffer.readBytes(msg, 0, frameSize)
                        if (state.line == null) {
                            state.encodedSampleRate = msg[0]
                            state.bitSize = msg[1]
                            state.channels = msg[2]
                            val built = buildFilterFunction(filters, audioFormat(
                                decodeSampleRate(state.encodedSampleRate),
                                state.bitSize.toInt(),
                                state.channels.toInt()
                            ))
                            state.filterFunc = built.second
                            try {
                                state.line = AudioSystem.getSourceDataLine(
                                    built.first,
                                    mixer
                                )
                                state.line!!.open()
                                state.line!!.start()
                            } catch (e:IllegalArgumentException) {
                                logger.error("no output audio device supporting this format", e)
                                state.line = null
                            }
                        } else if ((state.encodedSampleRate != msg[0]) || (state.bitSize != msg[1]) || (state.channels != msg[2])) {
                            state.line!!.stop()
                            state.line!!.close()
                            state.encodedSampleRate = msg[0]
                            state.bitSize = msg[1]
                            state.channels = msg[2]
                            val built = buildFilterFunction(filters, audioFormat(
                                decodeSampleRate(state.encodedSampleRate),
                                state.bitSize.toInt(),
                                state.channels.toInt()
                            ))
                            state.filterFunc = built.second
                            try {
                                state.line = AudioSystem.getSourceDataLine(
                                    built.first,
                                    mixer
                                )
                                state.line!!.open()
                                state.line!!.start()
                            } catch (e:IllegalArgumentException) {
                                logger.error("no output audio device supporting this format", e)
                                state.line = null
                            }
                        }
                        state.line?.let {
                            with (Unpooled.wrappedBuffer(msg)
                                .readerIndex(5)
                                .writerIndex(msg.size)) {
                                state.filterFunc.accept(this)
                                it.write(msg, readerIndex(), readableBytes())
                            }
                        }
                        state.buffer.discardReadBytes()
                        readable = state.buffer.readableBytes()
                    } else {
                        state.buffer.readerIndex(initialReadIndex)
                        break
                    }
                }
                state
        }
                .doOnError {
                    logger.error(it.message, it)
                    state.line?.let { it.stop();it.close()}
                    GlobalScope.launch {
                        delay(3000)
                        connect(host, port).subscribe()
                    }
                }
                .doOnSuccess { state.line?.let { it.stop();it.close()} }
                .then()

    }

    private fun buildFilterFunction(filters: Array<Filter>, audioFormat: AudioFormat): Pair<AudioFormat, Consumer<ByteBuf>> {
        var mappedAudioFormat = audioFormat
        var filterFunc = Consumer<ByteBuf> {  }
        filters.forEach {
            val built = it.buildFilterFunc(
                mappedAudioFormat
            )
            if (built != null) {
                mappedAudioFormat = built.first
                filterFunc = filterFunc.andThen(built.second)
            }
        }
        return mappedAudioFormat to filterFunc
    }

    override fun close() {
        runner.dispose()
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun toHex(b: Byte): String {
        val hexChars = CharArray(2)
        val v = b.toInt()
        hexChars[0] = hexArray[v and 0xF0 shr 4]
        hexChars[1] = hexArray[v and 0x0F]
        return String(hexChars)
    }

    fun decodeSampleRate(encodedSampleRate: Byte): Int {
        logger.info("encoded sample rate {} or hex {}", encodedSampleRate, toHex(encodedSampleRate))
        return (encodedSampleRate.toInt() and 0x4f) * if (encodedSampleRate.toInt() and 0x80 == 0) 48000 else 44100
    }

    fun audioFormat(sampleRate: Int, bitSize: Int, channels: Int): AudioFormat {
        return AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(),
            bitSize,
            channels,
            (bitSize + 7) / 8 * channels,
            sampleRate.toFloat(),
            false
        ).apply {
            logger.info("sampleRate {}, bitSize{}, channels{}", sampleRate, bitSize, channels)
        }
    }

}

fun main(args:Array<String>) = mainBody {
    ScreamerCLI().main("--sh=192.168.1.95", "--sp=6789" , "-fmuteRight", "-fmuteLeft")
//    ScreamerCLI().main("--sh=127.0.0.1", "--sp=6789", "-f=muteRight" )
    Thread.sleep(Long.MAX_VALUE)
}