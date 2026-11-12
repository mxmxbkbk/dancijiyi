package com.dancijiyi

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar

/**
 * 单 Activity 容器：承载 Navigation 图与统一 AppBar。
 * 页面内只做导航与 UI 状态展示，业务逻辑留给后续 ViewModel/Repository。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(setOf(R.id.wordListFragment))

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        setupActionBarWithNavController(navController, appBarConfiguration)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_word_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val onWordList = navController.currentDestination?.id == R.id.wordListFragment
        menu.findItem(R.id.action_import)?.isVisible = onWordList
        menu.findItem(R.id.action_review)?.isVisible = onWordList
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_import -> {
            navController.navigate(R.id.action_wordList_to_import)
            true
        }

        R.id.action_review -> {
            navController.navigate(R.id.action_wordList_to_review)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
}
