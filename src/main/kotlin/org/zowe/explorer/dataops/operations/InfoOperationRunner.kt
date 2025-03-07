/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import eu.ibagroup.r2z.InfoAPI
import eu.ibagroup.r2z.InfoResponse

class InfoOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return InfoOperationRunner()
  }
}

class InfoOperationRunner : OperationRunner<InfoOperation, InfoResponse> {
  override val operationClass = InfoOperation::class.java
  override val resultClass = InfoResponse::class.java

  override fun canRun(operation: InfoOperation) = true

  override fun run(operation: InfoOperation, progressIndicator: ProgressIndicator): InfoResponse {
    val response = api<InfoAPI>(url = operation.url, isAllowSelfSigned = operation.isAllowSelfSigned)
      .getSystemInfo()
      .cancelByIndicator(progressIndicator)
      .execute()
    if (!response.isSuccessful) {
      throw CallException(response, "Cannot connect to z/OSMF Server")
    }
    return response.body() ?: throw CallException(response, "Cannot parse z/OSMF info request body")
  }
}
