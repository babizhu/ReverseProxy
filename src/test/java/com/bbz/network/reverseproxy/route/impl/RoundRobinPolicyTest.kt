package com.bbz.network.reverseproxy.route.impl

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.*
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

        var roundRobinPolicy = RoundRobinPolicy()
        val ctx: ChannelHandlerContext = object : ChannelHandlerContext {
            override fun writeAndFlush(msg: Any?, promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun writeAndFlush(msg: Any?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun alloc(): ByteBufAllocator {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireExceptionCaught(cause: Throwable?): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelInactive(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun newPromise(): ChannelPromise {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun write(msg: Any?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun write(msg: Any?, promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun <T : Any?> hasAttr(key: AttributeKey<T>?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun flush(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun connect(remoteAddress: SocketAddress?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun connect(remoteAddress: SocketAddress?, localAddress: SocketAddress?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun connect(remoteAddress: SocketAddress?, promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun connect(remoteAddress: SocketAddress?, localAddress: SocketAddress?, promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isRemoved(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun newFailedFuture(cause: Throwable?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun newSucceededFuture(): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun pipeline(): ChannelPipeline {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun close(): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun close(promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun read(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun voidPromise(): ChannelPromise {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun <T : Any?> attr(key: AttributeKey<T>?): Attribute<T> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun deregister(): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun deregister(promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireUserEventTriggered(evt: Any?): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelUnregistered(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun disconnect(): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun disconnect(promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun handler(): ChannelHandler {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun newProgressivePromise(): ChannelProgressivePromise {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun name(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelRegistered(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelRead(msg: Any?): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun executor(): EventExecutor {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun bind(localAddress: SocketAddress?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun bind(localAddress: SocketAddress?, promise: ChannelPromise?): ChannelFuture {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelReadComplete(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun channel(): Channel {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelActive(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun fireChannelWritabilityChanged(): ChannelHandlerContext {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
        repeat(7) {
            val url = roundRobinPolicy.getUrl(DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.CONNECT, ""),
                    ctx)
            println(url)
        }
    }
}