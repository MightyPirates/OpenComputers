package li.cil.oc.common.items

import li.cil.oc.{CreativeTab, Config}
import net.minecraft.item.Item
import net.minecraft.world.World

class ItemRedstoneCard extends Item(Config.itemGPUId + 1) {
  setMaxStackSize(1)
  //setHasSubtypes(true)
  setUnlocalizedName("Redstone Card")
  setCreativeTab(CreativeTab)

  override def shouldPassSneakingClickToBlock(world: World, x: Int, y: Int, z: Int) = true
}
