package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.core.filter.HttpFilterAdapter
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.junit.Test


class HttpFilterTest : AbstractProxyTest() {
    override fun setUp() {
        val bootstrapProxy = super.bootstrapProxy()
        bootstrapProxy.withHttpFilter(object : HttpFilterAdapter() {
            override fun clientToProxyRequest(httpObject: HttpObject): HttpResponse? {
                if (httpObject is HttpRequest) {
                    httpObject.uri = "/test"
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
        val response1 = httpclient.execute(httpGet)
// The underlying HTTP connection is still held by the response object
// to allow the response content to be streamed directly from the network socket.
// In order to ensure correct deallocation of system resources
// the user MUST call CloseableHttpResponse#close() from a finally clause.
// Please note that if response content is not fully consumed the underlying
// connection cannot be safely re-used and will be shut down and discarded
// by the connection manager.
        response1.use { response1 ->
            println(response1.statusLine)
            val entity1 = response1.entity
            // do something useful with the response body
            // and ensure it is fully consumed
            println(EntityUtils.toString(response1.entity))
            EntityUtils.consume(entity1)
        }
    }

    @Test
    fun clientToProxyConnectedTest() {

    }
}