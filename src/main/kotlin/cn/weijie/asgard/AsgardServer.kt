package cn.weijie.asgard

import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.Job

object AsgardServer {

    init {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE)
    }

    private val coreInitFuture = Future.future<Nothing>()
    private val httpInitFuture = Future.future<Nothing>()

    /**
     * 在[port]端口启动服务，启动[instance]个verticle实例
     * **必要时可以提供一个你自己的[vertx]对象作为服务启动的上下文 - 这是可选的**
     */
    @JvmOverloads
    fun run(port : Int = 8080, instance : Int = 1, vertx : Vertx = Vertx.vertx()): AsgardServer {

        log.info("Initiating Asgard core.")

        // 启动计时开始变量
        val start = System.currentTimeMillis()

        // 加入默认处理器
        contentTypePool.contentTypeInit()

        // 部署服务
        log.info("Initiating vert.x core")
        vertx.deployVerticle(MainServerVerticle(httpInitFuture), DeploymentOptions()
                        .setInstances(instance)
                        .setConfig(JsonObject().put("port", port))) {
            if (it.succeeded()) {
                coreInitFuture.complete()
            } else {
                coreInitFuture.fail(it.cause())
            }
        }

        // 服务启动完毕
        CompositeFuture.all(coreInitFuture, httpInitFuture).setHandler {
            if (it.succeeded()) {
                log.info("App deployed, took {} ms", System.currentTimeMillis() - start)
            } else {
                log.info("App deploying failed 'cause of : {}", it.cause().localizedMessage)
            }
        }
        return this
    }

    // 扩展方法定义：加入默认content-type处理器
    private fun MutableMap<String, MutableList<(Buffer, (JsonObject) -> Job) -> Unit>>.contentTypeInit() = let {
        it.put(MIME.APPLICATION_JSON, mutableListOf({ buf, run ->
            val body = try {
                JsonObject().put(REQUEST_FIELD.INPUT, buf.toJsonObject())
            } catch (e : DecodeException) {
                try {
                    JsonObject().put(REQUEST_FIELD.INPUT, buf.toJsonArray())
                } catch (e : DecodeException) {
                    JsonObject().put(REQUEST_FIELD.INPUT, "{}")
                }
            }
            run(body)
        }))
        log.info("Loaded default MIME resolver for '{}'", MIME.APPLICATION_JSON)
        it.put(MIME.TEXT_HTML, mutableListOf(::plainTextHandler))
        log.info("Loaded default MIME resolver for '{}'", MIME.TEXT_HTML)
        it.put(MIME.TEXT_PLAIN, mutableListOf(::plainTextHandler))
        log.info("Loaded default MIME resolver for '{}'", MIME.TEXT_PLAIN)
    }

    /**
     * 注册路由处理器，需要提供路径正则表达式[pathPattern]和处理器[handler]
     */
    fun route(pathPattern : String, handler : suspend (JsonObject?) -> JsonObject): AsgardServer {
        routerPool.put(if (pathPattern.startsWith("/")) pathPattern else "/$pathPattern", handler)
        return this
    }

    /**
     * 注册路由处理器，需要提供路径正则表达式[pathPattern]和处理器[handler]，非suspend版本
     * @see route
     */
    fun route(pathPattern : String, handler : (JsonObject?) -> JsonObject): AsgardServer {
        routerPool.put(if (pathPattern.startsWith("/")) pathPattern else "/$pathPattern", { handler.invoke(it) })
        return this
    }

    /**
     * 注册Content-Type处理器，需要提供Content-Type字符串[contentType]以及处理器函数[handler]
     */
    fun contentType(contentType : String, handler : (Buffer, (JsonObject) -> Job) -> Unit): AsgardServer {
        contentTypePool.putIfAbsent(contentType, mutableListOf())?.add(handler)
        log.info("Add MIME resolver for '{}'", contentType)
        return this
    }

    /**
     * 注册模板引擎适配器，提供一个[TemplateAdapter]接口的实现类[adapter]
     */
    fun useTemplateAdapter(adapter : TemplateAdapter): AsgardServer {
        templateAdapter = adapter
        return this
    }
}

