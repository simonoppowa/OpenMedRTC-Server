package software.openmedrtc

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
import software.openmedrtc.Constants.MESSAGE_TYPE_ICE_CANDIDATE
import software.openmedrtc.Constants.MESSAGE_TYPE_SDP_ANSWER
import software.openmedrtc.Constants.MESSAGE_TYPE_SDP_OFFER
import software.openmedrtc.Constants.PATH_REST
import software.openmedrtc.Constants.PATH_USER_KEY
import software.openmedrtc.Constants.PATH_WEBSITE
import software.openmedrtc.Constants.PATH_WEBSOCKET
import software.openmedrtc.database.UserDatabase
import software.openmedrtc.database.entity.*
import software.openmedrtc.exception.SocketConnectionException
import software.openmedrtc.helper.Extensions.disconnectUser
import software.openmedrtc.helper.Extensions.mapMedicalsOnline
import java.lang.Exception
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
                call.respond(medChannels.mapMedicalsOnline())
            }
        }

        // Socket
        authenticate(AUTHENTICATION_KEY_BASIC) {
            webSocket("$PATH_WEBSOCKET/{$PATH_USER_KEY?}") {
                initWebsocketConnection(this)
            }
        }
    }
}

private suspend fun initWebsocketConnection(session: DefaultWebSocketServerSession) {
    try {
        val principal: UserIdPrincipal = session.call.authentication.principal()
            ?: throw SocketConnectionException("Could not find principal")

        val connectedUser = UserDatabase.usersRegistered[principal.name]
            ?: throw SocketConnectionException("No registered user found with given credentials") // TODO Remove hardcoded string

        println("User connected to websocket: ${connectedUser.email}")
        session.send(Frame.Text("Successfully connected"))

        when (connectedUser) {
            is Medical -> {
                medChannels[connectedUser.email] =
                    Channel(MedicalConnectionSession(connectedUser, session), mutableListOf()) // TODO make concurrent
                println("Channel created")
            }
            is Patient -> {
                val param: String = session.call.parameters[PATH_USER_KEY]
                    ?: throw SocketConnectionException("Wrong parameter given with socket call")

                val channelToConnect =
                    medChannels[param] ?: throw SocketConnectionException("No channel found with given parameter")

                channelToConnect.patientSessions.add(PatientConnectionSession(connectedUser, session))
                println("Patient joined channel")
            }
            else -> throw SocketConnectionException("Wrong type") // TODO
        }

        handleWebsocketExchange(session, connectedUser)

    } catch (socketConnectionException: SocketConnectionException) {
        socketConnectionException.printStackTrace()
        return
    }
}

private suspend fun handleWebsocketExchange(session: DefaultWebSocketServerSession, connectedUser: User) {
    try {
        while (true) {
            val frame = session.incoming.receive()
            if (frame is Frame.Text) {
                val message = frame.readText()
                println("Message received: $message")

                handleDataMessage(message, connectedUser)
            }
        }
    } catch (closedReceiveChannelException: ClosedReceiveChannelException) {
        println("Websocket close received")
    } catch (e: Throwable) {
        e.printStackTrace()
        session.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error while handling session"))
    } finally {
        medChannels.disconnectUser(connectedUser)
        println("User disconnected from websocket: ${connectedUser.email}")
    }
}

private suspend fun handleDataMessage(message: String, connectedUser: User) {
    try {
        val dataMessage = gson.fromJson(message, DataMessage::class.java) // TODO null fields
        when (dataMessage?.messageType) {
            MESSAGE_TYPE_SDP_OFFER, MESSAGE_TYPE_SDP_ANSWER, MESSAGE_TYPE_ICE_CANDIDATE -> {
                relayMessage(message, dataMessage, connectedUser)
            }
        }

    } catch (syntaxException: JsonSyntaxException) {
        println("Message has wrong type")
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

private suspend fun relayMessage(messageRaw: String, dataMessage: DataMessage, connectedUser: User) {
    when (connectedUser) {
        is Medical -> {
            // TODO
        }
        is Patient -> {
            val relayMessage: RelayMessage? = gson.fromJson(dataMessage.json, RelayMessage::class.java) // TODO null fields

            val channel = medChannels[relayMessage?.toUser]
            val medicalSession = channel?.hostSession
            val relayId = medicalSession?.medical?.email
            println("Relaying message to $relayId")

            medicalSession?.webSocketSession?.send(Frame.Text(messageRaw))
        }
    }
}
