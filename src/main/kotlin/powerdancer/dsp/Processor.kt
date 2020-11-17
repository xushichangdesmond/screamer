package powerdancer.dsp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Bump
import powerdancer.dsp.event.Close
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Init
import powerdancer.dsp.filter.Filter
import javax.sound.sampled.AudioFormat

object Processor {
    val logger = LoggerFactory.getLogger(Processor::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.dsp.Processor"))

    fun process(vararg filters: Filter) = scope.launch {
        filters.asFlow().fold(
            flow<Event> {
                emit(Init)
                while(true) {
                    emit(Bump)
                }
            }
        ) { accumulatedFlow: Flow<Event>, filter: Filter ->
            accumulatedFlow.flatMapConcat { event->
                filter.filter(event)
            }
        }.collect()

    }

}