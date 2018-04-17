package com.bbz.network.reverseproxy.utils

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.junit.Assert.assertEquals
import org.junit.Test

class DataConverterTest {

    @Test
    fun ipToInt() {
        val ip = "192.168.123.45"
        var result = DataConverter.ipToInt(ip)
        println(result)
        assertEquals(result, 763078848)
    }

    @Test
    fun intToIp() {
        val ip = 763078848
        var result = DataConverter.intToIp(ip)
        println(result)
        assertEquals(result, "192.168.123.45")

    }

    @Test
    fun toByteArray() {
        val i = -2
        var bytes = DataConverter.toByteArray(i)
        bytes.map { println(it) }
        var i1 = DataConverter.toInt(bytes)
        assertEquals(i, i1)
    }

    @Test
    fun toInt() {
        val ip = "192.168.123.45"
        var bytes = ip.split(".").reversed().map { it.toInt().toByte() }.toByteArray()
        println(ByteBufUtil.hexDump(bytes))
        println(DataConverter.toLong(bytes))
        println(DataConverter.toInt(bytes))

        println(DataConverter.ipToInt(ip))
    }


}