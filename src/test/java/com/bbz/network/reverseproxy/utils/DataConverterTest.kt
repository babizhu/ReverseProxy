package com.bbz.network.reverseproxy.utils

import org.junit.Assert.*

import org.junit.Test

class DataConverterTest {

    @Test
    fun ipToInt() {
        val ip = "192.168.123.45"
        var result = DataConverter.ipToInt(ip)
        println(result)
        assertEquals(result,763078848)
    }

    @Test
    fun intToIp() {
        val ip = 763078848
        var result = DataConverter.intToIp(ip)
        println(result)
        assertEquals(result,"192.168.123.45")

    }
}