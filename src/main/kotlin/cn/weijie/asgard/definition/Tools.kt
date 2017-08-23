package cn.weijie.asgard.definition

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.RoutingContext
import java.io.Serializable

/**
 * 定义模板处理器接口
 */
interface TemplateAdapter {
    fun templateName() : String
    fun contentType() : String
    fun resolve(data : JsonObject) : Buffer
}

private class SimpleEntry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
        val oldValue = value
        value = newValue
        return oldValue
    }
}

private fun Cookie.toJsonObject() = mapOf(
        Pair("name", name),
        Pair("value", value),
        Pair("domain", domain),
        Pair("path", path),
        Pair("isChanged", isChanged)
).run { JsonObject(this) }

private fun JsonObject.toCookie(): Cookie {
    val cookie = Cookie.cookie(getString("name"), getString("value"))
    cookie.domain = getString("domain")
    cookie.path = getString("path")
    return cookie
}

/**
 * 定义cookie处理器
 */
class CookieResolver(private val rc: RoutingContext) : MutableMap<String, JsonObject> {

    override val entries: MutableSet<MutableMap.MutableEntry<String, JsonObject>>
        get() = rc.cookies().mapTo(LinkedHashSet()) { SimpleEntry(it.name, it.toJsonObject()) }
    override val keys: MutableSet<String>
        get() = rc.cookies().mapTo(LinkedHashSet()) { it.name }
    override val size: Int
        get() = rc.cookieCount()
    override val values: MutableCollection<JsonObject>
        get() = rc.cookies().mapTo(LinkedHashSet()) { it.toJsonObject() }

    override fun containsKey(key: String): Boolean {
        return null != rc.getCookie(key)
    }

    override fun containsValue(value: JsonObject): Boolean {
        return false
    }

    override fun get(key: String): JsonObject? {
        val cookie = rc.getCookie(key)
        return cookie.toJsonObject()
    }

    override fun isEmpty(): Boolean {
        return rc.cookieCount() <= 0
    }

    override fun clear() {
        keys.forEach {rc.removeCookie(it)}
    }

    override fun put(key: String, value: JsonObject): JsonObject? {
        val oldValue = get(key)
        rc.addCookie(value.toCookie())
        return oldValue
    }

    override fun putAll(from: Map<out String, JsonObject>) {
        from.forEach { (_, value) -> rc.addCookie(value.toCookie()) }
    }

    override fun remove(key: String): JsonObject? {
        val removeCookie = rc.removeCookie(key)
        return removeCookie.toJsonObject()
    }

    fun addCookie(name: String, value: String) = rc.addCookie(Cookie.cookie(name, value))
    fun getCookie(name: String) = rc.getCookie(name)
    fun removeCookie(name: String) = rc.removeCookie(name)
}

/**
 * 定义session处理器
 */
class SessionResolver(private val rc: RoutingContext) : MutableMap<String, JsonObject>
        by rc.session().data().mapValuesTo(LinkedHashMap(), { (_, value) -> JsonObject.mapFrom(value)}) {

    fun addSession(name: String, value: Any) = rc.session().put(name, value)
    fun <T> getSession(name: String) = rc.session().get<T>(name)
    fun <T> removeSession(name: String) = rc.session().remove<T>(name)
}

/**
 * 定义四元组
 */
data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val forth: D
) : Serializable {
    override fun toString(): String = "($first, $second, $third, $forth)"
}

fun <T> Quadruple<T, T, T, T>.toList(): List<T> = listOf(first, second, third, forth)