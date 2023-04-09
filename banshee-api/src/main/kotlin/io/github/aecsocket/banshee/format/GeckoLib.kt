package io.github.aecsocket.banshee.format

import io.github.aecsocket.alexandria.extension.force
import io.github.aecsocket.banshee.*
import io.github.aecsocket.klam.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import kotlin.math.max
import kotlin.math.min

private const val EASING = "easing"
private const val EASING_ARGS = "easingArgs"
private const val VECTOR = "vector"

object GeckoLib {
    @ConfigSerializable
    data class GeometryData(
        // this is actually an array, but we only care about the first value
        @Setting("minecraft:geometry") val geometry: Geometry = Geometry(),
    ) {
        @ConfigSerializable
        data class Geometry(
            @Setting("bones") val bones: MutableList<Bone> = ArrayList(),
        ) {
            @ConfigSerializable
            data class Bone(
                @Required @Setting("name") val name: String,
                @Setting("pivot") val pivot: FVec3 = FVec3(0.0f),
            )
        }

        data class BoneMap(
            private val all: Map<String, Geometry.Bone>,
        ) : Map<String, Geometry.Bone> by all
    }

    @ConfigSerializable
    data class AnimationData(
        @Required @Setting("animations") private val animations: MutableMap<String, GLAnimation>,
    ) : Map<String, GLAnimation> by animations

    @ConfigSerializable
    data class GLAnimation(
        @Required @Setting("animation_length") val animationLength: Float,
        @Setting("loop") val loop: Loop = Loop.FALSE,
        @Setting("override_previous_animation") val overridePreviousAnimation: Boolean = false,
        @Setting("anim_time_update") val animTimeUpdate: Float = 0.0f,
        @Setting("blend_weight") val blendWeight: Float = 0.0f,
        @Setting("start_delay") val startDelay: Float = 0.0f,
        @Setting("loop_delay") val loopDelay: Float = 0.0f,
        @Setting("bones") val bones: MutableMap<String, Properties> = HashMap(),
    ) {
        enum class Loop {
            FALSE,
            HOLD_ON_LAST_FRAME,
            TRUE,
        }

        @ConfigSerializable
        data class Properties(
            @Setting("rotation") val rotation: Keyframes = Keyframes(),
            @Setting("position") val position: Keyframes = Keyframes(),
            @Setting("scale") val scale: Keyframes = Keyframes(),
        )

        sealed interface Easing {
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
        data class Keyframe(
            @Required @Setting("vector") val vector: FVec3,
            @Setting(nodeFromParent = true) val easing: Easing = Easing.Linear,
        )

        data class TimedKeyframe(
            val time: Float,
            val keyframe: Keyframe,
        )

        data class Keyframes(
            private val all: MutableList<TimedKeyframe> = ArrayList(),
        ) : List<TimedKeyframe> by all {
            object Serializer : TypeSerializer<Keyframes> {
                override fun serialize(type: Type, obj: Keyframes?, node: ConfigurationNode) {}
                override fun deserialize(type: Type, node: ConfigurationNode): Keyframes {
                    return if (node.hasChild(VECTOR)) {
                        Keyframes(
                            mutableListOf(
                                TimedKeyframe(
                                    time = 0.0f,
                                    keyframe = node.force()
                                )
                            )
                        )
                    } else {
                        val all = node.force<MutableMap<Float, Keyframe>>()
                        Keyframes(all.toSortedMap().map { (time, keyframe) ->
                            TimedKeyframe(time, keyframe)
                        }.toMutableList())
                    }
                }
            }
        }

        fun createAnimation(): GLAnimation {
            val bones = bones.map { (key, properties) ->
                key to object : AnimationBone {
                    override val transform = object : AnimationCurve<AnimatedTransform> {
                        override fun get(time: Float): AnimatedTransform {
                            val adjTime = max(
                                0.0f, when (loop) {
                                    Loop.FALSE -> if (time > animationLength) 0.0f else time
                                    Loop.HOLD_ON_LAST_FRAME -> min(time, animationLength)
                                    Loop.TRUE -> time % animationLength
                                }
                            )

                            fun at(keyframes: Keyframes, default: FVec3): FVec3 {
                                val index = keyframes.indexOfLast { adjTime >= it.time }
                                return if (index == -1) default else {
                                    val start = keyframes[index]
                                    val end = if (index == keyframes.size - 1) start else keyframes[index + 1]
                                    val timeDelta = end.time - start.time

                                    mix(
                                        start.keyframe.vector,
                                        end.keyframe.vector,
                                        if (timeDelta == 0.0f) 0.0f else (adjTime - start.time) / timeDelta,
                                    )
                                }
                            }

                            val position = at(properties.position, FVec3(0.0f))
                            val rotation = at(properties.rotation, FVec3(0.0f))
                            val scale = at(properties.scale, FVec3(1.0f))

                            return AnimatedTransform(
                                DAffine3(
                                    DVec3(position / 16.0f),
                                    asQuat(rotation.map { radians(it) }, EulerOrder.XYZ)
                                ),
                                scale,
                            )
                        }
                    }
                }
            }.associate { it }
            return object : GLAnimation {
                override fun bone(key: String) = bones[key]
                    ?: throw IllegalArgumentException("Invalid bone '$key'")
            }
        }
    }

    fun loadBoneMap(node: ConfigurationNode): GeometryData.BoneMap {
        val geometry = node.force<GeometryData>()
        return GeometryData.BoneMap(geometry.geometry.bones.map { bone ->
            bone.name to bone
        }.associate { it })
    }

    fun loadAnimations(boneMap: GeometryData.BoneMap, node: ConfigurationNode): Map<String, Animation> {
        return node.force<AnimationData>().map { (animationKey, animation) ->
            val bones = animation.bones.map { (boneKey, bone) ->
                val boneGeometry = boneMap[boneKey]
                    ?: throw SerializationException(node, Animation::class.java, "Invalid ")

                fun asKeyframeList(keyframes: GLAnimation.Keyframes) = KeyframeList(keyframes.map { (time, keyframe) ->
                    Keyframe(time, keyframe.vector)
                }).toCurve()

                boneKey to AnimationBone(
                    pivot = boneMap[boneKey],
                    position = asKeyframeList(bone.position),
                    rotation = asKeyframeList(bone.rotation),
                    scale = asKeyframeList(bone.scale),
                )
            }.associate { it }
            animationKey to Animation(bones)
        }.associate { it }
    }
}
