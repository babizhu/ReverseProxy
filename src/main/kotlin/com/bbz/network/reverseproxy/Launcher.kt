package com.bbz.network.reverseproxy

import com.bbz.network.reverseproxy.impl.DefaultReverseProxyServer

//class Launcher {
//
//}
data class Student(val name:String)

fun main(args: Array<String>) {

    val bootstrap = DefaultReverseProxyServer.bootstrap()
//            .bootstrapFromFile("./littleproxy.properties")
            .withPort(8000)
            .withConnectTimeout(1000)
    bootstrap.start()
}