package com.bbz.network.reverseproxy.pojo

import com.alibaba.fastjson.JSONObject
import java.net.InetSocketAddress

open class BackendServer(
        /**
         * ip地址
         */
        val ip: String,

        val port: Int,
        /**
         * 权重
         */
        val weight: Int,
        val down:Boolean

) {
    val address: InetSocketAddress = InetSocketAddress(ip, port)
    var fails = 0

    companion object {
        fun create(json: JSONObject):BackendServer {
            return BackendServer(json.getString("ip"),
                    json.getIntValue("port"),
                    json.getIntValue("weight"),
                    json.getBoolean("down"))
        }
    }

}