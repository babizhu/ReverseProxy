package com.bbz.network.reverseproxy.core.misc

enum class ErrorCode(val code: Int, var msg: String) {


    SUCCESS(0, "success"),

    SYSTEM_ERROR(1, "system error"),
    DECODE_FAILURE(999, "DECODE_FAILURE"),

    BACKEND_SERVER_NOT_FOUND(1000, "BACKEND_SERVER_NOT_FOUND"),

}

