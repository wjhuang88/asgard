package cn.weijie.asgard.definition

import cn.weijie.asgard.AsgardServer
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.experimental.Job

internal val log : Logger = LoggerFactory.getLogger(AsgardServer::class.java)

// 收集用户注册的路由处理器
internal val routerPool = mutableMapOf<String, suspend (JsonObject?) -> JsonObject>()

// 收集所有Content-Type处理器
internal val contentTypePool = mutableMapOf<String, MutableList<(Buffer, (JsonObject) -> Job) -> Unit>>()

// 模板处理器配置
private var _templateAdapter : TemplateAdapter? = null
internal var templateAdapter : TemplateAdapter
    get() = _templateAdapter ?: object : TemplateAdapter {
        init {
            log.debug("Using default json template resolver")
        }
        override fun templateName(): String = "DefaultJson"
        override fun contentType() = MIME.APPLICATION_JSON
        override fun resolve(data: JsonObject) = data.toBuffer()
    }
    set(value) {
        log.info("Using template resolver: ${value.templateName()}")
        _templateAdapter = value
    }

