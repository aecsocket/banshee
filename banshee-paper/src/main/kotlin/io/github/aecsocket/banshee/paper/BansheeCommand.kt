package io.github.aecsocket.banshee.paper

import io.github.aecsocket.alexandria.paper.BaseCommand
import io.github.aecsocket.alexandria.paper.Context
import io.github.aecsocket.alexandria.paper.extension.position
import io.github.aecsocket.alexandria.paper.extension.withMeta
import io.github.aecsocket.alexandria.paper.render.ModelDescriptor
import io.github.aecsocket.alexandria.paper.render.PaperRender
import io.github.aecsocket.alexandria.paper.render.PlayerTracker
import io.github.aecsocket.banshee.AnimationBone
import io.github.aecsocket.banshee.format.GeckoLib
import io.github.aecsocket.glossa.messageProxy
import io.github.aecsocket.klam.*
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

internal class BansheeCommand(
    private val banshee: Banshee
) : BaseCommand(banshee, banshee.glossa.messageProxy()) {
    init {
        manager.command(root
            .literal("test")
            .handler(::test)
        )
    }

    private fun test(ctx: Context) {
        val sender = ctx.sender as Player
        val scale = FVec3(1.0f)
        val tracker = PlayerTracker { setOf(sender) }
        val origin = DAffine3(sender.eyeLocation.position(), FQuat.identity())

        data class Bone(
            val key: String,
            val modelData: Int?,
            val children: List<Bone> = emptyList(),
        ) {
            var render: PaperRender? = null
            lateinit var animation: AnimationBone

            fun walk(block: (Bone) -> Unit) {
                block(this)
                children.forEach { it.walk(block) }
            }
        }

        val tree = Bone("pelvis", null, listOf(
            Bone("spine", 2, listOf(
                Bone("left_upper_arm", 9, listOf(
                    Bone("left_lower_arm", 7, listOf(
                        Bone("left_hand", 6)
                    ))
                )),
                Bone("right_upper_arm", 15, listOf(
                    Bone("right_lower_arm", 13, listOf(
                        Bone("right_hand", 12)
                    ))
                )),
                Bone("head", 3)
            )),
            Bone("left_thigh", 8, listOf(
                Bone("left_calf", 4, listOf(
                    Bone("left_foot", 5)
                ))
            )),
            Bone("right_thigh", 14, listOf(
                Bone("right_calf", 10, listOf(
                    Bone("right_foot", 11)
                ))
            ))
        ))

        val geometry = GeckoLib.deserializeGeometry(banshee.configLoaderBuilder()
            .file(banshee.dataFolder.resolve("player.geo.json"))
            .build().load())
        val animations = GeckoLib.deserializeAnimations(geometry, banshee.configLoaderBuilder()
            .file(banshee.dataFolder.resolve("player.animation.json"))
            .build().load())
        val animation = animations["walk"]!!

        tree.walk { bone ->
            bone.animation = animation.bone(bone.key)
            val modelData = bone.modelData ?: return@walk
            bone.render = banshee.renders.createModel(ModelDescriptor(
                scale = scale,
                billboard = Display.Billboard.FIXED,
                item = ItemStack(Material.STICK).withMeta<ItemMeta> {
                    it.setCustomModelData(modelData)
                },
            ), tracker, origin).apply { spawn() }
        }

        val start = System.currentTimeMillis()
        banshee.scheduling.onEntity(sender).runRepeating { task ->
            val time = (System.currentTimeMillis() - start) / 1000.0f

            fun update(bone: Bone, parentTransform: DAffine3, parentScale: FVec3) {
                val (animTransform, animScale) = tree.animation.transform[time]
                val thisTransform = parentTransform * animTransform
                val thisScale = parentScale * animScale
                bone.render?.let { render ->
                    render.transform = DAffine3(thisTransform)
                    render.scale = FVec3(thisScale)
                }
                bone.children.forEach { child ->
                    update(child, thisTransform, thisScale)
                }
            }

            update(tree, origin, scale)

            if (System.currentTimeMillis() > start + 10_000) {
                task.cancel()
                tree.walk { bone ->
                    bone.render?.despawn()
                }
            }
        }
    }
}
