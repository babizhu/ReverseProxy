package com.bbz.network.reverseproxy.route

import io.netty.handler.codec.http.HttpRequest
import io.netty.channel.ChannelHandlerContext
import java.net.InetSocketAddress

interface RoutePolicy {
    fun getUrl(request: HttpRequest, ctx: ChannelHandlerContext): InetSocketAddress

}