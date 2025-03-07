/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.fetch.FileFetchProvider
import org.zowe.explorer.dataops.synchronizer.ContentSynchronizer
import org.zowe.explorer.dataops.synchronizer.adapters.MFContentAdapter

interface DataOpsManager : Disposable {

  companion object {
    @JvmStatic
    val instance: DataOpsManager
      get() = ApplicationManager.getApplication().getService(DataOpsManager::class.java)
  }

  fun <A : FileAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>, vFileClass: Class<out F>
  ): AttributesService<A, F>

  fun tryToGetAttributes(file: VirtualFile): FileAttributes?

  fun tryToGetFile(attributes: FileAttributes): VirtualFile?

  fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*, *>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File>

  fun isSyncSupported(file: VirtualFile): Boolean

  fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer?

  fun getMFContentAdapter(file: VirtualFile): MFContentAdapter

  fun isOperationSupported(operation: Operation<*>): Boolean

  @Throws(Throwable::class)
  fun <R : Any> performOperation(
    operation: Operation<R>,
    progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE
  ): R

  val componentManager: ComponentManager

}

inline fun <reified A : FileAttributes, reified F : VirtualFile> DataOpsManager.getAttributesService(): AttributesService<A, F> {
  return getAttributesService(A::class.java, F::class.java)
}

fun <C> List<DataOpsComponentFactory<C>>.buildComponents(dataOpsManager: DataOpsManager): MutableList<C> {
  return map { it.buildComponent(dataOpsManager) }.toMutableSmartList()
}
