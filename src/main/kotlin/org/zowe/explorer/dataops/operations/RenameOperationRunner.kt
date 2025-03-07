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
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.findAnyNullable
import org.zowe.explorer.utils.runWriteActionOnWriteThread
import org.zowe.explorer.vfs.sendVfsChangesTopic
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.MoveUssFile
import eu.ibagroup.r2z.RenameData

class RenameOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return RenameOperationRunner(dataOpsManager)
  }
}

class RenameOperationRunner(private val dataOpsManager: DataOpsManager) : OperationRunner<RenameOperation, Unit> {

  override val operationClass = RenameOperation::class.java

  override val resultClass = Unit::class.java

  override fun canRun(operation: RenameOperation): Boolean {
    return with(operation.attributes) {
      this is RemoteMemberAttributes || this is RemoteDatasetAttributes || this is RemoteUssAttributes
    }
  }

  override fun run(
    operation: RenameOperation,
    progressIndicator: ProgressIndicator
  ) {
    when (val attributes = operation.attributes) {
      is RemoteDatasetAttributes -> {
        attributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDataset(
              authorizationToken = it.connectionConfig.authToken,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = attributes.name
                )
              ),
              toDatasetName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              sendVfsChangesTopic()
            } else {
              throw CallException(response, "Unable to rename the selected dataset")
            }
          } catch (e: Throwable) {
            if (e is CallException) { throw e } else { throw RuntimeException(e) }
          }
        }
      }
      is RemoteMemberAttributes -> {
        val parentAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
        parentAttributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDatasetMember(
              authorizationToken = it.connectionConfig.authToken,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = parentAttributes.datasetInfo.name,
                  oldMemberName = attributes.info.name
                )
              ),
              toDatasetName = parentAttributes.datasetInfo.name,
              memberName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              sendVfsChangesTopic()
            } else {
              throw CallException(response, "Unable to rename the selected member")
            }
          } catch (e: Throwable) {
            if (e is CallException) { throw e } else { throw RuntimeException(e) }
          }
        }
      }
      is RemoteUssAttributes -> {
        val parentDirPath = attributes.parentDirPath
        attributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).moveUssFile(
              authorizationToken = it.connectionConfig.authToken,
              body = MoveUssFile(
                from = attributes.path
              ),
              filePath = FilePath("$parentDirPath/${operation.newName}")
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              sendVfsChangesTopic()
            } else {
              throw CallException(response, "Unable to rename the selected file or directory")
            }
          } catch (e: Throwable) {
            if (e is CallException) { throw e } else { throw RuntimeException(e) }
          }
        }
      }
    }
  }
}
