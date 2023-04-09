package io.github.aecsocket.banshee

import io.github.aecsocket.alexandria.extension.registerExact
import org.spongepowered.configurate.serialize.TypeSerializerCollection

val bansheeApiSerializers: TypeSerializerCollection = TypeSerializerCollection.builder()
    .registerExact(GeckoLibAnimation.Easing.Serializer)
    .registerExact(GeckoLibAnimation.Keyframes.Serializer)
    .build()
