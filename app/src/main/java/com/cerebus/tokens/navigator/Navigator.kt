package com.cerebus.tokens.navigator

import androidx.appcompat.app.AppCompatActivity
import com.cerebus.tokens.presentation.MainActivity

/**
 * [Navigator] - an interface intended for screen navigation
 * It is using inside of [MainActivity]
 *
 * @see NavigatorImpl
 * @see MainActivity
 *
 * @author Anastasia Drogunova
 * @since 05.07.2023
 */
interface Navigator {

    var destination: Destinations
    fun navigate(destination: Destinations, activity: AppCompatActivity?, modifier: Modifiers = Modifiers.REPLACE_WITH_BACK)
}
