package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.*
import powerdancer.screamer.*
import java.util.function.IntPredicate

object Karaoke {
    fun run(): Job {

        return Processor.process(
            Channel(),
            ConfigurationFilter(),
            ScreamMulticastAudioReceiver(),
            ToFloat64Converter(),
            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(-0.5, 0.5),
                        doubleArrayOf(0.1, 0.1),
                    ),
                    LowPassFilter(125) { i -> i == 1 },
                    HighPassFilter(125) { i -> i == 0 },
                    VolumeMultiplier(doubleArrayOf(0.05, 0.2)),
                    FromFloat64Converter(4),
                    TcpAudioSender("192.168.1.91", 6789)
                ),
                //left
                arrayOf(
                    Mix(
                        doubleArrayOf(0.5, -0.5),
                    ),
                    HighPassFilter(125),
                    VolumeMultiplier(doubleArrayOf(0.05)),
                    FromFloat64Converter(4),
                    TcpAudioSender("192.168.1.89", 6789)
                )
            )

        )
    }
}