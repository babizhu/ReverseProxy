package com.bbz.network.reverseproxy.utils

import java.io.FileInputStream
import java.util.*

class PropertiesUtil( path: String) {

    private val pps = Properties()

    init {
        var fileInputStream = FileInputStream(path)
        pps.load(fileInputStream)
        fileInputStream.close()

    }

    fun getInt(key: String, defaultValue: Int? = null): Int? {
        var value = pps.getProperty(key)
        if (value != null) {
            return value.toInt()
        }else{
            return defaultValue
        }

    }
}