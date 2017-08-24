package cn.weijie.asgard.tool;

import com.google.inject.Guice;
import com.google.inject.Injector;

public final class InjectUtils {

    private InjectUtils() {}

    private final static Injector injector = Guice.createInjector();

    public static <T> T getInstance(Class<T> clazz) {
        return injector.getInstance(clazz);
    }
}
