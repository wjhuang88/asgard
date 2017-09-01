package cn.weijie.asgard.endpoint.statics;

import cn.weijie.asgard.definition.MIME;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface WebRoot {
    String path() default "/";
    String dir();
    String produce() default MIME.TEXT_HTML;
    String index() default "/index.html";
}
