package com.bbz.network.reverseproxy.route.impl

import org.junit.Test
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.util.*

class IpHashPolicyTest {

    @Test
    fun getUrl() {
        val count = 6
        println("192.168.1.20:${"192.168.1.20".hashCode()}:${"192.168.1.20".hashCode() % count}")
        println("192.168.1.21:${"192.168.1.21".hashCode()}:${"192.168.1.21".hashCode() % count}")
        println("192.168.1.22:${"192.168.1.22".hashCode()}:${"192.168.1.22".hashCode() % count}")
        println("192.168.1.23:${"192.168.1.23".hashCode()}:${"192.168.1.23".hashCode() % count}")
        println("192.168.1.24:${"192.168.1.24".hashCode()}:${"192.168.1.24".hashCode() % count}")
        println("192.168.1.25:${"192.168.1.25".hashCode()}:${"192.168.1.25".hashCode() % count}")
        println("192.168.1.26:${"192.168.1.26".hashCode()}:${"192.168.1.26".hashCode() % count}")
        println("192.168.1.28:${"192.168.1.27".hashCode()}:${"192.168.1.27".hashCode() % count}")
        println("192.168.1.28:${"192.168.1.28".hashCode()}:${"192.168.1.28".hashCode() % count}")
        println("192.168.1.29:${"192.168.1.29".hashCode()}:${"192.168.1.29".hashCode() % count}")
        println("192.168.1.30:${"192.168.1.30".hashCode()}:${"192.168.1.30".hashCode() % count}")
        println("221.23.45.124:${"221.23.45.12".hashCode()}:${"221.23.45.124".hashCode() % count}")

        var address = InetSocketAddress("221.23.45.12", 8000)
        var address1 = InetSocketAddress("221.23.45.12", 8080)
        println(address.hashCode())
        println(address1.hashCode())
        println(address.hostName)
        println(address.hostString)
        println(address.address.hostAddress)


    }

    /**
     * 测试nginx的iphash算法
     */
    @Test
    fun testNginxIPHash() {
        val pps = Properties()
        pps.load(FileInputStream("resources/backend_server.properties"))

//        pps.get()

        val per = 0.5f //阈值，后端server命中个数与平均值偏离超过该比例则输出相关信息
        var random = Random()
        val peerNumber = 100//随机产生后端server节点数

//        val peerNumber = random.nextInt(6271) + 1//随机产生后端server节点数
        var result = IntArray(peerNumber)


        val totalNum = 1000000  //进行hash的总次数
//        int total_num_temp = total_num;
        repeat(totalNum) {
            val ipFiledCount = 4
            var ip = IntArray(ipFiledCount)
            repeat(ipFiledCount) {
                ip[it] = random.nextInt(255)//随机生成四个数作为ip地址前四段
            }
//            print(ip.map { it })
//            print("   ")
            var hash = 89
            repeat(3) {
                hash = (hash * 113 + ip[it]) % 6271
            }
            hash %= peerNumber
            result[hash]++//统计hash值命中
//            println(hash)

        }
        val avg = totalNum / peerNumber
        val max = (avg.toFloat() * (1 + per)).toInt()
        val min = (avg.toFloat() * (1 - per)).toInt()
        repeat(peerNumber) {
            //            if (result[it] > max || result[it] < min){
//                println("$it:${result[it]}")
//            }
            println("$it:${result[it]}")


        }
        println("avg:$avg\tmin:$min\tmax:$max")


    }
}