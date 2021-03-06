package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.*
import powerdancer.screamer.*

object Karaoke {
    fun run(): Job {

        return Processor.process(
            ConfigurationFilter(repeatTo = arrayOf("http://192.168.1.91:6788/", "http://192.168.1.89:6788/")),
            ScreamMulticastAudioReceiver(),
            ToFloat64Converter(),
            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(0.5, -0.5),
                        doubleArrayOf(0.5, -0.5),
                    ),
                      LowPassFilter(80) { i -> i == 1 },
                    HighPassFilter(80) { i -> i == 0 },
                    VolumeMultiplier(doubleArrayOf(0.05, 0.2)),
                    FromFloat64Converter(4),
                    TcpAudioSender("192.168.1.91", 6789)
                ),
                //left
                arrayOf(
                    Mix(
                        doubleArrayOf(0.5, -0.5),
                    ),
                    HighPassFilter(80),
                    VolumeMultiplier(doubleArrayOf(0.05)),
                    FromFloat64Converter(4),
                    TcpAudioSender("192.168.1.89", 6789)
                )
            )

        )
    }
}

fun main()  = runBlocking{
    Karaoke.run().join()
}