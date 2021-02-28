package eu.ibagroup.formainframe.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.EventHandler
import eu.ibagroup.formainframe.utils.crudable.annotations.Contains
import eu.ibagroup.formainframe.utils.sendTopic
import java.time.Duration

fun sendConfigServiceTopic(): EventHandler = sendTopic(ConfigService.CONFIGS_CHANGED)

interface ConfigService : PersistentStateComponent<ConfigState> {

  companion object {
    @JvmStatic
    val instance: ConfigService = ApplicationManager.getApplication().getService(ConfigService::class.java)

    @JvmStatic
    val CONFIGS_CHANGED = Topic.create("configsChanged", EventHandler::class.java)

    @JvmStatic
    val CONFIGS_LOADED = Topic.create("configsLoaded", Runnable::class.java)
  }

  val configsAreLoaded: Boolean

  @get:Contains(
    entities = [
      WorkingSetConfig::class,
      ConnectionConfig::class,
      UrlConnection::class
    ]
  )
  val crudable: Crudable

  val eventHandler: EventHandler

  val autoSaveDelay: Duration

}

class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfig) : Exception(
  "No username or password found for $connectionConfig"
)

val configCrudable: Crudable
  get() = ConfigService.instance.crudable
