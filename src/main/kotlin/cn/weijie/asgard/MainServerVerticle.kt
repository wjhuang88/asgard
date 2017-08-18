package cn.weijie.asgard

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.streams.ReadStream
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

/**
 * # 服务入口
 * 核心http服务以及路由绑定模块，这是一个verticle定义
 */
class MainServerVerticle(private val finishFuture: Future<Nothing>) : AbstractVerticle() {

    private var httpServer : HttpServer? = null

    private val log : Logger = LoggerFactory.getLogger(MainServerVerticle::class.java)

    // 服务被部署时的处理方法
    override fun start() {

        val router = Router.router(vertx)

        // 开始处理路由分发逻辑
        routerPool.forEach { (first, second) ->
            router.routeWithRegex(first).handler {
                val request = it.request()
                val response = it.response()

                // 创建协程运行用户请求处理器
                fun runHandler(body : JsonObject) = launch(VertxContextDispatcher(vertx)) {
                    log.debug("Received request to {}", request.uri())
                    body.put(REQUEST_FIELD.HEADERS, JsonObject().handleParams(request.headers()))
                            .put(REQUEST_FIELD.PARAMS, JsonObject().handleParams(request.params()))
                            .put(REQUEST_FIELD.URI, request.uri())
                            .put(REQUEST_FIELD.HOST, request.host())
                            .put(REQUEST_FIELD.PATH, request.path())
                            .put(REQUEST_FIELD.QUERY, request.query())
                            .put(REQUEST_FIELD.COOKIES, request.cookies())
                    val result = second(body)
                    response.putHeader(HttpHeaders.CONTENT_TYPE,
                            result.getString(RESPONSE_FIELD.CONTENT_TYPE, templateAdapter.contentType()))
                    response.end(templateAdapter.resolve(result))
                }

                // 执行分发
                it.dispatch(::runHandler)
            }
            log.info("Bind routing handler for path: '{}'", first)
        }

        val port = config().getInteger("port")
        log.info("Initiating http server")
        httpServer = vertx.createHttpServer()
        httpServer?.requestHandler(router::accept)?.listen(port) {
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
        httpServer?.close {
            if (it.succeeded()) {
                log.info("Server stopped.")
            } else {
                log.error("Server stopping failed.", it.cause())
            }
        }
    }

    private fun RoutingContext.dispatch(runHandler : (JsonObject) -> Job) = when {
        request().contentType().contains(MIME.APPLICATION_FORM_URLENCODED) -> {
            request().isExpectMultipart = true
            request().endHandler {
                val formAttributes = request().formAttributes()
                val params = request().params()
                JsonObject().handleParams(formAttributes).handleParams(params).endInput(runHandler)
            }
        }
        request().contentType().contains(MIME.MULTIPART_FORM_DATA) -> { // multipart表单以及文件上传处理
            request().isExpectMultipart = true
            val fileList = mutableListOf<ReadStream<Buffer>>()
            request().uploadHandler { upload ->
                fileList.add(upload)
            }
            request().endHandler {
                val formAttributes = request().formAttributes()
                JsonObject().put(REQUEST_FIELD.FILE_STREAM, fileList).handleParams(formAttributes).endInput(runHandler)
            }
        }
        else -> { // 默认情况搜索设置的自定义格式处理器
            val contentTypeHandlers = contentTypePool[request().contentType()] ?: listOf(::plainTextHandler)
            request().bodyHandler { buf ->
                contentTypeHandlers.forEach { handler ->
                    handler(buf, runHandler)
                }
            }
        }
    }
}
