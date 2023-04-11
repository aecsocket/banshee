package io.github.aecsocket.banshee.paper

import io.github.aecsocket.alexandria.Billboard
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
        val tracker = PlayerTracker { setOf(sender) }
        val basePosition = sender.location.position()
        val originTransform = FAffine3()

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
        val walkAnimation = animations["walk"]!!

        tree.walk { bone ->
            bone.animation = walkAnimation.bone(bone.key)
            bone.modelData?.let { modelData ->
                bone.render = banshee.renders.createModel(ModelDescriptor(
                    item = ItemStack(Material.STICK).withMeta<ItemMeta> {
                        it.setCustomModelData(modelData)
                    },
                    tracker = tracker,
                    billboard = Billboard.NONE,
                    interpolationDuration = 2,
                ), basePosition, originTransform).apply { spawn() }
            }
        }

        val start = System.currentTimeMillis()
        banshee.scheduling.onEntity(sender).runRepeating { task ->
            val time = (System.currentTimeMillis() - start) / 1000.0f

            fun update(bone: Bone, parentTransform: FAffine3) {
                val animTransform = bone.animation.transform[time]
                val thisTransform = parentTransform * animTransform
                bone.render?.let { render ->
                    render.transform = thisTransform
                }
                bone.children.forEach { child ->
                    update(child, thisTransform)
                }
            }

            update(tree, originTransform)

            if (System.currentTimeMillis() > start + 10_000) {
                task.cancel()
                tree.walk { bone ->
                    bone.render?.despawn()
                }
            }
        }
    }
}
