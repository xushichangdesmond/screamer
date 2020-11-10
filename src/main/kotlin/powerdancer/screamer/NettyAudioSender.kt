package powerdancer.screamer

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.reactor.asFlux
import org.slf4j.LoggerFactory
import powerdancer.dsp.old.Worker
import reactor.core.publisher.Mono
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class NettyAudioSender(val host: String, val port: Int): Worker {
    companion object {
        val logger = LoggerFactory.getLogger(NettyAudioSender::class.java)
    }

    val scope = CoroutineScope(Dispatchers.Default + CoroutineName("NettyAudioSender"))

    val output = Channel<ByteBuf>(2048)
    val input = Channel<Pair<AudioFormat, ByteArray>>(2048)

    val client = TcpClient.create()
        .host(host)
        .port(port)
        .handle(this::handle)
//        .wiretap("sender", LogLevel.INFO)
        .connectNow()

    init {
        scope.launch {

            var currentFormat: AudioFormat? = null
            var encodedSampleRate: Byte = 0
            var bitSize: Byte = 0
            var channels: Byte = 0

            input.consumeEach {
                val format = it.first
                val msg = it.second

                if (!format.equals(currentFormat)) {
                    currentFormat = format
                    bitSize = format.sampleSizeInBits.toByte()
                    channels = format.channels.toByte()
                    encodedSampleRate = encodeSampleRate(format.sampleRate.toInt())
                }
                val payloadSize = msg.size + 5
                val b = PooledByteBufAllocator.DEFAULT.heapBuffer(payloadSize + 2)
                b.writeShort(payloadSize)
                    .writeByte(encodedSampleRate.toInt())
                    .writeByte(bitSize.toInt())
                    .writeByte(channels.toInt())
                    .writeZero(2)
                    .writeBytes(msg)
                output.sendBlocking(b)
            }
        }
    }

    override suspend fun apply(format: AudioFormat, b: ByteBuffer): AudioFormat {
        val msg = ByteArray(b.remaining())
        System.arraycopy(
            b.array(),
            b.position(),
            msg,
            0,
            b.remaining()
        )
        input.sendBlocking(format to msg)
        b.position(b.limit())
        return format
    }

    fun handle(inbound: NettyInbound, outbound: NettyOutbound): Mono<Void> {
        return outbound.send(
            output.receiveAsFlow()
                .asFlux()
                .doOnError {
                    logger.error("error sending", it)
                }
        ).then()
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun toHex(b: Byte): String {
        val hexChars = CharArray(2)
        val v = b.toInt()
        hexChars[0] = hexArray[v and 0xF0 shr 4]
        hexChars[1] = hexArray[v and 0x0F]
        return String(hexChars)
    }

    fun encodeSampleRate(sampleRate: Int): Byte {
        return (if (sampleRate == 44100) 0x81.toByte()
            else (sampleRate / 48000).toByte())
    }

    override fun close() {
        scope.cancel()
        client.dispose()
    }
}