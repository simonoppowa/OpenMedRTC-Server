package software.openmedrtc.database.entity

abstract class User(
    val id: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String,
    val profilePicUrl: String
)

class Patient(
    id: String,
    title: String,
    firstName: String,
    lastName: String,
    passwordHash: String,
    profilePicUrl: String
) :
    User(id, title, firstName, lastName, passwordHash, profilePicUrl)

class Medical(
    id: String,
    title: String,
    firstName: String,
    lastName: String,
    passwordHash: String,
    profilePicUrl: String,
    val description: String,
    val waitingTime: Int = 15
) :
    User(id, title, firstName, lastName, passwordHash, profilePicUrl)
