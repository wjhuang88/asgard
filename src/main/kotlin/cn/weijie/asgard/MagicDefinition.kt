package cn.weijie.asgard

import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.impl.CookieImpl
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

internal fun HttpServerRequest.cookies() = this.getHeader(HttpHeaders.COOKIE)?.let {
    ServerCookieDecoder.LAX.decode(it).map(::CookieImpl)
} ?: emptyList()

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
 * 扩展方法：执行结果最终返回，提供执行方法体[runHandler]
 */
internal inline fun JsonObject.endInput(runHandler : (JsonObject) -> Job) = let { runHandler(JsonObject().put(REQUEST_FIELD.INPUT, it)) }

/**
 * 将协程运行环境指定到[vertx]的worker线程池上
 */
open class VertxContextDispatcher(private val vertx: Vertx) : CoroutineDispatcher() {
    init {
        log.info("Set coroutine dispatcher into vertx worker thread pool.")
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