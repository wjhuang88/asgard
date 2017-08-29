package cn.weijie.asgard.dispacher;

import cn.weijie.asgard.AsgardServer;
import cn.weijie.asgard.endpoint.restful.DELETE;
import cn.weijie.asgard.endpoint.restful.GET;
import cn.weijie.asgard.endpoint.restful.HEAD;
import cn.weijie.asgard.endpoint.restful.OPTIONS;
import cn.weijie.asgard.endpoint.restful.PATCH;
import cn.weijie.asgard.endpoint.restful.POST;
import cn.weijie.asgard.endpoint.restful.PUT;
import cn.weijie.asgard.endpoint.restful.ResourcePath;
import io.vertx.core.http.HttpMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import kotlin.Pair;

public class RestfulResolver implements EndpointResolver {

    private Class<?> clazz;

    public RestfulResolver(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void resolve(AsgardServer server) {
        ResourcePath resourcePath = clazz.getAnnotation(ResourcePath.class);
        if (null == resourcePath) {
            return;
        }
        Method[] methods = clazz.getMethods();
        if (null == methods || methods.length <= 0) {
            return;
        }
        Object target = buildTarget(clazz);
        for (Method m : methods) {
            Annotation[] annotations = m.getAnnotations();
            if (null == annotations || annotations.length <= 0) {
                continue;
            }
            for (Annotation anno : annotations) {
                String consume;
                String produce;
                HttpMethod httpMethod;
                if (anno instanceof DELETE) {
                    consume = ((DELETE) anno).consume();
                    produce = ((DELETE) anno).produce();
                    httpMethod = HttpMethod.DELETE;
                } else if (anno instanceof GET) {
                    consume = ((GET) anno).consume();
                    produce = ((GET) anno).produce();
                    httpMethod = HttpMethod.GET;
                } else if (anno instanceof HEAD) {
                    consume = ((HEAD) anno).consume();
                    produce = ((HEAD) anno).produce();
                    httpMethod = HttpMethod.HEAD;
                } else if (anno instanceof OPTIONS) {
                    consume = ((OPTIONS) anno).consume();
                    produce = ((OPTIONS) anno).produce();
                    httpMethod = HttpMethod.OPTIONS;
                } else if (anno instanceof PATCH) {
                    consume = ((PATCH) anno).consume();
                    produce = ((PATCH) anno).produce();
                    httpMethod = HttpMethod.PATCH;
                } else if (anno instanceof POST) {
                    consume = ((POST) anno).consume();
                    produce = ((POST) anno).produce();
                    httpMethod = HttpMethod.POST;
                } else if (anno instanceof PUT) {
                    consume = ((PUT) anno).consume();
                    produce = ((PUT) anno).produce();
                    httpMethod = HttpMethod.PUT;
                } else {
                    continue;
                }
                server.route(
                        resourcePath.value(),
                        new Pair<>(consume, produce),
                        httpMethod,
                        request -> resolveHandler(m, request, target, clazz));
            }
        }
    }
}
