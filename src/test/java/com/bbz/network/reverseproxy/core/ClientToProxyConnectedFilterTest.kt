package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.core.filter.HttpFilterAdapter
import com.bbz.network.reverseproxy.utils.DataConverter
import com.bbz.network.reverseproxy.utils.ProxyUtils
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.junit.Test
import java.net.InetSocketAddress


class ClientToProxyConnectedFilterTest : AbstractProxyTest() {
    override fun setUp() {
        val bootstrapProxy = super.bootstrapProxy()
        bootstrapProxy.withHttpFilter(object : HttpFilterAdapter() {
            /**
             * 被屏蔽ip地址列表
             */
            private var blackList = HashSet<Int>()

            override fun clientToProxyConnected(ctx: ChannelHandlerContext): HttpResponse? {
                println(ctx.channel().remoteAddress())
                val bytes = (ctx.channel().remoteAddress() as InetSocketAddress).address.address
                val ip = DataConverter.toInt(bytes)
                return if (blackList.contains(ip)) {
                    ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.BAD_GATEWAY, "IP禁止")
                } else {
                    null
                }
            }

            override fun init() {
                val blackListArray: Array<String> = arrayOf("192.168.1.1", "224.156.78.12", "1.2.3.4")
                blackList = blackListArray.map { DataConverter.ipToInt(it) }.toHashSet()

            }
        })
        proxyServer = bootstrapProxy.start()
    }

    @Test
    fun test() {
        val blackListArray: Array<String> = arrayOf("192.168.134.156", "224.156.78.12", "1.2.3.4")
        blackListArray.map {
            val list = it.split(".").map { it.toInt().toByte() }.toByteArray()
            println("$it:${DataConverter.toInt(list)}")
        }

        val i = -1668896576
        var toByteArray = DataConverter.toByteArray(i)
        for (b in toByteArray) {
            print(b)
            if (b < 0) {
                print("[${b + 256}]，")
            } else {
                print(",")
            }
        }
    }

    /**
     * 1、测试http filter
     */
    @Test
    fun filterTest() {
        val httpclient = HttpClients.createDefault()
        val httpGet = HttpGet("http://localhost:${proxyServer!!.getListenAddress().port}/")
        val response = httpclient.execute(httpGet)
        response.use { response1 ->
            println(response1.statusLine)
            val entity1 = response1.entity
            // do something useful with the response body
            // and ensure it is fully consumed
            println(EntityUtils.toString(response1.entity))
            EntityUtils.consume(entity1)
        }
        Thread.sleep(1000000)
    }

    @Test
    fun testIntConvertIp() {
        for (it in Int.MIN_VALUE..Int.MAX_VALUE) {
            if (it % 1000000 == 0) {
                println("$it done ")
            }
            val toByteArray = DataConverter.toByteArray(it)
            var toInt = DataConverter.toInt(toByteArray)
            if (it != toInt) {
                throw Exception()
            }
        }
        println("all done!")
    }

}

