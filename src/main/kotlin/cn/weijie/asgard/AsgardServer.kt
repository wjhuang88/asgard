package cn.weijie.asgard

import cn.weijie.asgard.core.MainServerVerticle
import cn.weijie.asgard.definition.*
import cn.weijie.asgard.dispacher.AnnotationReader
import cn.weijie.asgard.tool.ClasspathPackageScanner
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.sstore.SessionStore
import kotlinx.coroutines.experimental.Job
import java.util.concurrent.atomic.AtomicBoolean

object AsgardServer {

    init {
        printBanner("banner.txt")
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE)
    }

    private val coreInitFuture = Future.future<Nothing>()
    private val httpInitFuture = Future.future<Nothing>()

    private var running : AtomicBoolean = AtomicBoolean(false)
    private var closing : AtomicBoolean = AtomicBoolean(false)

    private var vertx : Vertx? = null
    private var serverFuture : Future<*>? = null

    /**
     * 在[port]端口启动服务，启动[instance]个verticle实例
     * **必要时可以提供一个你自己的[vertx]对象作为服务启动的上下文 - 这是可选的**
     */
    @JvmOverloads
    fun run(port : Int = 8080, vertx : Vertx = Vertx.vertx()) {

        if (running.get()) {
            log.warn("Asgard server is running, no new server will be created")
            return
        }
        running.getAndSet(true)
        this.vertx = vertx
        closing.getAndSet(false)
        log.info("Initiating Asgard core")
        // 启动计时开始变量
        val start = System.currentTimeMillis()
        // 加入默认处理器
        contentTypePool.contentTypeInit()
        // 设置协程执行环境
        VertxContextDispatcher.setVertx(vertx)

        log.info("Initiating vert.x core")

        // 目录扫描执行
        scanRunner()

        // 部署服务
        vertx.deployVerticle(MainServerVerticle(httpInitFuture), DeploymentOptions()
                .setInstances(1)
                .setConfig(JsonObject().put("port", port))) {
            if (it.succeeded()) {
                coreInitFuture.complete()
            } else {
                coreInitFuture.fail(it.cause())
            }
        }

        // 服务启动完毕
        serverFuture = CompositeFuture.all(coreInitFuture, httpInitFuture).setHandler {
            if (it.succeeded()) {
                log.info("App deployed, took {} ms", System.currentTimeMillis() - start)
            } else {
                log.info("App deploying failed 'cause of : {}", it.cause().localizedMessage)
            }
        }

        // 注册关闭事件
        Runtime.getRuntime().addShutdownHook(Thread {
            if (!closing.get()) {
                shutdown()
            }
        })
    }

    /**
     * 关闭服务
     */
    fun shutdown() {
        closing.getAndSet(true)
        while (serverFuture?.isComplete != true) {
            Thread.sleep(100)
        }
        log.info("Shutting down Asgard server")
        val closeFuture = Future.future<Nothing>()
        vertx?.close {
            log.info("Vert.x core closed")
            closeFuture.complete()
            vertx = null
        }
        while (!closeFuture.isComplete) {
            Thread.sleep(100)
        }
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
                    JsonObject().put(REQUEST_FIELD.INPUT, JsonObject())
                }
            }
            run(body)
        }))
        it.put(MIME.TEXT_HTML, mutableListOf(::plainTextHandler))
        it.put(MIME.TEXT_PLAIN, mutableListOf(::plainTextHandler))
        log.info("Loaded default MIME resolver for '{}', '{}', '{}'", MIME.TEXT_PLAIN, MIME.TEXT_HTML, MIME.APPLICATION_JSON)
    }

    /**
     * 注册路由处理器，必须提供路径[pathPattern]和处理器[handler]，
     * 如果需要可以规定处理的MIME类型[contentType]和请求方法[method]
     */
    fun route(
            pathPattern : String,
            contentType: Pair<String, String?> = Pair(MIME.ALL, null),
            method: HttpMethod? = null,
            handler : suspend (JsonObject?) -> JsonObject
    ): AsgardServer {
        routerPool.add(Quadruple(pathPattern.prependSlash(), contentType, method, handler))
        return this
    }

    /**
     * 注册路由处理器，必须提供路径[pathPattern]和处理器[handler]，
     * 如果需要可以规定处理的MIME类型[contentType]和请求方法[method]，非suspend版本
     * @see route
     */
    @JvmOverloads
    fun route(
            pathPattern : String,
            contentType: Pair<String, String?> = Pair(MIME.ALL, null),
            method: HttpMethod? = null,
            handler : (JsonObject?) -> JsonObject
    ): AsgardServer {
        routerPool.add(Quadruple(pathPattern.prependSlash(), contentType, method, { it -> handler(it) }))
        return this
    }

    /**
     * 注册静态资路由信息，将[path]路由到[root]目录中
     */
    fun routeStatic(path : String, root : String) {
        staticRouterMap.put(path, root)
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

    /**
     * 注册session存储
     */
    fun useSessionStore(store: SessionStore) {
        sessionStore = store
    }

    private var scanRunner: () -> Unit = {}

    /**
     * 扫描包路径[packageName]下的所有业务类
     */
    fun scan(vararg packageNames: String) {
        scanRunner = { for (packageName in packageNames) {
            try {
                ClasspathPackageScanner(packageName).fullyQualifiedClassNameList.forEach {
                    val annotationReader = AnnotationReader(Class.forName(it))
                    if (log.isDebugEnabled) {
                        log.debug("Resolving {} class: {}", annotationReader.endpointTypeName, it)
                    }
                    annotationReader.resolver?.resolve(this)
                }
            } catch (e: Exception) {
                log.error("Scan package: $packageName fail", e)
            }
        }}
    }
}

