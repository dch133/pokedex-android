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

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kmpalette.palette.graphics.Palette
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import com.skydoves.pokedex.compose.core.data.sync.Sync
import com.skydoves.pokedex.compose.core.designsystem.component.PokedexAppBar
import com.skydoves.pokedex.compose.core.designsystem.component.PokedexCircularProgress
import com.skydoves.pokedex.compose.core.designsystem.theme.PokedexTheme
import com.skydoves.pokedex.compose.core.model.LocalOnRegionSelected
import com.skydoves.pokedex.compose.core.model.LocalSelectedRegion
import com.skydoves.pokedex.compose.core.model.Pokemon
import com.skydoves.pokedex.compose.core.model.PokemonRegion
import com.skydoves.pokedex.compose.core.navigation.PokedexScreen
import com.skydoves.pokedex.compose.core.navigation.currentComposeNavigator
import com.skydoves.pokedex.compose.core.preview.PokedexPreviewTheme
import com.skydoves.pokedex.compose.core.preview.PreviewUtils
import kotlinx.coroutines.flow.flowOf

@Composable
fun PokedexHome(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  homeViewModel: HomeViewModel = hiltViewModel(),
) {
  val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
  val pokemonPagingItems = homeViewModel.pokemonPagingData.collectAsLazyPagingItems()

  PokedexHome(
    sharedTransitionScope = sharedTransitionScope,
    animatedContentScope = animatedContentScope,
    uiState = uiState,
    pokemonPagingItems = pokemonPagingItems,
    onFetchByRegion = homeViewModel::fetchByRegion,
  )
}

@Composable
internal fun PokedexHome(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  uiState: HomeUiState,
  pokemonPagingItems: LazyPagingItems<Pokemon>,
  onFetchByRegion: (PokemonRegion) -> Unit,
) {
  val composeNavigator = currentComposeNavigator
  val selectedRegion = LocalSelectedRegion.current
  val onRegionSelected = LocalOnRegionSelected.current

  // Sync the global region state with the ViewModel whenever it changes.
  LaunchedEffect(selectedRegion) {
    onFetchByRegion(selectedRegion)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    PokedexAppBar(
      onActionClick = { composeNavigator.navigate(PokedexScreen.Settings) }
    )

    RegionSelectionRow(
      selectedRegion = selectedRegion,
      onRegionSelected = onRegionSelected
    )

    SyncProgressIndicator()

    HomeContent(
      sharedTransitionScope = sharedTransitionScope,
      animatedContentScope = animatedContentScope,
      uiState = uiState,
      pokemonPagingItems = pokemonPagingItems,
      navigateToDetails = { composeNavigator.navigate(PokedexScreen.Details(pokemon = it)) }
    )
  }
}

@Composable
private fun SyncProgressIndicator() {
  val context = LocalContext.current
  val isSyncing by Sync.isSyncing(context).collectAsStateWithLifecycle(initialValue = false)

  if (isSyncing) {
    LinearProgressIndicator(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      color = PokedexTheme.colors.primary,
      trackColor = PokedexTheme.colors.primary.copy(alpha = 0.2f)
    )
  }
}

@Composable
private fun RegionSelectionRow(
  selectedRegion: PokemonRegion,
  onRegionSelected: (PokemonRegion) -> Unit
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
  ) {
    items(PokemonRegion.entries.filter { it != PokemonRegion.UNKNOWN }) { region ->
      FilterChip(
        selected = selectedRegion == region,
        onClick = { onRegionSelected(region) },
        label = { Text(region.displayName) }
      )
    }
  }
}

@Composable
private fun HomeContent(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  uiState: HomeUiState,
  pokemonPagingItems: LazyPagingItems<Pokemon>,
  navigateToDetails: (Pokemon) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      modifier = Modifier.testTag("PokedexList"),
      columns = GridCells.Fixed(2),
      contentPadding = PaddingValues(6.dp),
    ) {
      items(
        count = pokemonPagingItems.itemCount,
        key = pokemonPagingItems.itemKey { it.name }
      ) { index ->
        val pokemon = pokemonPagingItems[index]
        if (pokemon != null) {
          var palette by rememberPaletteState()
          val backgroundColor by palette.paletteBackgroundColor()

          PokemonCard(
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            pokemon = pokemon,
            onPaletteLoaded = { palette = it },
            backgroundColor = backgroundColor,
            onCardClick = { navigateToDetails(pokemon) }
          )
        }
      }
    }

    if (uiState == HomeUiState.Loading || pokemonPagingItems.loadState.refresh is LoadState.Loading) {
      PokedexCircularProgress()
    }
  }
}

@Composable
private fun PokemonCard(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  onPaletteLoaded: (Palette) -> Unit,
  backgroundColor: Color,
  pokemon: Pokemon,
  onCardClick: () -> Unit,
) {

  with(sharedTransitionScope) {
    Card(
      modifier = Modifier
        .padding(6.dp)
        .fillMaxWidth()
        .testTag("Pokemon")
        .sharedBounds(
          sharedContentState = rememberSharedContentState(key = "pokemon-${pokemon.name}"),
          animatedVisibilityScope = animatedContentScope,
        )
        .clickable { onCardClick() },
      shape = RoundedCornerShape(14.dp),
      colors = CardColors(
        containerColor = backgroundColor,
        contentColor = backgroundColor,
        disabledContainerColor = backgroundColor,
        disabledContentColor = backgroundColor,
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
      LandscapistImage(
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(top = 20.dp)
          .size(120.dp),
        imageModel = { pokemon.imageUrl },
        imageOptions = ImageOptions(contentScale = ContentScale.Inside),
        component = rememberImageComponent {
          if (!LocalInspectionMode.current) {
            +PalettePlugin(
              imageModel = pokemon.imageUrl,
              useCache = true,
              paletteLoadedListener = { onPaletteLoaded.invoke(it) },
            )
          }
        },
      )

      Text(
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .fillMaxWidth()
          .padding(12.dp),
        text = pokemon.name,
        color = PokedexTheme.colors.black,
        textAlign = TextAlign.Center,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PokedexHomePreview() {
  val pokemonPagingItems = flowOf(PagingData.from(PreviewUtils.mockPokemonList()))
    .collectAsLazyPagingItems()

  PokedexPreviewTheme { animatedContentScope ->
    PokedexHome(
      sharedTransitionScope = this@PokedexPreviewTheme,
      animatedContentScope = animatedContentScope,
      uiState = HomeUiState.Idle,
      pokemonPagingItems = pokemonPagingItems,
      onFetchByRegion = { },
    )
  }
}
