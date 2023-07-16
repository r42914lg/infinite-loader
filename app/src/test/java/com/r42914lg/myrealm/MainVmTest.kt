package com.r42914lg.myrealm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.r42914lg.myrealm.data.*
import com.r42914lg.myrealm.domain.BasicLoader
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.Loader
import com.r42914lg.myrealm.ui.MainActivityEvent
import com.r42914lg.myrealm.ui.MainActivityVm
import com.r42914lg.myrealm.utils.ServiceLocator
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VmTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        ServiceLocator.register { TestApiImpl.getInstance() }
        ServiceLocator.register { RemoteDataSource.getInstance() }
        ServiceLocator.register { LocalRepositoryInMem.getInstance() }
        ServiceLocator.register { BasicLoader.getInstance() }
    }

    @Test
    fun `#1 - load 1st chunk - check items`() = runTest {

        val viewModel = MainActivityVm(
            ServiceLocator.resolve()
        )

        viewModel.itemState.test {
            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE),
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

        viewModel.itemState.test(20.seconds) {
            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 1st chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 2nd chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS,
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 3rd chunk (comes from remote)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS,
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS + REMOTE_ITEMS.subList(ITEMS_PER_PAGE * 2, ITEMS_PER_PAGE * 3).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // attempt to load 4rd chunk, but no data in remote --> state remains the same
            viewModel.onAction(MainActivityEvent.Load)
            expectNoEvents()

            cancel()
        }
    }

    @Test
    fun `#3 - load all three chunks - refresh - check 1st chunk only & comes from remote`() = runTest {

        val viewModel = MainActivityVm(
            ServiceLocator.resolve()
        )

        viewModel.itemState.test(20.seconds) {
            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 1st chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 2nd chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS,
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 3rd chunk (comes from remote)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS,
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS + REMOTE_ITEMS.subList(ITEMS_PER_PAGE * 2, ITEMS_PER_PAGE * 3).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // pull to refresh -> should produce ALL 3 chunks from remote
            viewModel.onAction(MainActivityEvent.PullToRefresh)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS + REMOTE_ITEMS.subList(ITEMS_PER_PAGE * 2, ITEMS_PER_PAGE * 3).toList(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = REMOTE_ITEMS.subList(ITEMS_PER_PAGE * 0, ITEMS_PER_PAGE * 1).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            cancel()
        }
    }

    @Test
    fun `#4 - load all three chunks - reset & reload - check 1st chunk only & comes from local`() = runTest {

        val viewModel = MainActivityVm(
            ServiceLocator.resolve()
        )

        viewModel.itemState.test(20.seconds) {
            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 1st chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 2nd chunk (comes from local)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(0, ITEMS_PER_PAGE).toList(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS,
                isLoading = false,
                isError = false,
            ), awaitItem())

            // load 3rd chunk (comes from remote)
            viewModel.onAction(MainActivityEvent.Load)

            assertEquals(Loader.State(
                data = LOCAL_ITEMS,
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS + REMOTE_ITEMS.subList(ITEMS_PER_PAGE * 2, ITEMS_PER_PAGE * 3).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            // pull to refresh -> should produce ALL 3 chunks from remote
            viewModel.onAction(MainActivityEvent.ResetAndLoad)

            assertEquals(Loader.State<List<Item>>(
                data = listOf(),
                isLoading = true,
                isError = false,
            ), awaitItem())

            assertEquals(Loader.State(
                data = LOCAL_ITEMS.subList(ITEMS_PER_PAGE * 0, ITEMS_PER_PAGE * 1).toList(),
                isLoading = false,
                isError = false,
            ), awaitItem())

            cancel()
        }
    }
}
