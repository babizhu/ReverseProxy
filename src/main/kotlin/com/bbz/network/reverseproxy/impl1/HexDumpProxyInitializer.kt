package com.bbz.network.reverseproxy.impl1

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.timeout.IdleStateHandler

class HexDumpProxyInitializer(private val remoteHost: String,
                              private val remotePort: Int) : ChannelInitializer<SocketChannel>() {

    public override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast("codec", HttpServerCodec())
        ch.pipeline().addLast(
                "idle",
                IdleStateHandler(0, 0, 70))
//        ch.pipeline().addLast(LoggingHandler (LogLevel.INFO))

        ch.pipeline().addLast("handler", HexDumpProxyFrontendHandler(remoteHost, remotePort))
    }
}