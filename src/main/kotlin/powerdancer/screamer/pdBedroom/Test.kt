package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.Job
import powerdancer.dsp.old.*
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamerMulticastAudioSource

object Test {
    fun run(): Job {
        val cb = CBarrier(2)

        return Processor.process(
            ScreamerMulticastAudioSource(),
            ToDSPSignalConverter(),
            VolumeMultiplier(0.6),
            Forker(
                //right
                arrayOf(
                    Mix(
                        doubleArrayOf(-1.0, 1.0), // right channel to right speaker
                        doubleArrayOf(1.0, -1.0), // left channel to left speaker
//                        doubleArrayOf(1.0),
//                        doubleArrayOf(0.0,1.0)
                    ),
//                    DspImpulseLogger(LoggerFactory.getLogger("a")),
                    FromDSPSignalConverter(16),
                    AudioPlayer(2048)
                ),
                //left
//                arrayOf(
//                    Mix(
//
//                    ),
//                    FromDSPSignalConverter(32),
//                    cb,
//                    TcpAudioSender("192.168.1.89", 6789)
//                )
            )
        )
    }



}