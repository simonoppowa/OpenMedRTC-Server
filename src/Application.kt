package software.openmedrtc

import com.google.gson.Gson
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
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import software.openmedrtc.Constants.AUTHENTICATION_KEY_BASIC
import software.openmedrtc.Constants.AUTHENTICATION_REALM_BASIC
import software.openmedrtc.Constants.PATH_REST
import software.openmedrtc.Constants.PATH_WEBSITE
import software.openmedrtc.Constants.PATH_WEBSOCKET
import software.openmedrtc.database.UserDatabase
import software.openmedrtc.database.entity.Channel
import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.MedicalConnectionSession
import software.openmedrtc.dto.MedicalDTO
import java.util.concurrent.ConcurrentHashMap

private val gson = Gson()
private val medChannels = ConcurrentHashMap<String, Channel>()

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
        get(PATH_WEBSITE) {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // REST
        authenticate(AUTHENTICATION_KEY_BASIC) {
            get(PATH_REST) {
                call.respond(mapMedicalsOnline())
            }
        }

        // Socket
        authenticate(AUTHENTICATION_KEY_BASIC) {
            webSocket(PATH_WEBSOCKET) {
                val principal: UserIdPrincipal? = call.authentication.principal()
                val connectedUser = UserDatabase.usersRegistered[principal?.name] ?: return@webSocket

                println("User connected to websocket: ${connectedUser.email}")
                send(Frame.Text("Successfully connected"))

                if (connectedUser is Medical) {
                    medChannels[connectedUser.email] =
                        Channel(MedicalConnectionSession(connectedUser, this), mutableListOf())
                    println("Channel created")
                }

                try {
                    while (true) {
                        val frame = incoming.receive()
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            println("Message received: $message")
                            send(Frame.Text("Client said: $message"))
                        }
                    }
                } catch (closedReceiveChannelException: ClosedReceiveChannelException) {
                    println("User disconnected from websocket: ${connectedUser.email}")
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }
        }
    }
}

private fun mapMedicalsOnline(): List<MedicalDTO> = medChannels.values.map { MedicalDTO(it.hostSession.medical) }

