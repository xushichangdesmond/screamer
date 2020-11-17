package powerdancer.dsp.filter.impl

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import powerdancer.dsp.event.Close
import powerdancer.dsp.event.ConfigPush
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Init
import powerdancer.dsp.filter.Filter
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class ConfigurationFilter(val port: Int = 6788, vararg val repeatTo: String): Filter {
    val pending = Channel<ConfigPush>(10)
    val c = HttpClient.newHttpClient()

    lateinit var server: NettyApplicationEngine

    override suspend fun filter(event: Event): Flow<Event> = flow{
        var e = pending.poll()
        while (e != null) {
            emit(e)
            e = pending.poll()
        }
        when (event) {
            Init -> {
                server = embeddedServer(Netty, port) {
                    routing {
                        post("/") {
                            call.request.queryParameters.flattenForEach { k, v ->
                                CoroutineScope(coroutineContext).launch {
                                    pending.send(ConfigPush(k, v))
                                    repeatTo.forEach {
                                        launch {
                                            c.send(
                                                HttpRequest.newBuilder()
                                                    .uri(URI(it + "?$k=$v"))
                                                    .POST(HttpRequest.BodyPublishers.noBody())
                                                    .build(),
                                                HttpResponse.BodyHandlers.discarding()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.start()
            }
            Close-> {
                kotlin.runCatching { server.stop(5000, 5000) }
            }
        }
        emit(event)

    }
}