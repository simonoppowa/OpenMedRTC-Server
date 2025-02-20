package software.openmedrtc.helper

import software.openmedrtc.database.entity.Channel
import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.Patient
import software.openmedrtc.database.entity.User
import software.openmedrtc.dto.MedicalDTO
import java.lang.NullPointerException
import java.util.concurrent.ConcurrentHashMap

object Extensions {
    // TODO
    fun ConcurrentHashMap<String, Channel>.mapMedicalsOnline(): List<MedicalDTO> =
        values.map { MedicalDTO(it.hostSession.medical) }
}
