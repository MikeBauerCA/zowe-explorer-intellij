/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.dataops.operations.UssAllocationParams
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateUssFileName
import eu.ibagroup.r2z.CreateUssFile
import eu.ibagroup.r2z.FileMode
import eu.ibagroup.r2z.FileModeValue
import eu.ibagroup.r2z.FileType
import javax.swing.JComponent

val dummyState: CreateFileDialogState
get() = CreateFileDialogState(
  parameters = CreateUssFile(FileType.FILE, FileMode(7,7,7))
)

class CreateFileDialog(project: Project?, override var state: CreateFileDialogState = dummyState, filePath: String) :
  StatefulDialog<CreateFileDialogState>(project = project) {

  override fun createCenterPanel(): JComponent {

    val modelTemplateFactory = {
      CollectionComboBoxModel(
        listOf(
          FileModeValue.NONE,
          FileModeValue.READ,
          FileModeValue.WRITE,
          FileModeValue.READ_WRITE,
          FileModeValue.EXECUTE,
          FileModeValue.READ_EXECUTE,
          FileModeValue.READ_WRITE,
          FileModeValue.READ_WRITE_EXECUTE
        )
      )
    }

    fun Int.toFileModeValue(): FileModeValue {
      return when (this) {
        0 -> FileModeValue.NONE
        1 -> FileModeValue.EXECUTE
        2 -> FileModeValue.WRITE
        3 -> FileModeValue.WRITE_EXECUTE
        4 -> FileModeValue.READ
        5 -> FileModeValue.READ_EXECUTE
        6 -> FileModeValue.READ_WRITE
        7 -> FileModeValue.READ_WRITE_EXECUTE
        else -> FileModeValue.NONE
      }
    }


    return panel {
      row {
        label("Name")
        textField(state::fileName).withValidationOnInput {
          validateUssFileName(it)
        }.withValidationOnApply {
          validateForBlank(it)
        }.apply {
          focused()
        }
      }
      row {
        label("Owner")
        comboBox(
          model = modelTemplateFactory(),
          modelBinding = PropertyBinding(
            get = { state.parameters.mode.owner.toFileModeValue() },
            set = {
              state.parameters.mode.owner = it?.mode ?: 0
            }
          )
        )
      }
      row {
        label("Group")
        comboBox(
          model = modelTemplateFactory(),
          modelBinding = PropertyBinding(
            get = { state.parameters.mode.group.toFileModeValue() },
            set = {
              state.parameters.mode.group = it?.mode ?: 0
            }
          )
        )
      }
      row {
        label("All")
        comboBox(
          model = modelTemplateFactory(),
          modelBinding = PropertyBinding(
            get = { state.parameters.mode.all.toFileModeValue() },
            set = {
              state.parameters.mode.all = it?.mode ?: 0
            }
          )
        )
      }
    }

  }

  init {
    val type = if (state.parameters.type == FileType.DIR) "Directory" else "File"
    title = "Create $type under $filePath"
    init()
  }

}


val emptyFileState: CreateFileDialogState
  get() = CreateFileDialogState(
    parameters = CreateUssFile(
      type = FileType.FILE,
      mode = FileMode(6, 6, 6)
    )
  )

val emptyDirState: CreateFileDialogState
  get() = CreateFileDialogState(
    parameters = CreateUssFile(
      type = FileType.DIR,
      mode = FileMode(7, 7, 7)
    )
  )

data class CreateFileDialogState(
  val parameters: CreateUssFile,
  var fileName: String = "",
  var path: String = "",
)

fun CreateFileDialogState.toAllocationParams(): UssAllocationParams {
  return UssAllocationParams(parameters, fileName, path)
}
