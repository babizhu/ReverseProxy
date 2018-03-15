package com.bbz.network.reverseproxy.impl1

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

/**
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
class HexDumpProxy {
    companion object {
        val LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "8000"))
        val REMOTE_HOST = System.getProperty("remoteHost", "localhost")!!
        val REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "8080"))
    }

    fun start() {
        System.err.println("Proxying *:" + HexDumpProxy.LOCAL_PORT + " to " + HexDumpProxy.REMOTE_HOST + ':' + HexDumpProxy.REMOTE_PORT + " ...");

        // Configure the bootstrap.
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
//                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(HexDumpProxyInitializer(HexDumpProxy.REMOTE_HOST, HexDumpProxy.REMOTE_PORT))
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(HexDumpProxy.LOCAL_PORT).sync().channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

    }
}

