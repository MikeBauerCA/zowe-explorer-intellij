/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager

class SyncAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
  }

  override fun update(e: AnActionEvent) {
    val file = getSupportedVirtualFile(e) ?: let {
      makeDisabled(e)
      return
    }
    val editor = getEditor(e) ?: let {
      makeDisabled(e)
      return
    }
    e.presentation.isEnabledAndVisible = !(editor.document.text.toByteArray() contentEquals file.contentsToByteArray())
  }

  private fun makeDisabled(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
  }

  private fun getDataOpsManager(e: AnActionEvent): DataOpsManager {
    return service()
  }

  private fun getEditor(e: AnActionEvent): Editor? {
    return e.getData(CommonDataKeys.EDITOR)
  }

  private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
    return e.getData(CommonDataKeys.VIRTUAL_FILE)
  }

  private fun getSupportedVirtualFile(e: AnActionEvent): VirtualFile? {
    return getVirtualFile(e)?.let {
      if (getDataOpsManager(e).isSyncSupported(it)) {
        it
      } else {
        null
      }
    }
  }

}
