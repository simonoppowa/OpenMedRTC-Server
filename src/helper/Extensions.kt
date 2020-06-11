package software.openmedrtc.helper

import software.openmedrtc.database.entity.Channel
import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.Patient
import software.openmedrtc.database.entity.User
import software.openmedrtc.dto.MedicalDTO
import java.lang.NullPointerException
import java.util.concurrent.ConcurrentHashMap

object Extensions {
    fun ConcurrentHashMap<String, Channel>.mapMedicalsOnline(): List<MedicalDTO> =
        values.map { MedicalDTO(it.hostSession.medical) }

    fun ConcurrentHashMap<String, Channel>.disconnectUser(user: User) {
        if (user is Medical) {
            // Close channel
            try {
                this.remove(user.email)
            } catch (exception: NullPointerException) {
                println("No user to disconnect found")
            }
        } else if (user is Patient) {
            // Disconnect patient from channel
            for (channel in this.values) {
                if (channel.hasPatientConnected(user)) {
                    channel.patientSessions.removeAll { it.patient.email == user.email }
                }
            }
        }
    }
}
