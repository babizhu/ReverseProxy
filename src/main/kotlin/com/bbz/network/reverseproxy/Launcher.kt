package com.bbz.network.reverseproxy

import com.bbz.network.reverseproxy.impl.DefaultReverseProxyServer

//class Launcher {
//
//}

fun main(args: Array<String>) {
    val bootstrap = DefaultReverseProxyServer.bootstrap()
//            .bootstrapFromFile("./littleproxy.properties")
            .withPort(8080)
            .withConnectTimeout(1000)
    bootstrap.start()
}