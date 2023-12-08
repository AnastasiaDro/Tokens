package com.cerebus.tokens.logger.impl

import com.cerebus.tokens.logger.api.LoggerFactory

class LoggerFactoryImpl: LoggerFactory {
    override fun createLogger(tag: String) = LoggerImpl(tag)
}