@file:JvmName("BannerPrinter")
package cn.weijie.asgard

import cn.weijie.asgard.definition.prependSlash

fun printBanner(file: String) {
    AsgardServer::class.java.getResourceAsStream(file.prependSlash()).let {
        if (null != it && it.available() > 0) {
            it.bufferedReader().readText().let { println(it) }
        } else {
            println()
            println(":::: Asgard server ::::")
        }
    }
    println()
}