package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.utils.ChannelExecutor
import eu.ibagroup.formainframe.utils.Execution
import eu.ibagroup.formainframe.utils.QueueExecutor
import kotlinx.coroutines.channels.Channel
import java.time.Duration

private val CHANNEL_DELAY = Duration.ofMillis(ConfigService.instance.autoSaveDelayMillis)

abstract class AbstractAttributedContentSynchronizer<Attributes : VFileInfoAttributes>(
  dataOpsManager: DataOpsManager
) : AbstractQueuedContentSynchronizer(dataOpsManager) {

  protected val attributesService by lazy {
    dataOpsManager.getAttributesService(attributesClass, vFileClass)
  }

  protected abstract fun buildExecution(
    file: VirtualFile,
    saveStrategy: SaveStrategy
  ): Execution<Unit, Unit>

  @Suppress("UNCHECKED_CAST")
  override fun buildExecutorForFile(file: VirtualFile, saveStrategy: SaveStrategy): QueueExecutor<Unit, Unit> {
    return ChannelExecutor(Channel(Channel.CONFLATED), CHANNEL_DELAY, buildExecution(file, saveStrategy))
  }

  override fun accepts(file: VirtualFile): Boolean {
    return vFileClass.isAssignableFrom(file::class.java) && attributesService.getAttributes(file) != null
  }

  protected abstract val vFileClass: Class<out VirtualFile>

  protected abstract val attributesClass: Class<out Attributes>

}