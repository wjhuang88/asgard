package cn.weijie.asgard;

public class App {

    public static void main(String[] args) throws InterruptedException {
        AsgardServer server = AsgardServer.INSTANCE;
        server.route("test/:id", requestBody -> requestBody.put("sig", "response")).run();
    }
}
