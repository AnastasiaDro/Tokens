package com.cerrebus.tokens.logger.impl

import com.cerrebus.tokens.logger.api.Logger
import com.cerrebus.tokens.logger.api.LoggerFactory

class LoggerFactoryImpl: LoggerFactory {
    override fun createLogger(tag: String) = LoggerImpl(tag)


}