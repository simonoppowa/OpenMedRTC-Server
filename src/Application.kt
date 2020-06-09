package software.openmedrtc

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.features.*
import org.slf4j.event.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.auth.*
import io.ktor.gson.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    // Init Ktor Features
    install(CallLogging) {
        level = Level.DEBUG
        filter { call -> call.request.path().startsWith("/") }
    }
    install(Authentication) {
    }
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }


    routing {

        // Website
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // Socket
        webSocket("/myws/echo") {
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }

        // REST
        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

