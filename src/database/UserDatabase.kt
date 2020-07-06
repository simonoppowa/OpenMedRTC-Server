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
        put("chrome@gmail.com", Medical("chrome@gmail.com", "Dr", "Chromium", "Chrome", "chrome", mockPicUrl, "General"))
        put("android_29@gmail.com", Medical("android_29@gmail.com", "", "Android", "Pie", "test", mockPicUrl,"General"))
        put("android_26@gmail.com", Patient("android_26@gmail.com", "", "Android", "Nougat", "test", mockPicUrl))
    }

}
