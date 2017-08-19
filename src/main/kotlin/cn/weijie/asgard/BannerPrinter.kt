@file:JvmName("BannerPrinter")
package cn.weijie.asgard

import io.vertx.core.Vertx

fun printBanner(file: String, vertx: Vertx) {
    vertx.fileSystem().let {
        if (it.existsBlocking(file)) {
            it.readFileBlocking(file).toString().let { println(it) }
        } else {
            println()
            println(":::: Asgard server ::::")
        }
    }
    println()
}