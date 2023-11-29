package com.cerebus.tokens.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cerebus.tokens.R


/**
 * [MainActivity]
 * It contains a nav_container for all fragments
 *
 * @see R.layout.activity_main
 * @see R.navigation.nav_graph
 *
 * @author Anastasia Drogunova
 * @since last update: 29.11.2023
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
