package cn.weijie.asgard

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

interface TemplateAdapter {
    fun templateName() : String
    fun contentType() : String
    fun resolve(data : JsonObject) : Buffer
}