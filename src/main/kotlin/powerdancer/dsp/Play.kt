package powerdancer.dsp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import powerdancer.screamer.*
import java.io.File

class Play {
}

@ExperimentalStdlibApi
fun main() = runBlocking{

    val p1 = Processor.process(
        TcpAudioSource(6789),
//        DspImpulseLogger(LoggerFactory.getLogger("play")),
        SizedBuffer(20000),
        AudioPlayer(8192)
    )

//    delay(1000L)

    val p2 = Processor.process(
        ScreamerMulticastAudioSource(),
        ToDSPSignalConverter(),
//        Mix(
//            doubleArrayOf(0.0, 0.5)
//        ),
        FromDSPSignalConverter(16),
//        AudioPlayer(2048)
        TcpAudioSender("127.0.0.1", 6789)
    )

    p1.join()
    p2.join()
}