package powerdancer.dsp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import powerdancer.screamer.Forker
import powerdancer.screamer.NettyAudioSender
import powerdancer.screamer.NettyAudioSource
import powerdancer.screamer.ScreamerMulticastAudioSource
import java.io.File

class Play {
}

@ExperimentalStdlibApi
fun main() = runBlocking{

    val p1 = Processor.process(
        NettyAudioSource(6789),
//        DspImpulseLogger(LoggerFactory.getLogger("play")),
        AudioPlayer(2048)
    )

//    delay(1000L)

    val p2 = Processor.process(
        ScreamerMulticastAudioSource(),
        ToDSPSignalConverter(),
        VolumeMultiplier(0.5),
        FromDSPSignalConverter(16),
//        AudioPlayer(2048)
        NettyAudioSender("127.0.0.1", 6789)
    )

    p1.join()
    p2.join()
}