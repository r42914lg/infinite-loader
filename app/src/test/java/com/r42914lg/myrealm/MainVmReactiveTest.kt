package com.r42914lg.myrealm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.r42914lg.myrealm.data.*
import com.r42914lg.myrealm.domain.*
import com.r42914lg.myrealm.ui.MainActivityEvent
import com.r42914lg.myrealm.ui.MainActivityVm
import com.r42914lg.myrealm.utils.ServiceLocator
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VmReactiveTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        ServiceLocator.register { TestApiImpl.getInstance() }
        ServiceLocator.register { RemoteDataSource.getInstance() }
        ServiceLocator.register { ReactiveLocalRepoInMem.getInstance() }
        ServiceLocator.register { CacheFirstLoader.getInstance() }
    }

    @Test
    fun `#1 - load 1st chunk - check items`() = runTest {

        val viewModel = MainActivityVm(
            ServiceLocator.resolve()
        )

        viewModel.itemState.test {

            // isLoading == true (we are loading)
             assertEquals(Loader.State<List<Item>>(
                 data = listOf(),
                 isLoading = true,
                 isError = false,
             ), awaitItem())

            // isLoading == true (we are caching -> just skip)
            skipItems(1)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE),
                isLoading = false,
                isError = false,
            ), awaitItem())

            viewModel.onAction(MainActivityEvent.Load)

            // isLoading == true (we are loading)
            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE),
                isLoading = true,
                isError = false,
            ), awaitItem())

            skipItems(1)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE * 2),
                isLoading = false,
                isError = false,
            ), awaitItem())


            cancel()
        }
    }

    @Test
    fun `#2 - load all three chunks - load one more - check items only for 3 chunks`() = runTest {

        val viewModel = MainActivityVm(
            ServiceLocator.resolve()
        )

        viewModel.itemState.test {
            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            // isLoading == true (we are caching -> just skip)
            skipItems(1)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 2nd chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            // isLoading == true (we are caching -> just skip)
            skipItems(1)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE * 2).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 3rd chunk (comes from remote)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE * 2).toList(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            // isLoading == true (we are caching -> just skip)
            skipItems(1)

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(0, ITEMS_PER_PAGE * 3).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // attempt to load 4rd chunk, but no data in remote --> state remains the same
            viewModel.onAction(MainActivityEvent.Load)
            expectNoEvents()

            cancel()
        }
    }
}

