package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.old.*
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamerMulticastAudioSource
import powerdancer.screamer.TcpAudioSender
import java.util.function.IntPredicate

object Karaoke {
    fun run(): Job {

        val cb = CBarrier(2)

        return Processor.process(
            ScreamerMulticastAudioSource(),
            ToDSPSignalConverter(),

            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(-0.5, 0.5),
                        doubleArrayOf(0.5, 0.5),
                    ),
                    LowPassFilter(125f, IntPredicate { i -> i == 1 }),
                    HighPassFilter(125f, IntPredicate { i -> i == 0 }),
                    FromDSPSignalConverter(32),
                    cb,
                    TcpAudioSender("192.168.1.91", 6789)
                ),
                //left
                arrayOf(
                    Mix(
                        doubleArrayOf(0.5, -0.5),
                    ),
                    HighPassFilter(125f),
                    FromDSPSignalConverter(32),
                    cb,
                    TcpAudioSender("192.168.1.89", 6789)
                )
            )

        )
    }
}