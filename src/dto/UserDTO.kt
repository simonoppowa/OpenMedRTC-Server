package software.openmedrtc.dto

import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.Patient

abstract class UserDTO(
    val id: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val profilePicUrl: String
)

class MedicalDTO(
    id: String,
    title: String,
    firstName: String,
    lastName: String,
    profilePicUrl: String,
    val description: String,
    val waitingTime: Int
) : UserDTO(id, title, firstName, lastName, profilePicUrl) {
    constructor(medical: Medical) : this(
        medical.id,
        medical.title,
        medical.firstName,
        medical.lastName,
        medical.profilePicUrl,
        medical.description,
        medical.waitingTime
    )
}

class PatientDTO(id: String, title: String, firstName: String, lastName: String, profilePicUrl: String) :
    UserDTO(id, title, firstName, lastName, profilePicUrl) {
    constructor(patient: Patient): this(
        patient.id,
        patient.title,
        patient.firstName,
        patient.lastName,
        patient.profilePicUrl
    )
}
