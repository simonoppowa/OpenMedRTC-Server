package software.openmedrtc.database

import software.openmedrtc.database.entity.Medical
import software.openmedrtc.database.entity.Patient
import software.openmedrtc.database.entity.User
import java.util.concurrent.ConcurrentHashMap

/**
 * // TODO Remove Mock
 * A Simple database mock with demo users
 */
object UserDatabase {
    val usersRegistered = ConcurrentHashMap<String, User>().apply {
        put("chrome@gmail.com", Medical("chrome@gmail.com", "Dr", "Chromium", "Chrome", "chrome", "General"))
        put("android_29", Medical("android_29", "", "Android", "Pie", "test", "General"))
        put("android_26", Patient("android_26", "", "Android", "Nougat", "test"))
    }

}
