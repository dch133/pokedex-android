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

package com.skydoves.pokedex.compose.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.skydoves.pokedex.compose.core.data.worker.PokemonSyncWorker
import com.skydoves.pokedex.compose.core.model.PokemonRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Utility class to manage synchronization tasks and observe status.
 */
object Sync {
  /**
   * Initializes the synchronization process with specific constraints for a given region.
   */
  fun initialize(context: Context, region: PokemonRegion) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(true)
      .build()

    val syncRequest = OneTimeWorkRequestBuilder<PokemonSyncWorker>()
      .setConstraints(constraints)
      .setInputData(workDataOf(PokemonSyncWorker.KEY_REGION_NAME to region.name))
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      PokemonSyncWorker.WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      syncRequest
    )
  }

  /**
   * Returns a Flow of [Boolean] indicating if a sync is currently in progress.
   */
  fun isSyncing(context: Context): Flow<Boolean> {
    return WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkFlow(PokemonSyncWorker.WORK_NAME)
      .map { workInfos ->
        workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
      }
  }
}
