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

package com.skydoves.pokedex.compose.core.data.repository.home

import androidx.annotation.VisibleForTesting
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import androidx.tracing.trace
import com.skydoves.pokedex.compose.core.common.network.Dispatcher
import com.skydoves.pokedex.compose.core.common.network.PokedexAppDispatchers
import com.skydoves.pokedex.compose.core.database.PokedexDatabase
import com.skydoves.pokedex.compose.core.database.entitiy.mapper.asDomain
import com.skydoves.pokedex.compose.core.database.entitiy.mapper.asEntity
import com.skydoves.pokedex.compose.core.model.Pokemon
import com.skydoves.pokedex.compose.core.model.PokemonRegion
import com.skydoves.pokedex.compose.core.network.service.PokedexClient
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@VisibleForTesting
class HomeRepositoryImpl @Inject constructor(
  private val pokedexClient: PokedexClient,
  private val pokedexDatabase: PokedexDatabase,
  @Dispatcher(PokedexAppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository {

  @OptIn(ExperimentalPagingApi::class)
  override fun fetchPokemonList(
    region: PokemonRegion,
    onStart: () -> Unit,
    onCompletion: () -> Unit,
    onError: (String?) -> Unit,
  ): Flow<PagingData<Pokemon>> {
    return Pager(
      config = PagingConfig(pageSize = 20, enablePlaceholders = false),
      remoteMediator = PokemonRemoteMediator(
        homeRepository = this,
        region = region
      ),
      pagingSourceFactory = { 
        pokedexDatabase.pokemonDao().getPokemonPagingSource(
          minId = region.idRange.first,
          maxId = region.idRange.last
        ) 
      }
    ).flow
      .map { pagingData ->
        pagingData.map { it.asDomain() }
      }
      .onStart { onStart() }
      .onCompletion { onCompletion() }
      .flowOn(ioDispatcher)
  }

  override suspend fun syncRegion(region: PokemonRegion, page: Int): Boolean = trace("syncRegion") {
    val regionStartOffset = region.idRange.first - 1
    val currentOffset = regionStartOffset + (page * 20) // Use standard limit of 20

    if (currentOffset >= region.idRange.last) return@trace true

    val limit = minOf(20, region.idRange.last - currentOffset)
    
    val response = pokedexClient.fetchPokemonList(limit = limit, offset = currentOffset)
    var success = false
    
    response.suspendOnSuccess {
      val pokemons = data.results
      pokemons.forEach { it.page = page }
      
      pokedexDatabase.withTransaction {
        pokedexDatabase.pokemonDao().insertPokemonList(pokemons.asEntity())
      }
      success = true
    }
    
    success
  }
}
