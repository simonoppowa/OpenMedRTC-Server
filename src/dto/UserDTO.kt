package software.openmedrtc.dto

import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.Patient

abstract class UserDTO(
    val email: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val profilePicUrl: String
)

class MedicalDTO(
    email: String,
    title: String,
    firstName: String,
    lastName: String,
    profilePicUrl: String,
    val description: String,
    val waitingTime: Int
) : UserDTO(email, title, firstName, lastName, profilePicUrl) {
    constructor(medical: Medical) : this(
        medical.email,
        medical.title,
        medical.firstName,
        medical.lastName,
        medical.profilePicUrl,
        medical.description,
        medical.waitingTime
    )
}

class PatientDTO(email: String, title: String, firstName: String, lastName: String, profilePicUrl: String) :
    UserDTO(email, title, firstName, lastName, profilePicUrl) {
    constructor(patient: Patient): this(
        patient.email,
        patient.title,
        patient.firstName,
        patient.lastName,
        patient.profilePicUrl
    )
}
