package com.bbz.network.reverseproxy.utils

import org.junit.Assert.*

import org.junit.Test

class PropertiesUtilTest {

    @Test
    fun getInt() {
        var propertiesUtil = PropertiesUtil("resources/backend_server.properties")
        var value = propertiesUtil.getInt("port")
        println(value)
        value = propertiesUtil.getInt("port",40)
        println(value)


    }
}