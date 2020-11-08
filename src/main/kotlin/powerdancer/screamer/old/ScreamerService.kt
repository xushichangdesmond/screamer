package powerdancer.screamer.old

import com.xenomachina.argparser.mainBody
import io.netty.buffer.ByteBuf
import io.netty.buffer.UnpooledByteBufAllocator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import reactor.core.publisher.*
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.concurrent.atomic.AtomicBoolean

class ScreamerService(
    port: Int
): AutoCloseable {

    companion object {
        val logger = LoggerFactory.getLogger(ScreamerService::class.java)
    }

    val toStop = AtomicBoolean(false)

    val processor = EmitterProcessor.create<ByteBuf>(1,false)
    val processorSink = processor.sink(FluxSink.OverflowStrategy.LATEST)

    val server = TcpServer.create()
            .port(port)
            .handle(this::handle)
//            .wiretap("debug", LogLevel.INFO)
            .bindNow()

    val screamAddress = InetSocketAddress(InetAddress.getByName("239.255.77.77"), 4010)
    val screamSocket = MulticastSocket(4010).apply {
        joinGroup(screamAddress, null)
    }

    init {
        GlobalScope.launch {
            var iterations = 0
            while (!toStop.get()) {
                if (iterations++ == 100) {
                    iterations = 0
                    yield()
                }

                val buf = getByteBuffer().clear()
                val packet = DatagramPacket(buf.array(), 2, 1157)
                screamSocket.receive(packet)
                buf.writeShort(packet.length)
                buf.writerIndex(2 + packet.length)
                val downStreams = processor.downstreamCount()
                if (downStreams > 0L) {
                    if (downStreams > 1L)
                        buf.retain((downStreams - 1).toInt())
                    processorSink.next(buf)
                } else {
                    buf.release()
                }
            }
        }
    }

    fun handle(inbound: NettyInbound, outbound: NettyOutbound): Mono<Void> {
        return outbound.send(
            processor
                .onBackpressureBuffer(
                    100000,
                    BufferOverflowStrategy.DROP_OLDEST
                )
                .doOnError {
                    logger.error(it.message, it)
                }
        ).then()
    }

    inline fun getByteBuffer(): ByteBuf {
        return UnpooledByteBufAllocator.DEFAULT.heapBuffer(3000)
//        return ByteArray(3000)
    }

    override fun close() {
        toStop.set(true)
        processor.dispose()
        server.dispose()
        screamSocket.leaveGroup(screamAddress, null)
    }
}

fun main(args:Array<String>) = mainBody {
    ScreamerCLI().main("--lp=6789")
    Thread.sleep(Long.MAX_VALUE)
}