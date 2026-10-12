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

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.skydoves.pokedex.compose.core.data.repository.home.HomeRepository
import com.skydoves.pokedex.compose.core.model.Pokemon
import com.skydoves.pokedex.compose.core.model.PokemonRegion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val homeRepository: HomeRepository,
) : ViewModel() {

  internal val uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)

  private val _regionState = MutableStateFlow(PokemonRegion.KANTO)
  val regionState: StateFlow<PokemonRegion> = _regionState.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val pokemonPagingData: Flow<PagingData<Pokemon>> = _regionState.flatMapLatest { region ->
    homeRepository.fetchPokemonList(
      region = region,
      onStart = { },
      onComplete = { uiState.tryEmit(HomeUiState.Idle) },
      onError = { uiState.tryEmit(HomeUiState.Error(it)) },
    )
  }.cachedIn(viewModelScope)

  fun fetchByRegion(region: PokemonRegion) {
    _regionState.value = region
  }
}

@Stable
internal sealed interface HomeUiState {

  data object Idle : HomeUiState

  data object Loading : HomeUiState

  data class Error(val message: String?) : HomeUiState
}
