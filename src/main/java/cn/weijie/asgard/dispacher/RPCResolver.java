package cn.weijie.asgard.dispacher;

import cn.weijie.asgard.AsgardServer;
import cn.weijie.asgard.definition.MIME;
import cn.weijie.asgard.endpoint.rpc.Action;
import cn.weijie.asgard.endpoint.rpc.RPC;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import kotlin.Pair;

public class RPCResolver implements EndpointResolver {

    private Class<?> clazz;

    private Logger log = LoggerFactory.getLogger(RPCResolver.class);

    public RPCResolver(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void resolve(AsgardServer server) {
        RPC rpc = clazz.getAnnotation(RPC.class);
        if (null == rpc) {
            return;
        }
        String prefixPath = rpc.value();
        Method[] methods = clazz.getMethods();
        if (null == methods || methods.length <= 0) {
            return;
        }
        Object target = buildTarget(clazz);
        for (Method m : methods) {
            Action action = m.getAnnotation(Action.class);
            if (null == action) {
                continue;
            }
            String actionPath = action.value();
            if ("".equals(actionPath)) {
                actionPath = m.getName();
            }
            server.route(
                    prefixPath + "/" + actionPath,
                    new Pair<>(MIME.APPLICATION_JSON, MIME.APPLICATION_JSON),
                    HttpMethod.POST,
                    request -> resolveHandler(m, request, target, clazz));
        }
    }
}
