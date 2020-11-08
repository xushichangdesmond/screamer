package powerdancer.dsp

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import powerdancer.screamer.Forker
import java.io.File

class Play {
}

@ExperimentalStdlibApi
fun main() = runBlocking{
    Processor.process(
            AudioFileLoader(File("C:\\work\\TarsosDSP\\src\\tests\\be\\tarsos\\dsp\\test\\resources\\NR45.wav"), 256),
            ToDSPSignalConverter(),
            Forker(
//                arrayOf(DspImpulseLogger(LoggerFactory.getLogger("play"))),
                arrayOf(
                    HighPassFilter(1000f),
                    FromDSPSignalConverter(16),
                    AudioPlayer(1024)
                )
            )
    ).join()
}