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
import software.openmedrtc.Constants.AUTHENTICATION_KEY_BASIC
import software.openmedrtc.Constants.AUTHENTICATION_REALM_BASIC
import software.openmedrtc.database.UserDatabase

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
        basic(name = AUTHENTICATION_KEY_BASIC) {
            realm = AUTHENTICATION_REALM_BASIC
            validate { credentials ->
                if(UserDatabase.usersRegistered[credentials.name]?.passwordHash == credentials.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
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

        // REST
        authenticate(AUTHENTICATION_KEY_BASIC) {
            get("/rest") {
                call.respond(mapOf("hello" to "world"))
            }
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

    }
}

