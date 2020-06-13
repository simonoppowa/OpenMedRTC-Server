package software.openmedrtc.helper

import com.google.gson.*
import java.lang.reflect.Field
import java.lang.reflect.Type

/**
 * @author Brian Roach
 * https://stackoverflow.com/questions/21626690/gson-optional-and-required-fields
 */

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
internal annotation class JsonRequired


internal class AnnotatedDeserializer<T: Any> : JsonDeserializer<T> {
    @Throws(JsonParseException::class)
    override fun deserialize(je: JsonElement?, type: Type?, jdc: JsonDeserializationContext?): T {
        val pojo: T = Gson().fromJson(je, type) ?: throw JsonParseException("Could not parse json")

        val fields: Array<Field> = pojo::class.java.declaredFields
        for (f in fields) {
            if (f.getAnnotation(JsonRequired::class.java) != null) {
                f.isAccessible = true
                if (f.get(pojo) == null) {
                    throw JsonParseException("Missing field in JSON: " + f.name)
                }
            }
        }
        return pojo
    }
}
