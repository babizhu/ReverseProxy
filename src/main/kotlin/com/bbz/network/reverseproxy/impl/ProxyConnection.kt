package com.bbz.network.reverseproxy.impl

import io.netty.channel.ChannelInboundHandlerAdapter

abstract class ProxyConnection(protected val proxyServer: DefaultReverseProxyServer) : ChannelInboundHandlerAdapter() {

}