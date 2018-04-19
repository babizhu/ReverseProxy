package com.bbz.network.reverseproxy.core.filter.impl

import com.bbz.network.reverseproxy.core.DefaultReverseProxyServer
import com.bbz.network.reverseproxy.core.filter.HttpFilterAdapter
import com.bbz.network.reverseproxy.route.RoutePolicy
import com.bbz.network.reverseproxy.utils.DataConverter
import com.bbz.network.reverseproxy.utils.JsonUtils
import com.bbz.network.reverseproxy.utils.ProxyUtils
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import java.net.InetSocketAddress

class BlackListFilter : HttpFilterAdapter() {
    /**
     * 被屏蔽ip地址列表,ip转换为Int型
     */
    internal var blackList = HashSet<Int>()

    override fun clientToProxyConnected(ctx: ChannelHandlerContext): HttpResponse? {
        val bytes = (ctx.channel().remoteAddress() as InetSocketAddress).address.address
        val ip = DataConverter.toInt(bytes)
        return if (blackList.contains(ip)) {
            ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.BAD_GATEWAY, "IP禁止")
        } else {
            null
        }
    }


    override fun init() {
        val config = JsonUtils("resources/ip_blacklist.json").json.getJSONArray("blacklist")
        for (ip in config) {
            blackList.add(DataConverter.ipToInt(ip.toString()))
        }
    }
}

fun main(args: Array<String>) {


    val bootstrap = DefaultReverseProxyServer.bootstrap()
            .withRoutePolice(object : RoutePolicy {
                override fun getBackendServerAddress(request: HttpRequest, channel: Channel): InetSocketAddress? {

                    return when (request.uri()) {
                        "user" -> InetSocketAddress("user.api.com", 80)
                        "prouduct" -> InetSocketAddress("product.api.com", 80)
                        else -> InetSocketAddress("else.api.com", 80)
                    }
                }
            })
            .withPort(8000)
            .withHttpFilter(BlackListFilter())
    bootstrap.start()

}