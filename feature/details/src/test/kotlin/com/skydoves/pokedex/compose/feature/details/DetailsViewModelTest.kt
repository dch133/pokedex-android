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

package com.skydoves.pokedex.compose.feature.details

import app.cash.turbine.test
import com.skydoves.pokedex.compose.core.data.repository.details.DetailsRepository
import com.skydoves.pokedex.compose.core.test.MainCoroutinesRule
import com.skydoves.pokedex.compose.core.test.MockUtil
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DetailsViewModelTest {

  private lateinit var viewModel: DetailsViewModel
  private val detailsRepository: DetailsRepository = mock()
  private val pokemon = MockUtil.mockPokemon()

  @get:Rule
  val coroutinesRule = MainCoroutinesRule()

  @Before
  fun setup() {
    whenever(detailsRepository.fetchPokemonInfo(any(), any(), any())).thenReturn(
      flowOf(MockUtil.mockPokemonInfo()),
    )
    viewModel = DetailsViewModel(pokemon, detailsRepository)
  }

  @Test
  fun `uiState should emit Idle when data is loaded successfully`() = runTest {
    viewModel.uiState.test {
      assertEquals(DetailsUiState.Loading, awaitItem())

      // Trigger collection of pokemonInfo flow
      viewModel.pokemonInfo.test {
        assertEquals(MockUtil.mockPokemonInfo(), awaitItem())
      }

      assertEquals(DetailsUiState.Idle, awaitItem())
    }

    verify(detailsRepository, atLeastOnce()).fetchPokemonInfo(any(), any(), any())
  }

  @Test
  fun `pokemonInfo should emit correct data`() = runTest {
    viewModel.pokemonInfo.test {
      assertEquals(null, awaitItem()) // Initial value
      assertEquals(MockUtil.mockPokemonInfo(), awaitItem())
    }
  }
}
