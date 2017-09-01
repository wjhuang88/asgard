package cn.weijie.asgard;

import cn.weijie.asgard.definition.MIME;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import kotlin.Pair;

public class App {

    public static void main(String[] args) throws InterruptedException {
        AsgardServer server = AsgardServer.INSTANCE;
        server.scan("cn.weijie.asgard");
        server.route("test/:id", new Pair<>(MIME.ALL, MIME.ALL), HttpMethod.GET, requestBody -> requestBody.put("sig", "response")).run();
        server.shutdown();
    }
}
