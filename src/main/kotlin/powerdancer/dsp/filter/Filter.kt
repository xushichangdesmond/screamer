package powerdancer.dsp.filter

import kotlinx.coroutines.flow.Flow
import powerdancer.dsp.event.Event

interface Filter {
    suspend fun filter(event: Event): Flow<Event>
}