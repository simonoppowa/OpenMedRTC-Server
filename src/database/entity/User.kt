package software.openmedrtc.database.entity

abstract class User(
    val email: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String
)

class Patient(email: String, title: String, firstName: String, lastName: String, passwordHash: String) :
    User(email, title, firstName, lastName, passwordHash)

class Medical(
    email: String,
    title: String,
    firstName: String,
    lastName: String,
    passwordHash: String,
    val description: String,
    val waitingTime: Int = 15
) :
    User(email, title, firstName, lastName, passwordHash)
