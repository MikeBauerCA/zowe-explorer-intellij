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

import com.intellij.openapi.vfs.VirtualFile

fun interface SaveStrategy {
  fun decide(
    file: VirtualFile,
    lastSuccessfulState: ByteArray,
    currentRemoteState: ByteArray
  ): Boolean
}
