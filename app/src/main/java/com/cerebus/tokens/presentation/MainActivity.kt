package com.cerebus.tokens.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.cerebus.tokens.R
import com.cerebus.tokens.navigator.Destinations
import com.cerebus.tokens.navigator.Modifiers
import com.cerebus.tokens.navigator.Navigator
import com.cerebus.tokens.navigator.NavigatorImpl


class MainActivity : AppCompatActivity() {
//    private val navigator: Navigator = NavigatorImpl()
//    fun getNavigator() = navigator
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

       // navigator.navigate(Destinations.TOKENS_FRAGMENT, this, Modifiers.REPLACE)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
        navController = navHostFragment.navController
        NavigationUI.setupActionBarWithNavController(this, navController)

    }
}