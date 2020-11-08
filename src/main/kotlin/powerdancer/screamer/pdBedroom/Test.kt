package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import powerdancer.dsp.FromDSPSignalConverter
import powerdancer.dsp.Mix
import powerdancer.dsp.Processor
import powerdancer.dsp.ToDSPSignalConverter
import powerdancer.screamer.Forker
import powerdancer.screamer.ScreamReceiver
import powerdancer.screamer.ScreamerMulticastAudioSource
import powerdancer.screamer.TcpAudioSender

object Test {
    fun run(): Job = GlobalScope.launch {
        val p1 = ScreamReceiver.run()

        val p2 = Processor.process(
            ScreamerMulticastAudioSource(),
            ToDSPSignalConverter(),
            Forker(
                arrayOf(
                    Mix(
                        doubleArrayOf(0.5, 0.5),
                        doubleArrayOf(0.5, 0.5),
                    ),
                    FromDSPSignalConverter(16),
                    TcpAudioSender("127.0.0.1", 6789)
                )
            )
        )
        p1.join()
        p2.join()
    }



}