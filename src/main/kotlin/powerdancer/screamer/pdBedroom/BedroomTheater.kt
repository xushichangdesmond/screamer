package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.old.*
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamerMulticastAudioSource
import powerdancer.screamer.TcpAudioSender
import java.util.function.IntPredicate

object BedroomTheater {
    fun run(): Job {

        val cb = CBarrier(3)

        return Processor.process(
            ScreamerMulticastAudioSource(),
            ToDSPSignalConverter(),
            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(0.0, 0.5), // right channel to right speaker
                        doubleArrayOf(0.2, 0.2), // mix both channels to subwoofer
                    ),
                    LowPassFilter(125f, IntPredicate { i -> i == 1 }),
                    HighPassFilter(125f, IntPredicate { i -> i == 0 }),
//                    DspImpulseLogger(LoggerFactory.getLogger("a")),
                    FromDSPSignalConverter(32),
                    cb,
                    TcpAudioSender("192.168.1.91", 6789)
                ),
                //left
                arrayOf(
                    Mix(
                        doubleArrayOf(0.5), // left channel to left speaker
                    ),
                    HighPassFilter(125f),
                    FromDSPSignalConverter(32),
                    cb,
                    TcpAudioSender("192.168.1.89", 6789)
                ),
                //center
                arrayOf(
                    Mix(
                        doubleArrayOf(1 / (Math.sqrt(2.0)) * 0.5, 1 / (Math.sqrt(2.0)) * 0.5) // center channel mix
                    ),
                    FromDSPSignalConverter(16),
                    cb,
                    AudioPlayer(
                        2048,
                        "Laser Proj (NVIDIA High Definition Audio)"
                    )
                )
            )

        )
    }
}