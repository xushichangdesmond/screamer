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



//    delay(1000L)

    val cb = CBarrier(2)

    val p2 = Processor.process(
        ScreamerMulticastAudioSource(),
        ToDSPSignalConverter(),
        Forker(
            arrayOf(
                FromDSPSignalConverter(16),
                cb,
                DspImpulseLogger(LoggerFactory.getLogger("one"))
            ),
            arrayOf(
                Mix(
                    doubleArrayOf(0.5, 0.5)
                ),
                FromDSPSignalConverter(16),
                cb,
                DspImpulseLogger(LoggerFactory.getLogger("two"))
            )
        )
    )

    p2.join()
}