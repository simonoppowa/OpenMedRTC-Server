package software.openmedrtc

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
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
import software.openmedrtc.Constants.MESSAGE_TYPE_PATIENTS_LIST
import software.openmedrtc.Constants.MESSAGE_TYPE_SDP_ANSWER
import software.openmedrtc.Constants.MESSAGE_TYPE_SDP_OFFER
import software.openmedrtc.Constants.PATH_AUTH
import software.openmedrtc.Constants.PATH_REST
import software.openmedrtc.Constants.PATH_USER_KEY
import software.openmedrtc.Constants.PATH_WEBSITE
import software.openmedrtc.Constants.PATH_WEBSOCKET
import software.openmedrtc.database.UserDatabase
import software.openmedrtc.database.entity.*
import software.openmedrtc.dto.MedicalDTO
import software.openmedrtc.dto.PatientDTO
import software.openmedrtc.exception.ConnectionException
import software.openmedrtc.exception.SocketConnectionException
import software.openmedrtc.helper.AnnotatedDeserializer
import software.openmedrtc.helper.Extensions.mapMedicalsOnline
import java.lang.NullPointerException
import java.util.concurrent.ConcurrentHashMap

private val gson = GsonBuilder().registerTypeAdapter(DataMessage::class.java, AnnotatedDeserializer<DataMessage>())
    .registerTypeAdapter(RelayMessage::class.java, AnnotatedDeserializer<RelayMessage>()).create()
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
            // setPrettyPrinting()
            registerTypeAdapter(DataMessage::class.java, AnnotatedDeserializer<DataMessage>())
            registerTypeAdapter(RelayMessage::class.java, AnnotatedDeserializer<RelayMessage>())
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
            // call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // REST
        authenticate(AUTHENTICATION_KEY_BASIC) {
            get(PATH_AUTH) {
                try {
                    val userDTO = when (val connectedUser = getUserFromSession(this.call)) {
                        is Medical -> {
                            MedicalDTO(connectedUser)
                        }
                        is Patient -> {
                            PatientDTO(connectedUser)
                        }
                        else -> {
                            return@get
                        }
                    }
                    call.respond(userDTO)
                } catch (connectionException: ConnectionException) {
                    connectionException.printStackTrace()
                }
            }
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
        val connectedUser = getUserFromSession(session.call)
        var channel: Channel? = null

        println("User connected to websocket: ${connectedUser.email}")

        when (connectedUser) {
            is Medical -> {
                channel = Channel(MedicalConnectionSession(connectedUser, session), mutableListOf()) // TODO make concurrent
                medChannels[connectedUser.email] = channel
                println("Channel created")
            }
            is Patient -> {
                val param: String = session.call.parameters[PATH_USER_KEY]
                    ?: throw SocketConnectionException("Wrong parameter given with socket call")

                 channel =
                    medChannels[param] ?: throw SocketConnectionException("No channel found with given parameter")

                channel.patientSessions.add(PatientConnectionSession(connectedUser, session))
                println("Patient joined channel")
                notifyConnectionChanged(channel)
            }
            else -> throw SocketConnectionException("Wrong type") // TODO
        }

        handleWebsocketExchange(session, connectedUser, channel)

    } catch (socketConnectionException: SocketConnectionException) {
        socketConnectionException.printStackTrace()
        return
    }
}

private suspend fun notifyConnectionChanged(channel: Channel) {
    val patientList = channel.mapPatientsDTOOnline().toTypedArray()
    val patientListJson = gson.toJson(patientList, Array<PatientDTO>::class.java)
    val patientsDataMessage = DataMessage(MESSAGE_TYPE_PATIENTS_LIST, patientListJson)

    channel.hostSession.webSocketSession.send(Frame.Text(gson.toJson(patientsDataMessage, DataMessage::class.java)))
}

private suspend fun handleWebsocketExchange(
    session: DefaultWebSocketServerSession,
    connectedUser: User,
    channel: Channel
) {
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
        disconnectUser(connectedUser)
        notifyConnectionChanged(channel)
        println("User disconnected from channel: ${connectedUser.email}")
    }
}

private suspend fun handleDataMessage(message: String, connectedUser: User) {
    try {
        val dataMessage = gson.fromJson(message, DataMessage::class.java)
        when (dataMessage?.messageType) {
            MESSAGE_TYPE_SDP_OFFER, MESSAGE_TYPE_SDP_ANSWER, MESSAGE_TYPE_ICE_CANDIDATE -> {
                relayMessage(message, dataMessage, connectedUser)
            }
        }

    } catch (syntaxException: JsonSyntaxException) {
        println("Message has wrong type")
    } catch (parseException: JsonParseException) {
        println(parseException.localizedMessage)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

private suspend fun relayMessage(messageRaw: String, dataMessage: DataMessage, connectedUser: User) {
    val relayMessage: RelayMessage = gson.fromJson(dataMessage.json, RelayMessage::class.java)
        ?: throw JsonParseException("Relay message null")

    when (connectedUser) {
        is Medical -> {
            val channel = medChannels[connectedUser.email]
                ?: throw SocketConnectionException("No channel found to connected medical")
            val userSession =
                channel.patientSessions.firstOrNull { it.patient.email == relayMessage.toUser }
                    ?: throw SocketConnectionException("Patient to relay not connected")
            println("Relaying message to patient: ${userSession.patient.email}")

            userSession.webSocketSession.send(Frame.Text(messageRaw))
        }
        is Patient -> {
            val channel =
                medChannels[relayMessage.toUser] ?: throw SocketConnectionException("Medical to relay not connected")
            val medicalSession = channel.hostSession
            println("Relaying message to medical: ${medicalSession.medical.email}")

            medicalSession.webSocketSession.send(Frame.Text(messageRaw))
        }
    }
}

private fun getUserFromSession(call: ApplicationCall ): User {
    val principal: UserIdPrincipal = call.authentication.principal()
        ?: throw ConnectionException("Could not find principal")

    return UserDatabase.usersRegistered[principal.name]
        ?: throw ConnectionException("No registered user found with given credentials")
}

private fun disconnectUser(user: User) {
    if (user is Medical) {
        // Close channel
        try {
            medChannels.remove(user.email)
            println("Channel closed")
        } catch (exception: NullPointerException) {
            println("No user to disconnect found")
        }
    } else if (user is Patient) {
        // Disconnect patient from channel
        for (channel in medChannels.values) {
            if (channel.hasPatientConnected(user)) {
                channel.patientSessions.removeAll { it.patient.email == user.email }
            }
        }
    }
}
