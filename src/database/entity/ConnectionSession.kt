package software.openmedrtc.database.entity

import io.ktor.http.cio.websocket.WebSocketSession

data class PatientConnectionSession(val patient: Patient, val webSocketSession: WebSocketSession)
data class MedicalConnectionSession(val medical: Medical, val webSocketSession: WebSocketSession)
