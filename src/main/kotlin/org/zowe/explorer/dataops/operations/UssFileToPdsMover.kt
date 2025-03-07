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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.getParentsChain
import eu.ibagroup.r2z.*

class UssFileToPdsMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return UssFileToPdsMover(dataOpsManager)
  }
}

class UssFileToPdsMover(private val dataOpsManager: DataOpsManager) : AbstractFileMover() {
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.sourceAttributes is RemoteUssAttributes
        && !operation.sourceAttributes.isDirectory
        && operation.destinationAttributes is RemoteDatasetAttributes
        && operation.destinationAttributes.isDirectory
        && operation.commonUrls(dataOpsManager).isNotEmpty()
        && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  fun proceedMoveCopyToPds(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val sourceAttributes = (operation.sourceAttributes as RemoteUssAttributes)
    val destinationAttributes = (operation.destinationAttributes as RemoteDatasetAttributes)
    val from = sourceAttributes.path
    val to = destinationAttributes.name
    val api = api<DataAPI>(connectionConfig)
    var memberName = sourceAttributes.name.filter { it.isLetterOrDigit() }.take(8)
    if (memberName.isEmpty()) {
      memberName = "empty"
    }
    val opName = if (operation.isMove) "move" else "copy"
    var throwable: Throwable? = null

    val copyResponse = api.copyToDatasetMemberFromUssFile(
      authorizationToken = connectionConfig.authToken,
      xIBMBpxkAutoCvt = XIBMBpxkAutoCvt.ON,
      body = CopyDataZOS.CopyFromFile(
        request = "copy",
        file = CopyDataZOS.CopyFromFile.File(sourceAttributes.path, CopyDataZOS.CopyFromFile.File.CopyType.TEXT),
        replace = true
      ),
      toDatasetName = destinationAttributes.name,
      memberName = memberName
    ).cancelByIndicator(progressIndicator).execute()
    if (!copyResponse.isSuccessful &&
      copyResponse.errorBody()?.string()?.contains("Truncation of a record occurred during an I/O operation.") != true) {
      return CallException(copyResponse, "Cannot $opName $from to $to")
    }

    if (operation.isMove) {
      val deleteResponse = api.deleteUssFile(
        authorizationToken = connectionConfig.authToken,
        filePath = from.substring(1),
        xIBMOption = XIBMOption.RECURSIVE
      ).execute()
      if (!deleteResponse.isSuccessful) {
        val rollbackResponse = api.deleteDatasetMember(
          authorizationToken = connectionConfig.authToken,
          datasetName = to, memberName = memberName
        ).execute()
        throwable = if (rollbackResponse.isSuccessful){
          CallException(deleteResponse, "Cannot $opName $from to $to. Rollback proceeded successfully.")
        } else {
          CallException(deleteResponse, "Cannot $opName $from to $to. Rollback failed.")
        }
      }
    }

    return throwable
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        throwable = proceedMoveCopyToPds(requester.connectionConfig, operation, progressIndicator)
        break
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }


}
