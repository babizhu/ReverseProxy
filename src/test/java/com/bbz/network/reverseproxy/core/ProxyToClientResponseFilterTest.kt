package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.core.filter.HttpFilterAdapter
import com.bbz.network.reverseproxy.utils.DataConverter
import com.bbz.network.reverseproxy.utils.ProxyUtils
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.junit.Test


class ProxyToClientResponseFilterTest : AbstractProxyTest() {
    override fun setUp() {
        val bootstrapProxy = super.bootstrapProxy()
        bootstrapProxy.withHttpFilter(object : HttpFilterAdapter() {
            override fun proxyToClientResponse(httpObject: HttpObject): HttpResponse? {
                if (httpObject is HttpResponse) {
                    httpObject.headers().add("server","liulaoye")
                    return ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.BAD_GATEWAY, "不允许访问")

//                    println(httpObject)
                }
                return null

            }
        })
        proxyServer = bootstrapProxy.start()
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
}

