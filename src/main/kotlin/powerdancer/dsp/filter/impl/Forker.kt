package powerdancer.dsp.filter.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.cancel
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Bump
import powerdancer.dsp.event.Event
import powerdancer.dsp.filter.Filter

class Forker(vararg val pipelines: Array<Filter>): Filter {
    val logger = LoggerFactory.getLogger(Forker::class.java)
    val scope = CoroutineScope(Dispatchers.Default)

    override suspend fun filter(event: Event): Flow<Event> {
        return flow {
            pipelines.asFlow().map { pipeline->
                scope.launch {
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
                        }.collect()
                }
            }.toList().joinAll()
        }


    }

}