package cn.weijie.asgard.definition

import cn.weijie.asgard.definition.VertxContextDispatcher.vertx
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

/**
 * 纯文本返回处理器
 */
internal inline fun plainTextHandler(buf : Buffer, run : (JsonObject) -> Job) {
    run(JsonObject().put(REQUEST_FIELD.INPUT, buf.toString()))
}

/**
 * 扩展方法：获取request中的Content-Type
 */
internal fun HttpServerRequest.contentType(): String = this.getHeader(HttpHeaders.CONTENT_TYPE) ?: MIME.ALL

/**
 * 扩展方法：JsonObject中注入MultiMap数据，提供需要注入的MultiMap对象[attributes]
 */
internal fun JsonObject.handleParams(attributes: MultiMap) = also { ret ->
    attributes.forEach { (key, value) ->
        if (ret.containsKey(key)) {
            ret.getJsonArray(key).add(value)
        } else {
            ret.put(key, mutableListOf(value))
        }
    }
}

/**
 * 扩展方法：如果字符串不是"/"开头就添加"/"
 */
internal fun String.prependSlash() = if (this.startsWith("/")) this else "/$this"

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