package cn.weijie.asgard.definition

import com.google.common.collect.Maps
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.Session
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import java.io.Serializable
import kotlin.coroutines.experimental.CoroutineContext

/**
 * 纯文本返回处理器
 */
internal inline fun plainTextHandler(buf : Buffer, run : (JsonObject) -> Job) {
    run(JsonObject().put(REQUEST_FIELD.INPUT, buf.toString()))
}

/**
 * 定义模板处理器接口
 */
interface TemplateAdapter {
    fun templateName() : String
    fun contentType() : String
    fun resolve(data : JsonObject) : Buffer
}

/**
 * 代理Request数据
 */
class RequestResolver(private val json: JsonObject) {
    fun getHeaders() = json.getJsonObject(REQUEST_FIELD.HEADERS).map as HeaderResolver
    fun getParams() = json.getJsonObject(REQUEST_FIELD.PARAMS).map as ParameterResolver
    fun getCookies() = json.getJsonObject(REQUEST_FIELD.COOKIES).map as CookieResolver
    fun getSessions() = json.getJsonObject(REQUEST_FIELD.SESSION).map as SessionResolver

    fun getHeader(key: String) = json.getJsonObject(REQUEST_FIELD.HEADERS).getJsonArray(key)?.list?.map { it.toString() }
    fun getSingleHeader(key: String) = getHeader(key)?.get(0)

    fun getParam(key: String) = json.getJsonObject(REQUEST_FIELD.PARAMS).getJsonArray(key)?.list?.map { it.toString() }
    fun getSingleParam(key: String) = getParam(key)?.get(0)

    fun getRequestBody() = json.map[REQUEST_FIELD.INPUT]
    fun getRequestBodyAsString() = getRequestBody() as String
    fun getRequestBodyAsJsonObject() = getRequestBody() as JsonObject
    fun getRequestBodyAsJsonArray() = getRequestBody() as JsonArray
}

/**
 * 代理请求中的params值
 */
class ParameterResolver(rc: RoutingContext) : MutableMap<String, Any>
    by JsonObject().handleParams(rc.request().params()).map

/**
 * 代理请求中的header值
 */
class HeaderResolver(rc: RoutingContext) : MutableMap<String, Any>
    by JsonObject().handleParams(rc.request().headers()).map

/**
 * 一个MutableMap.MutableEntry<K, V>]的简单实现
 */
private class SimpleEntry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
        val oldValue = value
        value = newValue
        return oldValue
    }
}

/**
 * 扩展[Cookie]对象，添加转换到[JsonObject]的方法
 */
private fun Cookie.toJsonObject() = mapOf(
        Pair("name", name),
        Pair("value", value),
        Pair("domain", domain),
        Pair("path", path),
        Pair("isChanged", isChanged)
).run { JsonObject(this) }

/**
 * 扩展[JsonObject]，添加转换为[Cookie]的方法
 * @see toJsonObject
 */
private fun JsonObject.toCookie(): Cookie {
    val cookie = Cookie.cookie(getString("name"), getString("value"))
    cookie.domain = getString("domain")
    cookie.path = getString("path")
    return cookie
}

/**
 * 定义*cookie*处理器
 *
 * @property rc 路由处理上下文
 * @constructor 创建cookie处理器
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

    override fun toString(): String {
        return mapToString(this)
    }
}

/**
 * 定义session处理器，需要提供路由请求上下文对象[rc]
 *
 * @property rc 路由处理上下文
 * @constructor 创建cookie处理器
 */
class SessionResolver(private val rc: RoutingContext) : MutableMap<String, String> by rc.session().transToMap() {

    fun addSession(name: String, value: Any): Session = rc.session().put(name, value)
    fun <T> getSession(name: String) = rc.session().get<T>(name)
    fun <T> removeSession(name: String) = rc.session().remove<T>(name)

    override fun toString(): String {
        return mapToString(this)
    }
}

/**
 * 创建一个[Session]对象的代理Map
 */
private fun Session.transToMap() = data().let({
    Maps.transformValues(it, {
        value -> when(value) {
            is String -> value
            is Number, is Boolean, is Char -> value.toString()
            else -> Json.encode(value)
        }
    })
})
private fun <K, V> mapToString(map: Map<K, V>): String = map.entries.joinToString(", ", "{", "}") { mapToString(map, it) }
private fun <K, V> mapToString(map: Map<K, V>, entry: Map.Entry<K, V>): String = mapToString(map, entry.key) + "=" + mapToString(map, entry.value)
private fun <K, V> mapToString(map: Map<K, V>, o: Any?): String = if (o === map) "(this Map)" else o.toString()

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

/**
 * 将协程运行环境指定到[vertx]的worker线程池上
 */
object VertxContextDispatcher : CoroutineDispatcher() {

    private lateinit var vertx: Vertx

    fun setVertx(vertx: Vertx = Vertx.vertx()) {
        this.vertx = vertx
        log.info("Set coroutine dispatcher into vert.x worker thread pool.")
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        vertx.executeBlocking<Any>({
            it.complete(block.run())
        }, {
            if (!it.succeeded()) {
                log.error("Request handler executing failed.", it.cause())
            }
        })
    }
}