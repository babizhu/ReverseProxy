package com.bbz.network.reverseproxy.route

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest
import java.net.InetSocketAddress

interface RoutePolicy {
    fun getBackendServerAddress(request: HttpRequest, channel: Channel): InetSocketAddress?

}