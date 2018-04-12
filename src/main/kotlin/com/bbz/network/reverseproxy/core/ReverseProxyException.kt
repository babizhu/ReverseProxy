package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.core.misc.ErrorCode

class ReverseProxyException(val errorCode: ErrorCode, message: String) : RuntimeException(message) {
    constructor(errorCode: ErrorCode) : this(errorCode, "")
}

fun main(args: Array<String>) {
    ReverseProxyException(ErrorCode.BACKEND_SERVER_NOT_FOUND,"abcd")
    ReverseProxyException(ErrorCode.BACKEND_SERVER_NOT_FOUND)


}
