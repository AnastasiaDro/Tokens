package com.cerebus.tokens.logger.api

interface LoggerFactory {

    fun createLogger(tag: String): Logger
}