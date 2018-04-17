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
        return source[3].toInt() shl 24 or (source[2].toInt() and 0xFF shl 16) or (source[1].toInt() and 0xFF shl 8) or (source[0].toInt() and 0xFF)
    }

    /**
     * 这个方案先凑合，以后再调整
     */
    fun toLong(byteArray: ByteArray): Long {
        return toInt(byteArray) + Math.abs(Int.MIN_VALUE.toLong()) * 2
    }


    /**
     * 把点分格式的ip转换为4字节Int,注意使用了reversed方法
     * 把192.168.1.1转换为byte数组的时候，从人的角度192是高位，并且在数组中的index为0，
     * 但从toInt函数的角度，高位在数组中的index应该是3，因此reversed一下
     */
    fun ipToInt(ip: String): Int {
        return toInt(ip.split(".").reversed().map { it.toInt().toByte() }.toByteArray())
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