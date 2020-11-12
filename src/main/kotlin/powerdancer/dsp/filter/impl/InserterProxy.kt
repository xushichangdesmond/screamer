package powerdancer.dsp.filter.impl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import powerdancer.dsp.event.Event
import powerdancer.dsp.filter.Filter

// lets you insert events at this filter manually by calling the insert func (safe to call during processing run)
class InserterProxy: Filter {
    val pending = Channel<Flow<Event>>(Int.MAX_VALUE)

    override suspend fun filter(event: Event): Flow<Event> = flow {
        var p = pending.poll()
        while (p != null) {
            emitAll(p)
            p = pending.poll()
        }
        emit(event)
    }

    suspend fun insert(events: Flow<Event>) {
        pending.send(events)
    }
}