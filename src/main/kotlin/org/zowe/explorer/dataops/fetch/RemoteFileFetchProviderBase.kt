/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.services.ErrorSeparatorService
import org.zowe.explorer.utils.runIfTrue
import org.zowe.explorer.utils.runWriteActionOnWriteThread
import org.zowe.explorer.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.set
import kotlin.concurrent.withLock
import kotlin.streams.toList

abstract class RemoteFileFetchProviderBase<Request : Any, Response : Any, File : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : FileFetchProvider<Request, RemoteQuery<Request, Unit>, File> {

  private enum class CacheState {
    FETCHED, ERROR
  }

  private val lock = ReentrantLock()

  private val cache = mutableMapOf<RemoteQuery<Request, Unit>, Collection<File>>()
  private val cacheState = mutableMapOf<RemoteQuery<Request, Unit>, CacheState>()
  protected var errorMessages = mutableMapOf<RemoteQuery<Request, Unit>, String>()

  override fun getCached(query: RemoteQuery<Request, Unit>): Collection<File>? {
    return lock.withLock { (cacheState[query] == CacheState.FETCHED).runIfTrue { cache[query] } }
  }

  override fun isCacheValid(query: RemoteQuery<Request, Unit>): Boolean {
    return lock.withLock { cacheState[query] != CacheState.ERROR }
  }

  override fun getFetchedErrorMessage(query: RemoteQuery<Request, Unit>): String? {
    return lock.withLock { cacheState[query] == CacheState.ERROR }.runIfTrue { errorMessages[query] }
  }

  protected abstract fun fetchResponse(
    query: RemoteQuery<Request, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File?

  protected abstract fun cleanupUnusedFile(file: File, query: RemoteQuery<Request, Unit>)

  open fun compareOldAndNewFile(oldFile: File, newFile: File): Boolean {
    return oldFile.path == newFile.path
  }

  override fun reload(
    query: RemoteQuery<Request, Unit>,
    progressIndicator: ProgressIndicator
  ) {
    runCatching {
      val fetched = fetchResponse(query, progressIndicator)
      val files = runWriteActionOnWriteThread {
        fetched.mapNotNull {
          convertResponseToFile(it)
        }
      }

      cache[query]?.parallelStream()?.filter { oldFile ->
        oldFile.isValid && files.none { compareOldAndNewFile(oldFile, it) }
      }?.toList()?.apply {
        runWriteActionOnWriteThread {
          forEach { cleanupUnusedFile(it, query) }
        }
      }
      cache[query] = files
      cacheState[query] = CacheState.FETCHED
      files
    }.onSuccess {
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onCacheUpdated(query, it)
    }.onFailure {
      if (it is ProcessCanceledException) {
        cleanCacheInternal(query, false)
        sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onFetchCancelled(query)
      } else {
        if (it is CallException) {
          val details = it.errorParams?.get("details")
          var errorMessage = it.message ?: "Error"
          if (details is List<*>) {
            errorMessage = details[0] as String
          }
          errorMessages[query] = service<ErrorSeparatorService>().separateErrorMessage(errorMessage)["error.description"] as String
        }
        cache[query] = listOf()
        cacheState[query] = CacheState.ERROR
        sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onFetchFailure(query, it)
      }
    }
  }

  override fun cleanCache(query: RemoteQuery<Request, Unit>) {
    cleanCacheInternal(query, true)
  }

  private fun cleanCacheInternal(query: RemoteQuery<Request, Unit>, sendTopic: Boolean) {
    cacheState.remove(query)
    if (sendTopic) {
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onCacheCleaned(query)
    }
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}
