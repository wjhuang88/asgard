package cn.weijie.asgard;

public class App {

    public static void main(String[] args) {
        AsgardServer.route("test", requestBody -> {
            return requestBody.put("sig", "response");
        });
        AsgardServer.run(8080, 1);
    }
}
