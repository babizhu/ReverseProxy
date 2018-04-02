package com.bbz.network.reverseproxy.route.impl

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.concurrent.EventExecutor
import org.junit.Test
import java.net.SocketAddress

class RoundRobinPolicyTest {

    @Test
    fun getUrl() {

//        var roundRobinPolicy = RoundRobinPolicy()
//        var request = DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.CONNECT, "")
//        var epollServerSocketChannel = EpollServerSocketChannel()
//        val channel: Channel = epollServerSocketChannel
//        repeat(7) {
//            val url = roundRobinPolicy.getBackendServerAddress(request,null)
//
//            println(url)
//        }
    }
}