package com.cerrebus.tokens.logger.api

interface LoggerFactory {

    fun createLogger(tag: String): Logger
}