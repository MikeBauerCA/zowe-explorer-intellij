/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.attributes

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.io.IOException

interface AttributesService<Attributes : FileAttributes, VFile : VirtualFile> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<AttributesServiceFactory>("org.zowe.explorer.attributesService")

    @JvmField
    val ATTRIBUTES_CHANGED = Topic.create("attributesChanged", AttributesListener::class.java)
  }

  @Throws(IOException::class)
  fun getOrCreateVirtualFile(attributes: Attributes): VFile

  fun getVirtualFile(attributes: Attributes): VFile?

  fun getAttributes(file: VFile): Attributes?

  @Throws(IOException::class)
  fun updateAttributes(file: VFile, newAttributes: Attributes)

  @Suppress("UNCHECKED_CAST")
  @Throws(IOException::class)
  fun updateAttributes(file: VFile, updater: Attributes.() -> Unit) {
    getAttributes(file)?.let { updateAttributes(file, updater.cloneAndApply(it)) }
  }

  @Throws(IOException::class)
  fun updateAttributes(oldAttributes: Attributes, updater: Attributes.() -> Unit) {
    getVirtualFile(oldAttributes)?.let { updateAttributes(it, updater.cloneAndApply(oldAttributes)) }
  }

  @Throws(IOException::class)
  fun updateAttributes(oldAttributes: Attributes, newAttributes: Attributes) {
    getVirtualFile(oldAttributes)?.let { updateAttributes(it, newAttributes) }
  }

  @Throws(IOException::class)
  fun clearAttributes(file: VFile)

  @Throws(IOException::class)
  fun clearAttributes(attributes: Attributes) {
    getVirtualFile(attributes)?.let { clearAttributes(it) }
  }

  val attributesClass: Class<out Attributes>

  val vFileClass: Class<out VFile>

}

@Suppress("UNCHECKED_CAST")
fun <Attributes : FileAttributes> (Attributes.() -> Unit).cloneAndApply(attributes: Attributes): Attributes {
  val cloned = attributes.clone() as Attributes
  invoke(cloned)
  return cloned
}
