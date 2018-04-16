package com.bbz.network.reverseproxy.utils

object DataConverter {
    // 有符号

    //    fun convertTwoUnSignInt(byteArray: ByteArray): Int =
//            (byteArray[3].toInt() shl 24) or (byteArray[2].toInt() and 0xFF) or (byteArray[1].toInt() shl 8) or (byteArray[0].toInt() and 0xFF)
    fun toByteArray(source: Int): ByteArray {
        var target = byteArrayOf(0, 0, 0, 0)
        var index = 0
        while (index < 4) {
            target[index] = (source.shr(8 * index) and 0xff).toByte()
            index++
        }
        return target
    }

    /**
     * 数组byte转int数据
     */
    fun toInt(source: ByteArray): Int {
        return source[0].toInt() shl 24 or (source[1].toInt() and 0xFF shl 16) or (source[2].toInt() and 0xFF shl 8) or (source[3].toInt() and 0xFF)
    }

    /**
     * 这个方案先凑合，以后再调整
     */
    fun toLong(byteArray: ByteArray): Long {
        return toInt(byteArray) + Math.abs(Int.MIN_VALUE.toLong()) * 2
    }


    /**
     * 把点分格式的ip转换为4字节Int
     */
    fun ipToInt(ip: String): Int {
        return toInt(ip.split(".").map { it.toInt().toByte() }.toByteArray())
    }

    /**
     * 把4字节Int转换为点分格式的ip
     */
    fun intToIp(ip: Int): String {
        val bytes = toByteArray(ip)
        var builder = StringBuilder()
        for (byte in bytes) {
            if (byte < 0) {
                builder.append(byte + 256)
            } else {
                builder.append(byte)
            }
            builder.append(".")
        }
        return builder.substring(0, builder.length - 1)
    }
}