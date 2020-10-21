package powerdancer.screamer

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.concurrent.atomic.AtomicBoolean

class ScreamerReceiver(
    port: Int
): AutoCloseable {

    companion object {
        val logger = LoggerFactory.getLogger(ScreamerReceiver::class.java)
    }

    val toStop = AtomicBoolean(false)

    val processor = EmitterProcessor.create<ByteBuf>(false)
    val processorSink = processor.sink(FluxSink.OverflowStrategy.LATEST)

    val server = TcpServer.create()
            .port(port)
            .handle(this::handle)
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

                val buf = getByteBuffer()
                val packet = DatagramPacket(buf.array(), 1157)
                screamSocket.receive(packet)
                processorSink.next(buf)
            }
        }
    }

    fun handle(inbound: NettyInbound, outbound: NettyOutbound): Mono<Void> {
        return outbound.send(processor.onBackpressureBuffer(
            10000,
            BufferOverflowStrategy.DROP_OLDEST
        )).then()
    }

    inline fun getByteBuffer(): ByteBuf {
        return PooledByteBufAllocator.DEFAULT.heapBuffer(3000)
    }

    override fun close() {
        toStop.set(true)
        processor.dispose()
        server.dispose()
        screamSocket.leaveGroup(screamAddress, null)
    }
}