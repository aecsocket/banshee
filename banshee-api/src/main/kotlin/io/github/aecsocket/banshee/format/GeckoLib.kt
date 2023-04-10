package io.github.aecsocket.banshee.format

import io.github.aecsocket.alexandria.extension.force
import io.github.aecsocket.banshee.*
import io.github.aecsocket.klam.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.contains
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val ANIMATIONS = "animations"
private const val ANIMATION_LENGTH = "animation_length"
private const val BONES = "bones"
private const val EASING = "easing"
private const val EASING_ARGS = "easingArgs"
private const val LOOP = "loop"
private const val MINECRAFT_GEOMETRY = "minecraft:geometry"
private const val NAME = "name"
private const val PIVOT = "pivot"
private const val POSITION = "position"
private const val ROTATION = "rotation"
private const val SCALE = "scale"
private const val VECTOR = "vector"

private const val SCALE_FACTOR = 16.0f

object GeckoLib {
    class Geometry internal constructor(
        internal val bones: Map<String, Bone>,
    ) {
        internal data class Bone(
            val pivot: FVec3,
        )
    }

    fun deserializeGeometry(node: ConfigurationNode) : Geometry {
        return Geometry(node
            .node(MINECRAFT_GEOMETRY)
            .node(0)
            .node(BONES)
            .childrenList().map { bone ->
                bone.node(NAME).force<String>() to Geometry.Bone(
                    pivot = bone.node(PIVOT).force<FVec3>() / SCALE_FACTOR,
                )
            }.associate { it }
        )
    }

    internal enum class Loop {
        FALSE,
        HOLD_ON_LAST_FRAME,
        TRUE,
    }

    internal sealed interface Easing {
        object Linear : Easing

        data class Step(
            val steps: Int
        ) : Easing

        enum class EaseEnds {
            IN,
            OUT,
            IN_OUT,
        }

        enum class EaseMethod {
            SINE,
            QUAD,
            CUBIC,
            QUART,
            QUINT,
            EXPO,
            CIRC,
        }

        sealed interface Smooth : Easing {
            val ends: EaseEnds
        }

        data class Basic(
            override val ends: EaseEnds,
            val method: EaseMethod,
        ) : Smooth

        data class Back(
            override val ends: EaseEnds,
            val overshoot: Float,
        ) : Smooth

        data class Elastic(
            override val ends: EaseEnds,
            val bounciness: Float,
        ) : Smooth

        data class Bounce(
            override val ends: EaseEnds,
            val bounciness: Float,
        ) : Smooth

        object Serializer : TypeSerializer<Easing> {
            override fun serialize(type: Type, obj: Easing?, node: ConfigurationNode) {}
            override fun deserialize(type: Type, node: ConfigurationNode): Easing {
                val easing = node.node(EASING)
                if (easing.empty()) return Linear
                val args = node.node(EASING_ARGS)
                return when (val method = easing.string) {
                    "linear" -> Linear
                    "step" -> Step(
                        node.node(EASING_ARGS).node(0).int,
                    )

                    "easeInSine" -> Basic(EaseEnds.IN, EaseMethod.SINE)
                    "easeOutSine" -> Basic(EaseEnds.OUT, EaseMethod.SINE)
                    "easeInOutSine" -> Basic(EaseEnds.IN_OUT, EaseMethod.SINE)

                    "easeInQuad" -> Basic(EaseEnds.IN, EaseMethod.QUAD)
                    "easeOutQuad" -> Basic(EaseEnds.OUT, EaseMethod.QUAD)
                    "easeInOutQuad" -> Basic(EaseEnds.IN_OUT, EaseMethod.QUAD)

                    "easeInCubic" -> Basic(EaseEnds.IN, EaseMethod.CUBIC)
                    "easeOutCubic" -> Basic(EaseEnds.OUT, EaseMethod.CUBIC)
                    "easeInOutCubic" -> Basic(EaseEnds.IN_OUT, EaseMethod.CUBIC)

                    "easeInQuart" -> Basic(EaseEnds.IN, EaseMethod.QUART)
                    "easeOutQuart" -> Basic(EaseEnds.OUT, EaseMethod.QUART)
                    "easeInOutQuart" -> Basic(EaseEnds.IN_OUT, EaseMethod.QUART)

                    "easeInQuint" -> Basic(EaseEnds.IN, EaseMethod.QUINT)
                    "easeOutQuint" -> Basic(EaseEnds.OUT, EaseMethod.QUINT)
                    "easeInOutQuint" -> Basic(EaseEnds.IN_OUT, EaseMethod.QUINT)

                    "easeInExpo" -> Basic(EaseEnds.IN, EaseMethod.EXPO)
                    "easeOutExpo" -> Basic(EaseEnds.OUT, EaseMethod.EXPO)
                    "easeInOutExpo" -> Basic(EaseEnds.IN_OUT, EaseMethod.EXPO)

                    "easeInCirc" -> Basic(EaseEnds.IN, EaseMethod.CIRC)
                    "easeOutCirc" -> Basic(EaseEnds.OUT, EaseMethod.CIRC)
                    "easeInOutCirc" -> Basic(EaseEnds.IN_OUT, EaseMethod.CIRC)

                    "easeInBack" -> Back(EaseEnds.IN, args.node(0).float)
                    "easeOutBack" -> Back(EaseEnds.OUT, args.node(0).float)
                    "easeInOutBack" -> Back(EaseEnds.IN_OUT, args.node(0).float)

                    "easeInElastic" -> Elastic(EaseEnds.IN, args.node(0).float)
                    "easeOutElastic" -> Elastic(EaseEnds.OUT, args.node(0).float)
                    "easeInOutElastic" -> Elastic(EaseEnds.IN_OUT, args.node(0).float)

                    "easeInBounce" -> Bounce(EaseEnds.IN, args.node(0).float)
                    "easeOutBounce" -> Bounce(EaseEnds.OUT, args.node(0).float)
                    "easeInOutBounce" -> Bounce(EaseEnds.IN_OUT, args.node(0).float)

                    else -> throw SerializationException(node, type, "Invalid easing method '$method'")
                }
            }
        }
    }

    @ConfigSerializable
    internal data class Keyframe(
        @Required @Setting("vector") val vector: FVec3,
        @Setting(nodeFromParent = true) val easing: Easing,
    )

    fun deserializeAnimations(geometry: Geometry, node: ConfigurationNode) : Map<String, Animation> {
        val type = Animation::class.java
        return node.node(ANIMATIONS).childrenMap().map { (animationKey, animation) ->
            val length = animation.node(ANIMATION_LENGTH).let { nLength ->
                val value = nLength.force<Float>()
                if (value <= 0.0f)
                    throw SerializationException(nLength, type, "Animation length must be >= 0.0")
                value
            }
            val loop = animation.node(LOOP).get { Loop.FALSE }

            animationKey.toString() to Animation(
                length = length,
                loop = when (loop) {
                    Loop.FALSE -> Animation.Loop.NONE
                    Loop.HOLD_ON_LAST_FRAME -> Animation.Loop.HOLD_LAST_FRAME
                    Loop.TRUE -> Animation.Loop.LOOP
                },
                bones = animation.node(BONES).childrenMap().map { (boneKey, bone) ->
                    val boneGeometry = geometry.bones[boneKey]
                        ?: throw SerializationException(bone, type, "Bone '$boneKey' does not exist in geometry")

                    fun timeline(
                        node: ConfigurationNode,
                        mapper: (Float) -> Float,
                    ): Animation.Timeline<FVec3> {
                        return if (node.contains(VECTOR)) {
                            // one keyframe at t = 0.0
                            val keyframe = node.force<Keyframe>()
                            Animation.Timeline(listOf(Animation.Keyframe(
                                time = 0.0f,
                                value = keyframe.vector.map(mapper),
                            )))
                        } else {
                            val keyframes = node.force<MutableMap<Float, Keyframe>>()
                            Animation.Timeline(keyframes.map { (time, keyframe) ->
                                Animation.Keyframe(
                                    time = time,
                                    value = keyframe.vector.map(mapper),
                                )
                            }.sortedBy { it.time })
                        }
                    }

                    val position = timeline(bone.node(POSITION)) { it / SCALE_FACTOR }
                    val rotation = timeline(bone.node(ROTATION)) { radians(it) }
                    val scale = timeline(bone.node(SCALE)) { it }

                    boneKey.toString() to Animation.BoneDescriptor(
                        pivot = boneGeometry.pivot,
                        position = position,
                        rotation = rotation,
                        scale = scale,
                    )
                }.associate { it },
            )
        }.associate { it }
    }
}
