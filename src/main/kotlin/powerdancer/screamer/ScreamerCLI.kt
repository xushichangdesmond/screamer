package powerdancer.screamer

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import powerdancer.screamer.filters.MuteChannel
import javax.sound.sampled.AudioSystem

class MyArgs(parser: ArgParser) {
    val listenPort: Int? by parser.storing(
        "-lp", "--listenPort",
        help = "run service daemon and listen on specified port"
    ) {toInt()}
        .default(null)

    val sourceHost: String? by parser.storing(
        "-sh", "--sourceHost",
        help = "stream audio from specified source host"
    ).default(null)

    val sourcePort: Int? by parser.storing(
        "sp", "--sourcePort",
        help = "stream audio from specified port on specified host"
    ) {toInt()}
        .default(null)

    val expectedMixerName: String? by parser.storing(
        "-m", "--mixer",
        help = "name of mixer to send audio to"
    ).default(null)

    val filters by parser.adding(
        "-f", "--filter",
        help = "name of filter to add"
    )

    val showMixers by parser.flagging(
        "--showMixers",
        help = "show mixes"
    ).default(false)
}

class ScreamerCLI {
    fun main(vararg args:String) {
        ArgParser(args).parseInto(::MyArgs).run {
            if (showMixers) {
                AudioSystem.getMixerInfo().forEach {
                    ScreamerClient.logger.info(it.name)
                }
            }
            if (listenPort != null) {
                ScreamerService(listenPort!!)
            } else if (sourceHost != null) {
                val filterList = filters.flatMap {
                    sequence<Filter> {
                        if (it.equals("muteLeft")) yield(MuteChannel(0))
                        else if (it.equals("muteRight")) yield(MuteChannel(1))
                    }
                }
                ScreamerClient(sourceHost!!, sourcePort!!, expectedMixerName, filterList.toTypedArray())
            }
            Unit
        }
    }

}

fun main(args:Array<String>) = mainBody {
    ScreamerCLI().main(*args)
    Thread.sleep(Long.MAX_VALUE)
}