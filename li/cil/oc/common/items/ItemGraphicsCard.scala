package li.cil.oc.common.items

import li.cil.oc.Config
import li.cil.oc.CreativeTab
import net.minecraft.item.Item
import net.minecraft.world.World

class ItemGraphicsCard extends Item(Config.itemGPUId) {
  setMaxStackSize(1)
  setHasSubtypes(true)
  setUnlocalizedName("oc.gpu")
  setCreativeTab(CreativeTab)

  override def shouldPassSneakingClickToBlock(world: World, x: Int, y: Int, z: Int) = true

  /** Buffer is only needed on server, we synchronize it ourselves. */
  override def getShareTag = false
}