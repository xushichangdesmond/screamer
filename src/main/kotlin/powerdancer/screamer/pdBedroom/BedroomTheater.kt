package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.*
import powerdancer.screamer.ScreamMulticastAudioReceiver
import powerdancer.screamer.TcpAudioSender
import java.util.function.IntPredicate

object BedroomTheater {
    fun run(): Job {

        return Processor.process(
            ConfigurationFilter(
                repeatTo = arrayOf("http://192.168.1.91:6788/", "http://192.168.1.89:6788/")
            ),
            ScreamMulticastAudioReceiver(),
            ToFloat64Converter(),
            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(0.0, 1.0), // right channel to right speaker
                        doubleArrayOf(0.5, 0.5), // mix both channels to subwoofer
                    ),
                    LowPassFilter(125, IntPredicate { i -> i == 1 }),
                    HighPassFilter(125, IntPredicate { i -> i == 0 }),
                    VolumeMultiplier(doubleArrayOf(0.05, 0.4)),
                    FromFloat64Converter(4),
                    TcpAudioSender("192.168.1.91")
                ),
                //left
                arrayOf(
                    Mix(
                        doubleArrayOf(1.0), // left channel to left speaker
                    ),
                    HighPassFilter(125),
                    VolumeMultiplier(doubleArrayOf(0.05)),
                    FromFloat64Converter(4),
                    TcpAudioSender("192.168.1.89")
                ),
                //center
                arrayOf(
                    FromFloat64Converter(2),
                    VolumeMultiplier(doubleArrayOf(0.5, 0.5), "centerVol"),
                    AudioPlayer(
                        2048,
                        "Laser Proj (NVIDIA High Definition Audio)"
                    )
                )
            )

        )
    }
}