package cn.weijie.asgard;

import io.vertx.core.http.HttpMethod;

public class App {

    public static void main(String[] args) throws InterruptedException {
        AsgardServer server = AsgardServer.INSTANCE;
        server.scan("cn.weijie.asgard.example");
        server.route("test/:id", "*/*", HttpMethod.GET, requestBody -> requestBody.put("sig", "response")).run();
    }

}
