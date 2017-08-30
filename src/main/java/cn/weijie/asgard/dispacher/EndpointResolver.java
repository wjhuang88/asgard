package cn.weijie.asgard.dispacher;

import cn.weijie.asgard.AsgardServer;
import cn.weijie.asgard.definition.CookieResolver;
import cn.weijie.asgard.definition.HeaderResolver;
import cn.weijie.asgard.definition.ParameterResolver;
import cn.weijie.asgard.definition.REQUEST_FIELD;
import cn.weijie.asgard.definition.RESPONSE_FIELD;
import cn.weijie.asgard.definition.RequestResolver;
import cn.weijie.asgard.definition.SessionResolver;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.joor.Reflect;

public interface EndpointResolver {

    Logger log = LoggerFactory.getLogger(EndpointResolver.class);

    default Object buildTarget(Class<?> clazz) {
        return Reflect.on(clazz).create().get();
    }

    default Object buildParameter(Parameter p, JsonObject request) {
        Class<?> type = p.getType();
        if (JsonObject.class.isAssignableFrom(type)) {
            return new JsonObject()
                    .put(REQUEST_FIELD.INPUT, request.getValue(REQUEST_FIELD.INPUT))
                    .put(REQUEST_FIELD.PARAMS, request.getValue(REQUEST_FIELD.PARAMS));
        }
        if (RequestResolver.class.isAssignableFrom(type)) {
            return new RequestResolver(request);
        }
        if (HeaderResolver.class.isAssignableFrom(type)) {
            return request.getJsonObject(REQUEST_FIELD.HEADERS).getMap();
        }
        if (ParameterResolver.class.isAssignableFrom(type)) {
            return request.getJsonObject(REQUEST_FIELD.PARAMS).getMap();
        }
        if (CookieResolver.class.isAssignableFrom(type)) {
            return request.getJsonObject(REQUEST_FIELD.COOKIES).getMap();
        }
        if (SessionResolver.class.isAssignableFrom(type)) {
            return request.getJsonObject(REQUEST_FIELD.SESSION).getMap();
        }
        return null;
    }

    default JsonObject buildResult(Object origin) {
        if (origin instanceof JsonObject) {
            return (JsonObject) origin;
        }
        JsonObject result = new JsonObject();
        try {
            result.put(RESPONSE_FIELD.DATA, origin);
        } catch (IllegalStateException e) {
            log.warn(e.getLocalizedMessage());
            try {
                result.put(RESPONSE_FIELD.DATA, JsonObject.mapFrom(origin));
            } catch (IllegalArgumentException x) {
                log.warn(x.getLocalizedMessage());
            }
        }
        return result;
    }

    default JsonObject resolveHandler(Method m, JsonObject request, Object target, Class<?> clazz) {
        Parameter[] parameters = m.getParameters();
        List<Object> args = new ArrayList<>(parameters.length);
        for (Parameter p : parameters) {
            Object argInstance = buildParameter(p, request);
            args.add(argInstance);
        }
        Object result = null;
        try {
            result = m.invoke(target, args.toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Method: {} from class: {} cannot be invoked successfully", e, m.getName(), clazz.getName());
        }
        log.debug("Method: {} from class: {} be invoked", m.getName(), clazz.getName());
        return buildResult(result);
    }

    void resolve(AsgardServer server);

}
