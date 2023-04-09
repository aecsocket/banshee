package io.github.aecsocket.banshee.paper

import io.github.aecsocket.alexandria.extension.force
import io.github.aecsocket.alexandria.paper.BaseCommand
import io.github.aecsocket.alexandria.paper.Context
import io.github.aecsocket.alexandria.paper.extension.position
import io.github.aecsocket.alexandria.paper.render.ModelDescriptor
import io.github.aecsocket.alexandria.paper.render.PlayerTracker
import io.github.aecsocket.banshee.format.GeckoLibAnimations
import io.github.aecsocket.glossa.messageProxy
import io.github.aecsocket.klam.*
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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
        val desc = ModelDescriptor(
            scale = scale,
            billboard = Display.Billboard.FIXED,
            item = ItemStack(Material.STONE),
        )
        val tracker = PlayerTracker { setOf(sender) }

        val origin = DAffine3(sender.eyeLocation.position(), FQuat.identity())

        val root = banshee.renders.createModel(desc, tracker, origin)
        root.spawn()
        val child = banshee.renders.createModel(desc, tracker, origin)
        child.spawn()
        val animations = banshee.configLoaderBuilder()
            .file(banshee.dataFolder.resolve("anim.json"))
            .build()
            .load()
            .force<GeckoLibAnimations>()
        val animation = animations["animation.model.new"]!!.createAnimation()

        val rootBone = animation.bone("root")
        val childBone = animation.bone("child1")

        val start = System.currentTimeMillis()
        banshee.scheduling.onEntity(sender).runRepeating { task ->
            val time = (System.currentTimeMillis() - start) / 1000.0f

            val (rootTransform, rootScale) = rootBone.transform[time]
            root.transform = origin * rootTransform
            root.scale = scale * rootScale

            val (childTransform, childScale) = childBone.transform[time]
            child.transform = origin * rootTransform * childTransform
            child.scale = scale * rootScale * childScale

            if (System.currentTimeMillis() > start + 10_000) {
                task.cancel()
                root.despawn()
                child.despawn()
            }
        }
    }
}
