package com.bbz.network.reverseproxy.impl1

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

class HexDumpProxyInitializer(private val remoteHost: String,
                              private val remotePort: Int) : ChannelInitializer<SocketChannel>() {

    public override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(
//                 LoggingHandler (LogLevel.INFO),
                HexDumpProxyFrontendHandler(remoteHost, remotePort));
    }
}