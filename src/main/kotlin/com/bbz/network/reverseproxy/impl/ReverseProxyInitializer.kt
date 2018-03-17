package com.bbz.network.reverseproxy.impl

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.timeout.IdleStateHandler

class ReverseProxyInitializer(private val proxyServer: DefaultReverseProxyServer): ChannelInitializer<SocketChannel>() {

    public override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast("codec", HttpServerCodec())
        ch.pipeline().addLast(
                "idle",
                IdleStateHandler(0, 0, proxyServer.getIdleConnectionTimeout()))
//        ch.pipeline().addLast(LoggingHandler (LogLevel.INFO))

        ch.pipeline().addLast("handler", ClientToProxyConnection(proxyServer ))
    }
}