package li.cil.oc.common.item

import appeng.api.AEApi
import appeng.api.parts.IPartItem
import appeng.core.CreativeTab
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api
import li.cil.oc.integration.appeng.PartP2POCNode
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class AppliedEnergisticsP2PTunnel extends SimpleItem with IPartItem {
  override def createPartFromItemStack(stack: ItemStack) = new PartP2POCNode(stack)

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) =
    AEApi.instance().partHelper().placeBus(stack, x, y, z, side, player, world) || super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)

  @SideOnly(Side.CLIENT)
  override def getSpriteNumber = 0

  @SideOnly(Side.CLIENT)
  override def getIconFromDamage(damage: Int) = api.Items.get("adapter").block().getIcon(2, 0)

  // Override instead of setting manually to be independent of load order.
  @SideOnly(Side.CLIENT)
  override def getCreativeTab = CreativeTab.instance
}
