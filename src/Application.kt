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
import software.openmedrtc.Constants.PATH_USER_KEY
import software.openmedrtc.Constants.PATH_WEBSITE
import software.openmedrtc.Constants.PATH_WEBSOCKET
import software.openmedrtc.database.UserDatabase
import software.openmedrtc.database.entity.*
import software.openmedrtc.exception.ConnectionException
import software.openmedrtc.helper.Extensions.disconnectUser
import software.openmedrtc.helper.Extensions.mapMedicalsOnline
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
            ?: throw ConnectionException("Could not find principal")

        val connectedUser = UserDatabase.usersRegistered[principal.name]
            ?: throw ConnectionException("No registered user found with given credentials") // TODO Remove hardcoded string

        println("User connected to websocket: ${connectedUser.email}")
        session.send(Frame.Text("Successfully connected"))

        when (connectedUser) {
            is Medical -> {
                medChannels[connectedUser.email] =
                    Channel(MedicalConnectionSession(connectedUser, session), mutableListOf())
                println("Channel created")
            }
            is Patient -> {
                val param: String = session.call.parameters[PATH_USER_KEY]
                    ?: throw ConnectionException("Wrong parameter given with socket call")

                val channelToConnect =
                    medChannels[param] ?: throw ConnectionException("No channel found with given parameter")

                channelToConnect.patientSessions.add(PatientConnectionSession(connectedUser, session))
                println("Patient joined channel")
            }
            else -> throw ConnectionException("Wrong type") // TODO
        }

        handleWebsocketExchange(session, connectedUser)

    } catch (connectionException: ConnectionException) {
        connectionException.printStackTrace()
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
                session.send(Frame.Text("Client said: $message"))
            }
        }
    } catch (closedReceiveChannelException: ClosedReceiveChannelException) {
        println("Websocket close received")
    } catch (e: Throwable) {
        e.printStackTrace()
    } finally {
        medChannels.disconnectUser(connectedUser)
        println("User disconnected from websocket: ${connectedUser.email}")
    }
}
