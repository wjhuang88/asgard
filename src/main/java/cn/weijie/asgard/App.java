package cn.weijie.asgard;

public class App {

    public static void main(String[] args) {
        AsgardServer server = AsgardServer.INSTANCE;
        server.route("test", requestBody -> requestBody.put("sig", "response")).run();
    }
}
