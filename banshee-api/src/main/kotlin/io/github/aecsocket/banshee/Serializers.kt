package io.github.aecsocket.banshee

import io.github.aecsocket.alexandria.extension.registerExact
import io.github.aecsocket.banshee.format.GeckoLib
import org.spongepowered.configurate.serialize.TypeSerializerCollection

val bansheeApiSerializers: TypeSerializerCollection = TypeSerializerCollection.builder()
    .registerExact(GeckoLib.Keyframe.Serializer)
    .registerExact(GeckoLib.Easing.Serializer)
    .build()
