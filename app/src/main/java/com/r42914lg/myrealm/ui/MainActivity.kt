package com.r42914lg.myrealm.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.r42914lg.myrealm.R
import com.r42914lg.myrealm.domain.Item
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityVm by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeToItems()
    }

    private fun subscribeToItems() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.itemState.collect {
                    if (it.isError) {
                        turnShimmer(false)
                        showErrorMsg()
                    } else if (it.isLoading) {
                        turnShimmer(true)
                    } else {
                        turnShimmer(false)
                        renderItems(it.data)
                    }
                }
            }
        }
    }

    private fun showErrorMsg() {}
    private fun turnShimmer(shimmerIsOn: Boolean) {}
    private fun renderItems(items: List<Item>) {}
}
