package com.cerebus.tokens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cerebus.tokens.navigator.Destinations
import com.cerebus.tokens.navigator.Modifiers
import com.cerebus.tokens.navigator.Navigator
import com.cerebus.tokens.navigator.NavigatorImpl


class MainActivity : AppCompatActivity() {
    private val navigator: Navigator = NavigatorImpl()
    fun getNavigator() = navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        navigator.navigate(Destinations.TOKENS_FRAGMENT, this, Modifiers.REPLACE)
    }
}