package cn.weijie.asgard.core

import cn.weijie.asgard.definition.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

/**
 * # 服务入口
 * 核心http服务以及路由绑定模块，这是一个verticle定义
 */
class MainServerVerticle(private val finishFuture: Future<Nothing>) : AbstractVerticle() {

    private lateinit var httpServer : HttpServer

    private val log : Logger = LoggerFactory.getLogger(MainServerVerticle::class.java)

    // 服务被部署时的处理方法
    override fun start() {

        log.info("Initiating http server")
        val store = sessionStore?: LocalSessionStore.create(vertx)

        val router = Router.router(vertx)
        router.route().handler(CookieHandler.create())
        router.route().handler(SessionHandler.create(store))
        router.route().handler(BodyHandler.create())

        router.registerStatic(staticRouterMap).register(routerPool)

        val port = config().getInteger("port")
        httpServer = vertx.createHttpServer()
        httpServer.requestHandler(router::accept)?.listen(port) {
            if (it.succeeded()) {
                log.info("Http server started on port: {}.", port)
                finishFuture.complete()
            } else {
                log.error("Http server cannot start.", it.cause())
                // TODO(创建自定义异常)
                finishFuture.fail(it.cause())
            }
        }
    }

    override fun stop() {
        log.info("Stopping http server")
        httpServer.close {
            if (it.succeeded()) {
                log.info("Http server stopped.")
            } else {
                log.error("Http server stopping failed.", it.cause())
            }
        }
    }
}
