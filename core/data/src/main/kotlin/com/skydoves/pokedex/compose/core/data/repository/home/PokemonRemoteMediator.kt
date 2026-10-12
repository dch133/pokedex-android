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

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.skydoves.pokedex.compose.core.database.entitiy.PokemonEntity
import com.skydoves.pokedex.compose.core.model.PokemonRegion

@OptIn(ExperimentalPagingApi::class)
class PokemonRemoteMediator(
  private val homeRepository: HomeRepository,
  private val region: PokemonRegion,
) : RemoteMediator<Int, PokemonEntity>() {

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, PokemonEntity>,
  ): MediatorResult {
    val page = when (loadType) {
      LoadType.REFRESH -> 0
      LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
      LoadType.APPEND -> {
        val lastItem = state.lastItemOrNull()
        if (lastItem == null) {
          0
        } else {
          lastItem.page + 1
        }
      }
    }

    val regionStartOffset = region.idRange.first - 1
    val currentOffset = regionStartOffset + (page * state.config.pageSize)
    
    // Check if we've reached the end of the region
    if (currentOffset >= region.idRange.last) {
      return MediatorResult.Success(endOfPaginationReached = true)
    }

    val success = homeRepository.syncRegion(region, page)

    return if (success) {
      // In a real app, you'd check if the last page was actually reached by the API response
      // For this practice, we simplify.
      MediatorResult.Success(endOfPaginationReached = false)
    } else {
      MediatorResult.Error(Exception("Sync failed"))
    }
  }
}
