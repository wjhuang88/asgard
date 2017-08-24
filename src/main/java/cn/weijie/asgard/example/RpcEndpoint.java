package cn.weijie.asgard.example;

import cn.weijie.asgard.definition.CookieResolver;
import cn.weijie.asgard.definition.SessionResolver;
import cn.weijie.asgard.endpoint.rpc.Action;
import cn.weijie.asgard.endpoint.rpc.RPC;
import io.vertx.core.json.JsonObject;

@RPC("rpc")
public class RpcEndpoint {

    @Action
    public String test(SessionResolver session, CookieResolver cookie, JsonObject data) {

        session.addSession("test", "test session");
        session.addSession("test2", new JsonObject("{\"TestJson\":\"test json value\"}"));
        System.out.println(session);
        System.out.println(cookie);
        System.out.println(data);

        return "test rpc";
    }
}
