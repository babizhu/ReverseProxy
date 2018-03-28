package com.bbz.network.reverseproxy.route.impl

import com.alibaba.fastjson.JSONObject
import com.bbz.network.reverseproxy.pojo.BackendServer
import com.bbz.network.reverseproxy.route.RoutePolicy
import com.bbz.network.reverseproxy.utils.JsonUtils
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * 轮训算法，来自nginx
 * https://github.com/nginx/nginx/blob/release-1.13.10/src/http/ngx_http_upstream_round_robin.c
 */

private class RoundRobinBackendServer(ip: String,
                                      port: Int,
                                      weight: Int,
                                      down: Boolean) : BackendServer(ip, port, weight, down) {


    var accessed = 0L /* 最近一次成功访问的时间点 */
//        val checked: Long, /* 用于检查是否超过了"一段时间" */
//
//ngx_uint_t max_fails; /* "一段时间内"，最大的失败次数，固定值 */
//time_t fail_timeout; /* "一段时间"的值，固定值 */

    var currentWeight = 0//当前权重
    var effectiveWeight = weight //有效权重
    var conns = 0 //当前连接数

    companion object {

        fun create(json: JSONObject): RoundRobinBackendServer {
            return RoundRobinBackendServer(json.getString("ip"),
                    json.getIntValue("port"),
                    json.getIntValue("weight"),
                    json.getBoolean("down"))
        }
    }

}

class RoundRobinPolicy : RoutePolicy {
    private val backendServer = arrayListOf<RoundRobinBackendServer>()

    companion object {
        private val log = LoggerFactory.getLogger(RoundRobinPolicy::class.java)

    }

    init {
        val config = JsonUtils("resources/backend_server.json")
        val servers = config.json.getJSONArray("backend_servers")
        for (server in servers) {
            backendServer.add(RoundRobinBackendServer.create(server as JSONObject))
        }
    }

    override fun getUrl(request: HttpRequest, ctx: ChannelHandlerContext): InetSocketAddress {
        var server = roundRobinPolicy()
        if (server == null) {
            log.error("无法找到合适的服务器")
            throw Exception("无法找到合适的服务器")
        }
        return server.address
    }

    /**
     * nginx的round robin算法
     */
    private fun roundRobinPolicy(): RoundRobinBackendServer? {
//        peer->current_weight += peer->effecitve_weight。
//
//        同时累加所有peer的effective_weight，保存为total。
        var best: RoundRobinBackendServer? = null
        var total = 0
        for (peer in backendServer) {
            if (peer.down) {
                continue
            }
            peer.currentWeight += peer.effectiveWeight
            total += peer.effectiveWeight

            if (peer.effectiveWeight < peer.weight) {
                peer.effectiveWeight++
            }
            if (best == null || peer.currentWeight > best.currentWeight) {
                best = peer
            }
        }
        if (best == null) {
            return null
        }

        best.currentWeight -= total
        best.accessed = System.currentTimeMillis()
        return best
    }
}