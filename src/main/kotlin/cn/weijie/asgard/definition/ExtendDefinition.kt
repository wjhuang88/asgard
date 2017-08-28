package cn.weijie.asgard.definition

import io.vertx.core.MultiMap
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Job

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
 * 按照MIME解析用户输入参数
 */
internal fun RoutingContext.dispatch(runHandler : (JsonObject) -> Job) = also {
    val contentTypeHandlers = contentTypePool[request().contentType()] ?: listOf(::plainTextHandler)
    contentTypeHandlers.forEach { handler ->
        handler(body, runHandler)
    }
}