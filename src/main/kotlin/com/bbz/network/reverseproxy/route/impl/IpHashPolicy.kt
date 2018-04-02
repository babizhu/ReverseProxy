package com.bbz.network.reverseproxy.route.impl

import com.alibaba.fastjson.JSONObject
import com.bbz.network.reverseproxy.pojo.BackendServer
import com.bbz.network.reverseproxy.route.RoutePolicy
import com.bbz.network.reverseproxy.utils.JsonUtils
import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest
import java.net.InetSocketAddress

@Suppress("unused")
/**
 * ip hash 策略
 * 来自nginx
 * https://github.com/nginx/nginx/blob/release-1.13.10/src/http/modules/ngx_http_upstream_ip_hash_module.c
 * 注意：
 * 在IPV4的情况下 ，为了性能仅取ip地址的前三段进行hash，这样测试的时候同内网的ip很可能会被hash到同一台服务器上
 *
 * IPV6，则需要重新处理，同样可以参考nginx代码
 */
class IpHashPolicy : RoutePolicy {

    private val backendServer = arrayListOf<BackendServer>()

    init {
        val config = JsonUtils("resources/backend_server.json")
        val servers = config.json.getJSONArray("backend_servers")
        for (server in servers) {
            backendServer.add(BackendServer.create(server as JSONObject))
        }
    }

    override fun getBackendServerAddress(request: HttpRequest, channel: Channel): InetSocketAddress {
        val ip = (channel.remoteAddress() as InetSocketAddress)
        val index = hash(ip)
        return backendServer[index].address
    }

    /**
     * nginx的hash算法
     */
    private fun hash(ip:InetSocketAddress):Int{
        val address = ip.address.address
        var hash = 89
        repeat(3) {
            hash = (hash * 113 + address[it]) % 6271
        }
        hash %= backendServer.size
        return hash
    }
}
