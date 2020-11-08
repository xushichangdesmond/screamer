package powerdancer.dsp

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamerMulticastAudioSource
import java.io.File

class Play {
}

@ExperimentalStdlibApi
fun main() = runBlocking{
    Processor.process(
//            AudioFileLoader(File("NR45.wav"), 256),
            ScreamerMulticastAudioSource(),
            ToDSPSignalConverter(),
            Forker(
//                arrayOf(DspImpulseLogger(LoggerFactory.getLogger("play"))),
                arrayOf(
                    HighPassFilter(500f),
                    FromDSPSignalConverter(16),
                    AudioPlayer(1024)
                )
            )
    ).join()
}