package cn.weijie.asgard.dispacher;

import cn.weijie.asgard.endpoint.restful.ResourcePath;
import cn.weijie.asgard.endpoint.rpc.RPC;
import cn.weijie.asgard.endpoint.statics.WebRoot;
import cn.weijie.asgard.endpoint.stream.Files;
import cn.weijie.asgard.endpoint.template.Pages;
import java.lang.annotation.Annotation;

public class AnnotationReader<T> {

    private Class<T> clazz;

    private Class<? extends Annotation> annotation;

    private String typeName;

    @SuppressWarnings("unchecked")
    public AnnotationReader(Class<T> target) {
        clazz = target;
        if (isRPC()) {
            typeName = "RPC";
            annotation = RPC.class;
        } else if (isRestful()) {
            typeName = "RESTful";
            annotation = ResourcePath.class;
        } else if (isStream()) {
            typeName = "Stream";
            annotation = Files.class;
        } else if (isTemplate()) {
            typeName = "Template";
            annotation = Pages.class;
        } else if (isStatic()) {
            typeName = "Static";
            annotation = WebRoot.class;
        } else {
            typeName = "Unknown";
        }
    }

    public boolean isRPC() {
        return clazz.isAnnotationPresent(RPC.class);
    }

    public boolean isRestful() {
        return clazz.isAnnotationPresent(ResourcePath.class);
    }

    public boolean isStream() {
        return clazz.isAnnotationPresent(Files.class);
    }

    public boolean isTemplate() {
        return clazz.isAnnotationPresent(Pages.class);
    }

    public boolean isStatic() {
        return clazz.isAnnotationPresent(WebRoot.class);
    }

    public String getEndpointTypeName() {
        return typeName;
    }

    public Class<? extends Annotation> getEndpointTypeClass() {
        return annotation;
    }
}
