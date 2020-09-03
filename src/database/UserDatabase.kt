package software.openmedrtc.database

import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.Patient
import software.openmedrtc.database.entity.User
import java.util.concurrent.ConcurrentHashMap

/**
 * // TODO Remove Mock
 * A Simple database mock with demo users
 */
const val mockPicUrl = "https://picsum.photos/200"
object UserDatabase {
    val usersRegistered = ConcurrentHashMap<String, User>().apply {
        put("john_doe", Medical("john_doe", "Dr.", "John", "Doe", "chrome", mockPicUrl, "General Practitioner"))
        put("sally_smith", Medical("sally_smith", "Dr.", "Sally", "Smith", "test", mockPicUrl,"General Practitioner"))
        put("jane_smith", Patient("jane_smith", "", "Jane", "Smith", "test", mockPicUrl))
    }

}
