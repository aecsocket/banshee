package io.github.aecsocket.banshee.paper

import io.github.aecsocket.alexandria.LoggingList
import io.github.aecsocket.alexandria.extension.force
import io.github.aecsocket.alexandria.extension.registerExact
import io.github.aecsocket.alexandria.paper.AlexandriaPlugin
import io.github.aecsocket.alexandria.paper.fallbackLocale
import io.github.aecsocket.alexandria.paper.seralizer.alexandriaPaperSerializers
import io.github.aecsocket.banshee.GeckoLibAnimations
import io.github.aecsocket.banshee.bansheeApiSerializers
import io.github.aecsocket.glossa.MessageProxy
import io.github.aecsocket.glossa.configurate.LocaleSerializer
import io.github.aecsocket.glossa.messageProxy
import io.github.aecsocket.klam.configurate.klamSerializers
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.format.TextColor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.ObjectMapper
import java.util.*

private lateinit var instance: Banshee
val BansheeAPI get() = instance

class Banshee : AlexandriaPlugin(Manifest("banshee",
    accentColor = TextColor.color(0x543deb),
    languageResources = listOf(
        "lang/en-US.yml",
    ),
    savedResources = listOf(
        "settings.yml",
    ),
)) {
    companion object {
        @JvmStatic
        fun api() = BansheeAPI
    }

    @ConfigSerializable
    data class Settings(
        override val defaultLocale: Locale = fallbackLocale,
    ) : AlexandriaPlugin.Settings

    override lateinit var settings: Settings private set
    lateinit var messages: MessageProxy<BansheeMessages> private set

    override val configOptions: ConfigurationOptions = ConfigurationOptions.defaults().serializers { it
        .registerAll(ConfigurateComponentSerializer.configurate().serializers())
        .registerExact(LocaleSerializer)
        .registerAll(klamSerializers)
        .registerAll(alexandriaPaperSerializers)
        .registerAll(bansheeApiSerializers)
        .registerAnnotatedObjects(ObjectMapper.factoryBuilder()
            .addDiscoverer(dataClassFieldDiscoverer())
            .build()
        )
    }

    init {
        instance = this
    }

    override fun loadSettings(node: ConfigurationNode?) {
        settings = node?.get() ?: Settings()
    }

    override fun load(log: LoggingList) {
        messages = glossa.messageProxy()
        val anims = configLoaderBuilder()
            .file(dataFolder.resolve("anim.json"))
            .build()
            .load()
            .force<GeckoLibAnimations>()
        println(anims)
    }
}
