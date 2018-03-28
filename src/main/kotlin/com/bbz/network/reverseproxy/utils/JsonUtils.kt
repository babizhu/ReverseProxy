package com.bbz.network.reverseproxy.utils

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.nio.file.Files
import java.nio.file.Paths

class JsonUtils(path: String) {
    val json: JSONObject

    init {
        val content = String(Files.readAllBytes(Paths.get(path)))
        json = JSON.parseObject(content)
//        println(json)
    }

}
