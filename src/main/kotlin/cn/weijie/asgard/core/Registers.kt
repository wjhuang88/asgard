package cn.weijie.asgard.core

import cn.weijie.asgard.definition.*
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import kotlinx.coroutines.experimental.launch

private val log : Logger = LoggerFactory.getLogger("${Router::class.java.name} -- [Register]")

// 静态资源路由注册
internal fun Router.registerStatic(staticRouterMap : Set<Quadruple<String, String, String?, String?>>) = also { router ->
    if (staticRouterMap.isEmpty()) {
        router.route("/static/*").handler(StaticHandler.create().setWebRoot("static"))
        log.info("Bind static handler for path: '/static/*' to root dir: 'static'")
    } else {
        staticRouterMap.forEach { (path, root, produce, index) ->
            val realPath = path.prependSlash()
            val route = router.route(realPath)
            val staticHandler = StaticHandler.create()
            produce?.let { route.produces(it) }
            index?.let { staticHandler.setIndexPage(it) }
            if (root.startsWith("/")) {
                staticHandler.setAllowRootFileSystemAccess(true)
            }
            staticHandler.setWebRoot(root)
            route.handler(staticHandler)
            log.info("Bind static handler for path: '{}' to root dir: '{}'", realPath, root)
        }
    }
}

// 路由处理器注册
internal fun Router.register(records : Set<Quadruple<String, Pair<String, String?>, HttpMethod?, suspend (JsonObject?) -> JsonObject>>) = also { router ->
    records.forEach { (routePath, contentType, httpMethod, routeHandler) ->
        val registerHandler: (RoutingContext) -> Unit = { it: RoutingContext ->
            // 执行分发
            it.dispatch(runHandler(it)(routeHandler)(contentType))
        }
        val route = router.route(routePath).consumes(contentType.first)
        if (null != contentType.second && contentType.second != MIME.ALL) {
            route.produces(contentType.second)
        }
        if (httpMethod != null) {
            route.method(httpMethod)
        }
        route.handler(registerHandler)
        log.info("Bind routing handler for path: '{}' with content-type: '{}' via method: '{}'", routePath, contentType, httpMethod ?: "*")
    }
}

// 创建协程运行用户请求处理器
private fun runHandler(rc: RoutingContext)
        = fun(rh: suspend (JsonObject?) -> JsonObject)
        = fun(ct: Pair<String, String?>)
        = fun(body : JsonObject) = launch(VertxContextDispatcher) {
    if (log.isDebugEnabled) {
        log.debug("Received request to {}", rc.request().uri())
    }
    // 业务代码输入对象中加入请求信息
    body.put(REQUEST_FIELD.HEADERS, HeaderResolver(rc))
            .put(REQUEST_FIELD.PARAMS, ParameterResolver(rc))
            .put(REQUEST_FIELD.URI, rc.request().uri())
            .put(REQUEST_FIELD.HOST, rc.request().host())
            .put(REQUEST_FIELD.PATH, rc.request().path())
            .put(REQUEST_FIELD.QUERY, rc.request().query())
            .put(REQUEST_FIELD.COOKIES, CookieResolver(rc))
            .put(REQUEST_FIELD.SESSION, SessionResolver(rc))
    rc.fileUploads().let { files ->
        if (!files.isEmpty()) {
            body.put(REQUEST_FIELD.UPLOAD_FILES, files)
        }
    }
    if (log.isDebugEnabled) {
        log.debug("Request data: {}", body)
    }
    val result = rh(body)
    val resContentType = ct.second ?: templateAdapter.contentType()
    rc.response().putHeader(HttpHeaders.CONTENT_TYPE, resContentType)
    rc.response().end(templateAdapter.resolve(result))
}