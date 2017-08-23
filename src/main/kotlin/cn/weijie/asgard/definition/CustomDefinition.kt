package cn.weijie.asgard.definition

import cn.weijie.asgard.AsgardServer
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.sstore.SessionStore
import kotlinx.coroutines.experimental.Job

internal val log : Logger = LoggerFactory.getLogger(AsgardServer::class.java)

// 收集用户注册的路由处理器
internal val routerPool = mutableSetOf<Quadruple<String, String, HttpMethod?, suspend (JsonObject?) -> JsonObject>>()

// 静态资源路由映射
internal val staticRouterMap = mutableMapOf<String, String>()

// 收集所有Content-Type处理器
internal val contentTypePool = mutableMapOf<String, MutableList<(Buffer, (JsonObject) -> Job) -> Unit>>()

internal var sessionStore: SessionStore? = null

// 模板处理器配置
private var _templateAdapter : TemplateAdapter? = null
internal var templateAdapter : TemplateAdapter
    get() = _templateAdapter ?: object : TemplateAdapter {
        init {
            log.debug("Using default json template resolver")
        }
        override fun templateName() = "DefaultJson"
        override fun contentType() = MIME.APPLICATION_JSON
        override fun resolve(data: JsonObject): Buffer {
            data.remove(RESPONSE_FIELD.CONTENT_TYPE)
            data.remove(RESPONSE_FIELD.TEMPLATE)
            return data.toBuffer()
        }
    }
    set(value) {
        log.info("Using template resolver: ${value.templateName()}")
        _templateAdapter = value
    }



