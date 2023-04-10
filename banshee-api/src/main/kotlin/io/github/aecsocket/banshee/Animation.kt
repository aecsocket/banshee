package io.github.aecsocket.banshee

import io.github.aecsocket.alexandria.assertGtEq
import io.github.aecsocket.klam.*
import kotlin.math.min

fun interface AnimationCurve<T> {
    operator fun get(time: Float): T
}

class Animation(
    val length: Float,
    val loop: Loop,
    bones: Map<String, BoneDescriptor>,
) {
    enum class Loop {
        NONE,
        HOLD_LAST_FRAME,
        LOOP,
    }

    data class Keyframe<T>(
        val time: Float,
        // TODO easing
        val value: T,
    ) {
        init {
            assertGtEq("time", 0.0f, time)
        }
    }

    class Timeline<T>(
        private val all: List<Keyframe<T>>
    ) : List<Keyframe<T>> by all {
        fun validate() {
            repeat(all.size - 1) { i ->
                val now = all[i].time
                val next = all[i+1].time
                if (now >= next)
                    throw IllegalArgumentException("Keyframe ${i+1}: time $next is before $now")
            }
        }
    }

    data class BoneDescriptor(
        val pivot: FVec3,
        val position: Timeline<FVec3>,
        val rotation: Timeline<FVec3>,
        val scale: Timeline<FVec3>,
    )

    val bones = bones.map { (boneKey, bone) ->
        fun <T> asCurve(
            timeline: Timeline<T>,
            default: T,
            lerp: (a: T, b: T, f: Float) -> T,
        ): AnimationCurve<T> {
            timeline.validate()
            return timeline.lastOrNull()?.let { (_, lastValue) ->
                AnimationCurve { time ->
                    val adjTime = when (loop) {
                        Loop.NONE -> time
                        Loop.HOLD_LAST_FRAME -> min(time, length)
                        Loop.LOOP -> time % length
                    }

                    val endIndex = timeline.indexOfFirst { (frameTime) -> frameTime >= adjTime }
                    if (endIndex == -1) {
                        // time is past the last keyframe, hold at the end
                        return@AnimationCurve lastValue
                    }
                    val startIndex = endIndex - 1
                    if (startIndex < 0) {
                        // time is before the first keyframe, we snap to the first keyframe
                        // no interpolation between default and #0
                        return@AnimationCurve timeline[0].value
                    }

                    // we're between two valid keyframes, interpolate them
                    val start = timeline[startIndex]
                    val end = timeline[endIndex]
                    val factor = (adjTime - start.time) / (end.time - start.time)
                    // TODO smooth interpolation; determine factor by easing method
                    lerp(start.value, end.value, factor)
                }
            } ?: AnimationCurve { default }
        }

        boneKey to AnimationBone(
            pivot = bone.pivot,
            position = asCurve(bone.position, FVec3(0.0f), ::mix),
            rotation = asCurve(bone.rotation, FVec3(0.0f), ::mix),
            scale = asCurve(bone.scale, FVec3(1.0f), ::mix),
        )
    }.associate { it }

    fun bone(key: String) = bones[key] ?: emptyBone
}

data class AnimationTransform(
    val transform: DAffine3,
    val scale: FVec3,
)

class AnimationBone internal constructor(
    val pivot: FVec3,
    val position: AnimationCurve<FVec3>,
    val rotation: AnimationCurve<FVec3>,
    val scale: AnimationCurve<FVec3>,
) {
    val transform = AnimationCurve { time ->
        val rotation = asQuat(rotation[time], EulerOrder.XYZ)
        AnimationTransform(
            DAffine3(
                DVec3(position[time] - (rotation * pivot) + pivot),
                rotation,
            ),
            scale[time],
        )
    }
}

private val emptyBone = AnimationBone(
    pivot = FVec3(0.0f),
    position = { FVec3(0.0f) },
    rotation = { FVec3(0.0f) },
    scale = { FVec3(1.0f) },
)
