package com.bbz.network.reverseproxy.impl

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel


class ReverseProxy {
    companion object {
        val LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "8000"))
        val REMOTE_HOST = System.getProperty("remoteHost", "localhost")!!
        val REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "8080"))
    }

    fun start() {
//        System.err.println("Proxying *:$LOCAL_PORT to $REMOTE_HOST:$REMOTE_PORT ...");
//
//        // Configure the bootstrap.
//        val bossGroup = NioEventLoopGroup(1)
//        val workerGroup = NioEventLoopGroup()
//        try {
//            val b = ServerBootstrap()
//            b.group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel::class.java)
////                    .handler(LoggingHandler(LogLevel.INFO))
//                    .childHandler(ReverseProxyInitializer(this))
//                    .childOption(ChannelOption.AUTO_READ, false)
//                    .bind(LOCAL_PORT).sync().channel().closeFuture().sync()
//        } finally {
//            bossGroup.shutdownGracefully()
//            workerGroup.shutdownGracefully()
//        }

    }
}

