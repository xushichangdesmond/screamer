package powerdancer.screamer

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.resources.LoopResources
import reactor.netty.tcp.TcpClient
import reactor.netty.tcp.TcpResources
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class ScreamerClient(host:String, port:Int): AutoCloseable {
    companion object {
        val logger = LoggerFactory.getLogger(ScreamerClient::class.java)
    }

    val client = TcpClient.create()
        .host(host)
        .port(port)
        .handle(this::handle)
        .connectNow()

    data class State(
        val buffer: ByteBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(3000),
        var line: SourceDataLine? = null,
        var encodedSampleRate: Byte = 0,
        var bitSize: Byte = 0
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
                            state.line = AudioSystem.getSourceDataLine(
                                audioFormat(
                                    decodeSampleRate(state.encodedSampleRate), state.bitSize.toInt()
                                )
                            )
                            state.line!!.open()
                            state.line!!.start()
                        } else if ((state.encodedSampleRate != msg[0]) || (state.bitSize != msg[1])) {
                            state.line!!.stop()
                            state.line!!.close()
                            state.encodedSampleRate = msg[0]
                            state.bitSize = msg[1]
                            state.line = AudioSystem.getSourceDataLine(
                                audioFormat(
                                    decodeSampleRate(state.encodedSampleRate), state.bitSize.toInt()
                                )
                            )
                            state.line!!.open()
                            state.line!!.start()
                        }
                        state.line!!.write(msg, 5, msg.size - 5)
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
                }
                .then()
                .doOnTerminate { state.line?.let { it.stop();it.close()}}
    }

    override fun close() {
        client.dispose()
    }

    fun decodeSampleRate(encodedSampleRate: Byte): Int {
        if (encodedSampleRate == (-127).toByte()) {
            return 44100
        } else if (encodedSampleRate == 2.toByte()) {
            return 96000
        }
        return 44100
    }

    fun audioFormat(sampleRate: Int, bitSize: Int): AudioFormat? {
        return AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(),
            bitSize,
            2,
            (bitSize + 7) / 8 * 2,
            sampleRate.toFloat(),
            false
        )
    }

}

fun main() {
    ScreamerClient("127.0.0.1", 6789)
    Thread.sleep(Long.MAX_VALUE)
}