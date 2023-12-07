package com.cerrebus.tokens.logger.impl

import android.util.Log
import com.cerrebus.tokens.logger.api.Logger
import kotlin.math.log

class LoggerImpl(tag: String): Logger {

    private val loggerTag = "$APP_TAG: $tag"
    override fun d(message: String) = Log.d(loggerTag, message)

    override fun i(message: String) = Log.i(loggerTag, message)

    override fun e(message: String) = Log.e(loggerTag, message)

    override fun w(message: String) = Log.w(loggerTag, message)

    companion object {
        const val APP_TAG = "TOKENS_APP"
    }
}