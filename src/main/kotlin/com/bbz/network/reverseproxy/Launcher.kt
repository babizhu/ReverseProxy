package com.bbz.network.reverseproxy

import com.bbz.network.reverseproxy.impl.DefaultReverseProxyServer

//class Launcher {
//
//}
/**
 * macos
 * ab -k -r  -c 100 -n 100000 http://localhost:8000/
 * Concurrency Level:      100
 * Time taken for tests:   11.212 seconds
 * Complete requests:      100000
 * Failed requests:        0
 * Keep-Alive requests:    99064
 * Total transferred:      84995320 bytes
 * HTML transferred:       61200000 bytes
 * Requests per second:    8918.64 [#/sec] (mean)----7450(带上http codec之后的数据)
 * Time per request:       11.212 [ms] (mean)
 * Time per request:       0.112 [ms] (mean, across all concurrent requests)
 * Transfer rate:          7402.76 [Kbytes/sec] received
 */
fun main(args: Array<String>) {

    val bootstrap = DefaultReverseProxyServer.bootstrap()
//            .bootstrapFromFile("./littleproxy.properties")
            .withPort(8000)
            .withConnectTimeout(3000)
    bootstrap.start()

//    ReverseProxy().start()
}