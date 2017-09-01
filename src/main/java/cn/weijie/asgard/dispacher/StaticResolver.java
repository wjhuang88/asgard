package cn.weijie.asgard.dispacher;

import cn.weijie.asgard.AsgardServer;
import cn.weijie.asgard.endpoint.statics.WebRoot;

public class StaticResolver implements EndpointResolver {

    private Class<?> clazz;

    public StaticResolver(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void resolve(AsgardServer server) {
        WebRoot webRoot = clazz.getAnnotation(WebRoot.class);
        if (null == webRoot) {
            return;
        }
        server.routeStatic(webRoot.path(), webRoot.dir(), webRoot.produce(), webRoot.index());
    }
}
