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

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skydoves.pokedex.compose.core.database.PokemonDao
import com.skydoves.pokedex.compose.core.database.entitiy.mapper.asDomain
import com.skydoves.pokedex.compose.core.database.entitiy.mapper.asEntity
import com.skydoves.pokedex.compose.core.model.Pokemon
import com.skydoves.pokedex.compose.core.model.PokemonRegion
import com.skydoves.pokedex.compose.core.network.service.PokedexClient
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.suspendOnSuccess

class PokemonPagingSource(
  private val pokedexClient: PokedexClient,
  private val pokemonDao: PokemonDao,
  private val region: PokemonRegion,
) : PagingSource<Int, Pokemon>() {

  override fun getRefreshKey(state: PagingState<Int, Pokemon>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
    val page = params.key ?: 0
    val loadSize = params.loadSize
    
    // Calculate the absolute offset for this region and page
    // region.idRange starts at 1 (Kanto), so the starting offset is 0.
    val regionStartOffset = region.idRange.first - 1
    val currentOffset = regionStartOffset + (page * loadSize)
    
    // Calculate how many items are left in this region
    val itemsRemainingInRegion = region.idRange.last - currentOffset
    
    if (itemsRemainingInRegion <= 0) {
      return LoadResult.Page(data = emptyList(), prevKey = page - 1, nextKey = null)
    }

    val limit = minOf(loadSize, itemsRemainingInRegion)

    return try {
      // 1. Try to get from database first (filtering by the current region's range)
      // Note: This logic assumes your DAO can handle region-based fetching or you filter manually
      var pokemons = pokemonDao.getPokemonList(page).asDomain()
        .filter { it.id in region.idRange }

      if (pokemons.isEmpty()) {
        // 2. Fetch from network with the calculated offset and limit
        val response = pokedexClient.fetchPokemonList(
          limit = limit,
          offset = currentOffset
        )
        
        var result: LoadResult<Int, Pokemon> = LoadResult.Error(Exception("Network error"))

        response.suspendOnSuccess {
          val fetchedPokemons = data.results
          fetchedPokemons.forEach { pokemon -> pokemon.page = page }
          
          // Save to database
          pokemonDao.insertPokemonList(fetchedPokemons.asEntity())

          val nextKey = if (data.next == null || fetchedPokemons.size < limit || (currentOffset + limit) >= region.idRange.last) {
            null
          } else {
            page + 1
          }
          
          result = LoadResult.Page(
            data = fetchedPokemons,
            prevKey = if (page == 0) null else page - 1,
            nextKey = nextKey
          )
        }.onFailure {
          result = LoadResult.Error(Exception(message()))
        }
        result
      } else {
        // Data found in DB
        val nextKey = if (pokemons.size < limit || (currentOffset + limit) >= region.idRange.last) null else page + 1
        LoadResult.Page(
          data = pokemons,
          prevKey = if (page == 0) null else page - 1,
          nextKey = nextKey
        )
      }
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
