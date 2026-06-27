package no.nordicsemi.android.toolbox.profile.repository.uartXml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.nordicsemi.android.toolbox.lib.storage.Configuration
import no.nordicsemi.android.toolbox.lib.storage.ConfigurationsDao
import no.nordicsemi.android.toolbox.profile.data.uart.MacroIcon
import no.nordicsemi.android.toolbox.profile.data.uart.UARTConfiguration
import no.nordicsemi.android.toolbox.profile.data.uart.UARTMacro
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.strategy.Strategy
import org.simpleframework.xml.strategy.VisitorStrategy
import org.simpleframework.xml.stream.Format
import org.simpleframework.xml.stream.HyphenStyle
import timber.log.Timber
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UartConfigurationRepository @Inject constructor(
    private val configurationDao: ConfigurationsDao,
    private val uartDataStore: UartConfigurationDataSource
) {
    // Get all uart configurations.
    fun getAllConfigurations(): Flow<List<UARTConfiguration>> =
        configurationDao.getAllConfigurations().map { configurations ->
            configurations.mapNotNull { it.toDomain() }
        }

    private fun Configuration.toDomain(): UARTConfiguration? {
        return try {
            val xml: String = xml
            val format = Format(HyphenStyle())
            val serializer: Serializer = Persister(format)
            val configuration = serializer.read(XmlConfiguration::class.java, xml)

            UARTConfiguration(
                _id,
                configuration.name ?: "Unknown",
                createMacro(configuration.commands)
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun createMacro(macros: Array<XmlMacro?>): List<UARTMacro?> {
        return macros.map {
            if (it == null) {
                null
            } else {
                val icon = MacroIcon.create(it.iconIndex)
                UARTMacro(icon, it.command, it.eol)
            }
        }
    }

    @IgnorableReturnValue
    suspend fun insertConfiguration(configuration: UARTConfiguration): Long? {
        val configurationEntity = configuration.toConfigurationEntity()
        return configurationEntity?.let { configurationDao.insertConfiguration(it) }
    }

    suspend fun deleteConfiguration(configuration: UARTConfiguration) {
        configurationDao.deleteConfiguration(configuration.name)
    }

    private fun UARTConfiguration.toConfigurationEntity(): Configuration? {
        return try {
            val format = Format(HyphenStyle())
            val strategy: Strategy = VisitorStrategy(CommentVisitor())
            val serializer: Serializer = Persister(strategy, format)
            val writer = StringWriter()
            serializer.write(this.toXmlConfiguration(), writer)

            return Configuration(
                _id = id,
                name = name,
                xml = writer.toString(),
                deleted = 0
            )
        } catch (e: Exception) {
            Timber.e(e, "Error converting to ConfigurationEntity")
            null
        }

    }

    private fun UARTConfiguration.toXmlConfiguration(): XmlConfiguration {
        val xmlConfiguration = XmlConfiguration()
        xmlConfiguration.name = name
        val commands = macros.map { macro ->
            macro?.let {
                XmlMacro().apply {
                    setEol(it.newLineChar.ordinal)
                    command = it.command
                    iconIndex = it.icon.index
                }
            }
        }.toTypedArray()
        xmlConfiguration.commands = commands
        return xmlConfiguration
    }

    fun getLastConfigurationName(): Flow<String?> {
        return uartDataStore.lastConfigurationName
    }

    suspend fun saveLastConfigurationNameToDataSource(name: String) {
        uartDataStore.saveConfigurationName(name)
    }
}