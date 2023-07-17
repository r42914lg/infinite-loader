package com.r42914lg.myrealm.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AbsListView
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.r42914lg.myrealm.R
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.ui.feed.ItemAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityVm by viewModels { MainActivityVm.Factory }

    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var btnRefresh: Button
    private lateinit var btnReset: Button

    private val adapter = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feedRecyclerView = findViewById(R.id.feed)
        feedRecyclerView.layoutManager = LinearLayoutManager(this)
        feedRecyclerView.adapter = adapter
        feedRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisibleItemIndex = (feedRecyclerView.layoutManager as LinearLayoutManager)
                    .findLastVisibleItemPosition()

                if (adapter.itemCount - lastVisibleItemIndex <= THRESHOLD) {
                    viewModel.onAction(MainActivityEvent.Load)
                }
            }
        })

        btnRefresh = findViewById(R.id.btn_refresh)
        btnReset = findViewById(R.id.btn_reset)

        subscribeToItems()
        setListeners()
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

    private fun setListeners() {
        btnRefresh.setOnClickListener {
            viewModel.onAction(MainActivityEvent.PullToRefresh)
        }

        btnReset.setOnClickListener {
            viewModel.onAction(MainActivityEvent.ResetAndLoad)
        }
    }

    private fun showErrorMsg() {
        Toast.makeText(this,"Error while loading...", Toast.LENGTH_LONG).show()
    }
    private fun turnShimmer(shimmerIsOn: Boolean) {
        Toast.makeText(
            this,
            "Item feed load ${if (shimmerIsOn) "started" else "finished"}",
            Toast.LENGTH_LONG).show()
    }
    private fun renderItems(items: List<Item>) {
        adapter.setItems(items)
    }

    companion object {
        const val THRESHOLD = 1
    }
}
