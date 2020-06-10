package software.openmedrtc.helper

import software.openmedrtc.database.entity.Channel
import software.openmedrtc.dto.MedicalDTO
import java.util.concurrent.ConcurrentHashMap

object Extensions {
    fun ConcurrentHashMap<String, Channel>.mapMedicalsOnline(): List<MedicalDTO> =
        values.map { MedicalDTO(it.hostSession.medical) }

}
