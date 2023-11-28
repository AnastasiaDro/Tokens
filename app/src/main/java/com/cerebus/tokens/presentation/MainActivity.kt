package com.cerebus.tokens.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.cerebus.tokens.R


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
        navController = navHostFragment.navController

    }

    override fun onSupportNavigateUp(): Boolean {
        println("Настя onSupportNavigateUp()")
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
