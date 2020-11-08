package powerdancer.screamer

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import org.slf4j.LoggerFactory
import powerdancer.dsp.Worker
import reactor.core.publisher.Mono
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class NettyAudioSource(val port: Int): Worker {
    companion object {
        val logger = LoggerFactory.getLogger(NettyAudioSource::class.java)
    }

    val scope = CoroutineScope(Dispatchers.Default + CoroutineName("NettyAudioSource"))

    val server = TcpServer.create()
        .port(port)
        .handle(this::handle)
//        .wiretap("source", LogLevel.INFO)
        .bindNow()

    val input = Channel<ByteBuf>()
    val output = Channel<Pair<AudioFormat, ByteArray>>()

    init {
        scope.launch {
            val buf: ByteBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(20000)

            var currentFormat: AudioFormat? = null
            var encodedSampleRate: Byte = 0
            var bitSize: Byte = 0
            var channels: Byte = 0

            input.consumeEach {
                buf.writeBytes(it)
                it.release()
                var readable = buf.readableBytes()
                while (readable > 1) {
//                    println("readable" + readable)
                    val initialReadIndex = buf.readerIndex()
//                    println("initialReadIndex" + initialReadIndex)
                    val frameSize = buf.readShort().toInt()
//                    println("frameSize" + frameSize)
                    if (readable >= frameSize + 2) {
                        val msg = ByteArray(frameSize)
                        buf.readBytes(msg, 0, frameSize)
                        var newEncodedSampleRate = msg[0]
                        var newBitSize = msg[1]
                        var newChannels = msg[2]

                        if ((currentFormat == null) ||
                            (encodedSampleRate != newEncodedSampleRate) ||
                            (bitSize != newBitSize) ||
                            (channels != newChannels)
                        ) {
                            encodedSampleRate = newEncodedSampleRate
                            bitSize = newBitSize
                            channels = newChannels
                            currentFormat = audioFormat(decodeSampleRate(encodedSampleRate), bitSize.toInt(), channels.toInt())
                        }
                        output.sendBlocking(currentFormat!! to msg)
                    } else {
                        buf.readerIndex(initialReadIndex)
                        return@consumeEach
                    }
                    buf.discardReadBytes()
                    readable = buf.readableBytes()
                    if (readable < 2) return@consumeEach
                }
            }
        }
    }

    override suspend fun apply(format: AudioFormat, b: ByteBuffer): AudioFormat {
        val r = output.receive()
        val start = b.position()
        b.limit(start + r.second.size - 5)
//        println("${b.position()} ${b.limit()} ${r.second.size}")
        b.put(r.second, 5, r.second.size - 5)
        b.position(start)
        return r.first
    }

    fun handle(inbound: NettyInbound, outbound: NettyOutbound): Mono<Void> {
        return inbound.receive().doOnNext { newBuffer ->
            newBuffer.retain()
            GlobalScope.launch { input.sendBlocking(newBuffer) }
        }.then()
    }

    fun decodeSampleRate(encodedSampleRate: Byte): Int {
        return (encodedSampleRate.toInt() and 0x7f) * if (encodedSampleRate.toInt() and 0x80 == 0) 48000 else 44100
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

    override fun close() {
        scope.cancel()
        server.dispose()
    }
}