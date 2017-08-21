package cn.weijie.asgard.core

import cn.weijie.asgard.definition.prependSlash
import cn.weijie.asgard.definition.staticRouterMap
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler

class StaticServerVerticle(private val finishFuture: Future<Nothing>) : AbstractVerticle() {

    private lateinit var httpServer : HttpServer

    private val log : Logger = LoggerFactory.getLogger(StaticServerVerticle::class.java)

    override fun start() {

        log.info("Initiating static server")
        val router = Router.router(vertx)

        if (staticRouterMap.isEmpty()) {
            router.route("/static/*").handler(StaticHandler.create().setWebRoot("static"))
            log.info("Bind static handler for path: '/static/*' to root: 'static'")
        } else {
            staticRouterMap.forEach { (path, root) ->
                val realPath = path.prependSlash()
                router.route(realPath).handler(StaticHandler.create().setWebRoot(root))
                log.info("Bind static handler for path: {} to root: {}", realPath, root)
            }
        }

        val port = config().getInteger("port")
        httpServer = vertx.createHttpServer()
        httpServer.requestHandler(router::accept)?.listen(port) {
            if (it.succeeded()) {
                log.info("Static server started on port: {}.", port)
                finishFuture.complete()
            } else {
                log.error("Static server cannot start.", it.cause())
                // TODO(创建自定义异常)
                finishFuture.fail(it.cause())
            }
        }
    }

    override fun stop() {
        log.info("Stopping static server")
        httpServer.close {
            if (it.succeeded()) {
                log.info("Static server stopped.")
            } else {
                log.error("Static server stopping failed.", it.cause())
            }
        }
    }
}
