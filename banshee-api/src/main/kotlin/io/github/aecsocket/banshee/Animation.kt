package io.github.aecsocket.banshee

import io.github.aecsocket.klam.DVec3
import io.github.aecsocket.klam.FVec3

data class Animation(
    val bones: Map<String, AnimationBone>,
) {
    fun bone(key: String) = bones[key]
        ?: throw IllegalArgumentException("Invalid bone '$key'")
}

data class AnimationBone(
    val pivot: FVec3,
    val position: AnimationCurve<FVec3>,
    val rotation: AnimationCurve<FVec3>,
    val scale: AnimationCurve<FVec3>,
)

fun interface AnimationCurve<T> {
    operator fun get(time: Float): T
}

data class Keyframe<T>(
    val time: Float,
    // TODO easing
    val value: T,
)

data class KeyframeList<T>(
    private val all: List<Keyframe<T>>,
) : List<Keyframe<T>> by all {
    fun toCurve(): AnimationCurve<T> {

    }

    // TODO option to compile to a curve (array lookup) as well
}
