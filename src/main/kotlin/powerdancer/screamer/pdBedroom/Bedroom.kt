package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.*
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamerMulticastAudioSource
import powerdancer.screamer.TcpAudioSender

object Bedroom {
    fun run(): Job = Processor.process(
        ScreamerMulticastAudioSource(),
        ToDSPSignalConverter(),
        Forker(
            arrayOf(
                Mix(
                    doubleArrayOf(0.5, 0.5),
                    doubleArrayOf(0.5, 0.5),
                ),
                FromDSPSignalConverter(24),
                TcpAudioSender("192.168.1.91", 6789)
            ),
            arrayOf(
                FromDSPSignalConverter(24),
                TcpAudioSender("192.168.1.89", 6789)
            )
        )

    )
}