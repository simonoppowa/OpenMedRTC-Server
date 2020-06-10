package software.openmedrtc.dto

import software.openmedrtc.database.entity.Medical

abstract class UserDTO(val email: String, val title: String, val firstName: String, val lastName: String)

class MedicalDTO(
    email: String,
    title: String,
    firstName: String,
    lastName: String,
    val description: String,
    val waitingTime: Int
) : UserDTO(email, title, firstName, lastName) {
    constructor(medical: Medical) : this(
        medical.email,
        medical.title,
        medical.firstName,
        medical.lastName,
        medical.description,
        medical.waitingTime
    )
}
