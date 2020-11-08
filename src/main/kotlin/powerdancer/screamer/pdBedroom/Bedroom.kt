package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.*
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamerMulticastAudioSource
import powerdancer.screamer.TcpAudioSender

object Bedroom {
    fun run(): Job {
        val cb = CBarrier(2)
        return Processor.process(
            ScreamerMulticastAudioSource(),
            ToDSPSignalConverter(),
            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(0.0, 1.0),
                        doubleArrayOf(0.25, 0.25),
                    ),
                    FromDSPSignalConverter(16),
                    cb,
                    TcpAudioSender("192.168.1.91", 6789)
                ),
                //left
                arrayOf(
                    Mix(
                        doubleArrayOf(1.0),
                    ),
                    FromDSPSignalConverter(16),
                    cb,
                    TcpAudioSender("192.168.1.89", 6789)
                )
            )

        )
    }
}