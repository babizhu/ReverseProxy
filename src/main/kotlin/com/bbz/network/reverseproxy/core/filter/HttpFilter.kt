package com.bbz.network.reverseproxy.core.filter

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpResponse

interface HttpFilter {

    /**
     * 如果返回HttpResponse不为null，则直接返回客户端HttpResponse，
     * 流程结束
     */
    fun clientToProxyRequest(httpObject: HttpObject): HttpResponse?

    /**
     * 客户端连接上来之后调用，可实现ip黑名单等功能
     * 如果返回HttpResponse不为null，则直接返回客户端HttpResponse
     * 流程结束
     */
    fun clientToProxyConnected(ctx:ChannelHandlerContext): HttpResponse?


    /**
     * 初始化filter的相关设置
     */
    fun init()
}