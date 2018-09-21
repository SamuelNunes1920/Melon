/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.work.sync

import android.content.Context
import androidx.annotation.IntRange
import androidx.work.*
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.work.enqueueUnique
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncLinkedWorker(
    context : Context, params : WorkerParameters
): Worker(context, params) {
    @Inject
    lateinit var repository: SagresSyncRepository

    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
        repository.performSync()

        val period = inputData.getInt(PERIOD, 60)
        createWorker(period, false)
        return Result.SUCCESS
    }

    companion object {
        private const val PERIOD = "linked_work_period"

        private const val TAG = "linked_sagres_sync_worker"
        private const val NAME = "worker_sagres_linked"

        fun createWorker(@IntRange(from = 1, to = 9000) period: Int, replace: Boolean = true) {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val data = workDataOf(PERIOD to period)

            val request = OneTimeWorkRequestBuilder<SyncLinkedWorker>()
                    .setInputData(data)
                    .addTag(TAG)
                    .setInitialDelay(period.toLong(), TimeUnit.MINUTES)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

            request.enqueueUnique(NAME, replace)
        }

        fun stopWorker() {
            WorkManager.getInstance().cancelAllWorkByTag(TAG)
        }
    }
}