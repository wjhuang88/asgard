package cn.weijie.asgard.dispacher;

import cn.weijie.asgard.AsgardServer;
import cn.weijie.asgard.definition.CookieResolver;
import cn.weijie.asgard.definition.REQUEST_FIELD;
import cn.weijie.asgard.definition.RESPONSE_FIELD;
import cn.weijie.asgard.definition.SessionResolver;
import cn.weijie.asgard.tool.InjectUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.reflect.Parameter;

public interface EndpointResolver {

    Logger log = LoggerFactory.getLogger(EndpointResolver.class);

    default Object buildTarget(Class<?> clazz) {
        return InjectUtils.getInstance(clazz);
    }

    default Object buildParameter(Parameter p, JsonObject request) {
        Class<?> type = p.getType();
        if (JsonObject.class.isAssignableFrom(type)) {
            return new JsonObject()
                    .put(REQUEST_FIELD.INPUT, request.getValue(REQUEST_FIELD.INPUT))
                    .put(REQUEST_FIELD.PARAMS, request.getValue(REQUEST_FIELD.PARAMS));
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

    void resolve(AsgardServer server);

}
