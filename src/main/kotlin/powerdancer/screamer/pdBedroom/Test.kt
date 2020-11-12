package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import powerdancer.dsp.*
import powerdancer.dsp.filter.impl.*
import powerdancer.screamer.ScreamMulticastAudioReceiver
import powerdancer.screamer.TcpAudioReceiver
import powerdancer.screamer.TcpAudioSender

object Test {
    fun run(): Job {
        return GlobalScope.launch {

            sequenceOf(
                Processor.process(
                    Channel(),
                    ScreamMulticastAudioReceiver(),
                    TcpAudioSender("127.0.0.1", 6789)
                ),

                Processor.process(
                    Channel(),
                    TcpAudioReceiver(6789),
                    ToFloat64Converter(),
                    FromFloat64Converter(2),
                    AudioPlayer(2048)
                )
            ).forEach { job -> job.join() }
        }
    }


}