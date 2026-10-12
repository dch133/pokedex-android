/*
 * Designed and developed by 2024 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.compose.feature.home

import androidx.paging.PagingData
import app.cash.turbine.test
import com.skydoves.pokedex.compose.core.data.repository.home.HomeRepository
import com.skydoves.pokedex.compose.core.model.Pokemon
import com.skydoves.pokedex.compose.core.model.PokemonRegion
import com.skydoves.pokedex.compose.core.test.MainCoroutinesRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HomeViewModelTest {

  private lateinit var viewModel: HomeViewModel
  private val homeRepository: HomeRepository = mock()

  @get:Rule
  val coroutinesRule = MainCoroutinesRule()

  @Before
  fun setup() {
    whenever(homeRepository.fetchPokemonList(any(), any(), any(), any())).thenReturn(
      flowOf(PagingData.empty()),
    )
    viewModel = HomeViewModel(homeRepository)
  }

  @Test
  fun `regionState should emit KANTO as initial state`() = runTest {
    viewModel.regionState.test {
      assertEquals(PokemonRegion.KANTO, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `fetchByRegion should update regionState`() = runTest {
    viewModel.regionState.test {
      assertEquals(PokemonRegion.KANTO, awaitItem())

      viewModel.fetchByRegion(PokemonRegion.JOHTO)
      assertEquals(PokemonRegion.JOHTO, awaitItem())

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `uiState should emit Idle as initial state`() = runTest {
    viewModel.uiState.test {
      assertEquals(HomeUiState.Idle, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `uiState should emit Error when repository returns an error`() = runTest {
    val errorMessage = "Internal Server Error"

    // 1. We mock the repository to invoke the 4th argument (index 3) which is 'onError'
    whenever(homeRepository.fetchPokemonList(any(), any(), any(), any()))
      .thenAnswer { invocation ->
        val onError = invocation.getArgument<(String?) -> Unit>(3)
        onError(errorMessage) // Trigger the error callback
        flowOf(PagingData.empty<Pokemon>()) // Still need to return a flow to satisfy the signature
      }

    viewModel.uiState.test {
      // 2. First item is always the initial state (Idle)
      assertEquals(HomeUiState.Idle, awaitItem())

      // 3. We MUST collect the paging data to trigger the flatMapLatest inside the ViewModel
      val job = launch {
        viewModel.pokemonPagingData.collect {}
      }

      // 4. Now await the Error state
      val state = awaitItem()
      assert(state is HomeUiState.Error)
      assertEquals(errorMessage, (state as HomeUiState.Error).message)

      job.cancel() // Clean up the collection
    }
  }
}
