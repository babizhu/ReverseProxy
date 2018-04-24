package com.bbz.network.reverseproxy.core.filter.impl

import com.bbz.network.reverseproxy.utils.DataConverter
import com.bbz.network.reverseproxy.utils.JsonUtils
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.junit.Test
import kotlin.experimental.and
import kotlin.experimental.xor
import kotlin.test.assertEquals

class BlackListFilterTest {

    @Test
    fun init() {
        var blackListFilter = BlackListFilter()
        blackListFilter.init()
        val config = readConfig()
        for (ip in config) {
            var temp = DataConverter.ipToInt(ip)
            println("$ip($temp)")
            assertEquals(blackListFilter.blackList.contains(temp), true)
        }


    }

    /**
     * read config and convert to hashset
     */
    private fun readConfig(): HashSet<String> {
        val config = JsonUtils("resources/ip_blacklist.json")
        val blackList = config.json.getJSONArray("blacklist")
        return blackList.map { it.toString() }.toHashSet()

    }

    @Test
    fun d() {
        var str = "192.168.1.123"
//        str.split(".").map { println(it.toInt().toByte().toString(2)) }
        var byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.124"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.125"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.126"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.127"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.128"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.133"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()
        println("$str::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")


        byteArray = DataConverter.toByteArray(Int.MAX_VALUE)
        println("${Int.MAX_VALUE}::${DataConverter.toInt(byteArray)}::${ByteBufUtil.hexDump(byteArray)}")

        str = "192.168.1.128"
        byteArray = str.split(".").map { it.toInt().toByte() }.toByteArray()

        println(DataConverter.toInt(byteArray) + Math.abs(Int.MIN_VALUE.toLong())*2)
        println("${DataConverter.toLong(byteArray)}")


    }
}