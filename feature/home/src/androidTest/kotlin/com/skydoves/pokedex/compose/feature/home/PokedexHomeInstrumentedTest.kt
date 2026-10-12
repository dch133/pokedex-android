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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skydoves.pokedex.compose.core.designsystem.theme.PokedexTheme
import com.skydoves.pokedex.compose.core.navigation.LocalComposeNavigator
import com.skydoves.pokedex.compose.core.navigation.PokedexNavigator
import com.skydoves.pokedex.compose.core.navigation.PokedexScreen
import com.skydoves.pokedex.compose.core.test.MockUtil
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A simple Fake implementation of the Navigator to avoid Mockito proxying issues on devices.
 */
class FakePokedexNavigator : PokedexNavigator {
  var lastNavigatedScreen: PokedexScreen? = null
    private set

  override fun navigate(screen: PokedexScreen) {
    lastNavigatedScreen = screen
  }

  override fun navigateUp(): Boolean = false
}

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class PokedexHomeInstrumentedTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun pokedexHome_loading_isDisplayed() {
    composeTestRule.setContent {
      val pokemonPagingItems =
        flowOf(PagingData.from(emptyList<com.skydoves.pokedex.compose.core.model.Pokemon>()))
          .collectAsLazyPagingItems()

      PokedexTheme {
        CompositionLocalProvider(LocalComposeNavigator provides FakePokedexNavigator()) {
          SharedTransitionLayout {
            AnimatedContent(targetState = HomeUiState.Loading, label = "loading") { targetState ->
              PokedexHome(
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedContentScope = this,
                uiState = targetState,
                pokemonPagingItems = pokemonPagingItems,
                onFetchByRegion = {}
              )
            }
          }
        }
      }
    }

    composeTestRule.onNodeWithTag("PokedexCircularProgress").assertIsDisplayed()
  }

  @Test
  fun pokedexHome_pokemonCard_onClick_navigatesToDetails() {
    val mockPokemonList = MockUtil.mockPokemonList()
    val fakeNavigator = FakePokedexNavigator()

    composeTestRule.setContent {
      val pokemonPagingItems = flowOf(PagingData.from(mockPokemonList))
        .collectAsLazyPagingItems()

      PokedexTheme {
        CompositionLocalProvider(LocalComposeNavigator provides fakeNavigator) {
          SharedTransitionLayout {
            AnimatedContent(targetState = HomeUiState.Idle, label = "interaction") { targetState ->
              PokedexHome(
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedContentScope = this,
                uiState = targetState,
                pokemonPagingItems = pokemonPagingItems,
                onFetchByRegion = {}
              )
            }
          }
        }
      }
    }

    // 1. Find the pokemon card and click it
    composeTestRule.onNodeWithTag("Pokemon").performClick()

    // 2. Verify navigation occurred by checking the Fake's state
    val expectedPokemon = mockPokemonList[0]
    assertEquals(PokedexScreen.Details(expectedPokemon), fakeNavigator.lastNavigatedScreen)
  }
}
