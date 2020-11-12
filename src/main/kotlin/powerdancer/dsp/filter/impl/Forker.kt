package powerdancer.dsp.filter.impl

import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Event
import powerdancer.dsp.filter.Filter

class Forker(vararg val pipelines: Array<Filter>): Filter {
    val logger = LoggerFactory.getLogger(Forker::class.java)

    override suspend fun filter(event: Event): Flow<Event> = pipelines.asFlow()
        .flatMapConcat { pipeline ->
            val eventCopy = event.clone()
            pipeline
                .fold(
                    flowOf(eventCopy.first)
                ) { accumulatedFlow: Flow<Event>, filter: Filter ->
                    accumulatedFlow.flatMapConcat { event ->
                        filter.filter(event)
                    }
                }.onCompletion {
                    eventCopy.second()
                }
        }

}