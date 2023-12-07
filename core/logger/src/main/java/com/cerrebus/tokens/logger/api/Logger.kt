package com.cerrebus.tokens.logger.api

/**
 * [Logger] - an interface for logging realisation
 *
 * @see
 *
 * @author Anastasia Drogunova
 * @since 05.12.2023
 */
interface Logger {

    /** Sending debug message **/
    fun d(message: String): Int

    /** Sending info message **/
    fun i(message: String): Int

    /** Sending error message **/
    fun e(message: String): Int

    /** Sending warning message **/
    fun w(message: String): Int
}