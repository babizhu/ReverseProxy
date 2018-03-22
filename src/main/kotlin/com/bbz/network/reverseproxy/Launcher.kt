package com.bbz.network.reverseproxy

import com.bbz.network.reverseproxy.impl.DefaultReverseProxyServer
import io.netty.util.ResourceLeakDetector

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

    //注意，这个选项对性能有很大影响，正式发布版本需要移除
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
//    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED)
//        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED)

    val bootstrap = DefaultReverseProxyServer.bootstrap()
//            .bootstrapFromFile("./littleproxy.properties")
            .withPort(8000)
            .withConnectTimeout(3000)
    bootstrap.start()

//    ReverseProxy().start()
}