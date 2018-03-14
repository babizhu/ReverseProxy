package com.bbz.network.reverseproxy

import com.bbz.network.reverseproxy.impl.DefaultReverseProxyServer
import com.bbz.network.reverseproxy.impl1.HexDumpProxy

//class Launcher {
//
//}
data class Student(val name:String)

fun main(args: Array<String>) {

//    val bootstrap = DefaultReverseProxyServer.bootstrap()
////            .bootstrapFromFile("./littleproxy.properties")
//            .withPort(8000)
//            .withConnectTimeout(3000)
//    bootstrap.start()

    HexDumpProxy().start()
}