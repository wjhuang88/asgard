package cn.weijie.asgard.endpoint.restful;

import cn.weijie.asgard.definition.MIME;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OPTIONS {
    String produce() default MIME.APPLICATION_JSON;
    String consume() default MIME.APPLICATION_JSON;
}
