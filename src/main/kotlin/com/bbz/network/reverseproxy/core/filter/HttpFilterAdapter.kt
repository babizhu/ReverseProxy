package com.bbz.network.reverseproxy.core.filter

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpResponse

open class  HttpFilterAdapter : HttpFilter {
    override fun init() {
    }

    override fun clientToProxyConnected(ctx: ChannelHandlerContext): HttpResponse? {
        return null
    }

    override fun clientToProxyRequest(httpObject: HttpObject): HttpResponse? {
        return null
    }
}