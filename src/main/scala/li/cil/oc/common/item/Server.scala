package li.cil.oc.common.item

import java.util
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.Tooltip
import li.cil.oc.{Settings, OpenComputers}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.world.World
import scala.collection.mutable

class Server(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "Server"
  val unlocalizedName = baseName + tier

  override def rarity = Array(EnumRarity.common, EnumRarity.uncommon, EnumRarity.rare).apply(tier max 0 min 2)

  override def maxStackSize = 1

  private object HelperInventory extends ServerInventory {
    def tier = Server.this.tier

    var container: ItemStack = null
  }

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(baseName, Settings.get.terminalsPerTier(tier)))
    HelperInventory.container = stack
    HelperInventory.reinitialize()
    val items = mutable.Map.empty[String, Int]
    for (item <- (0 until HelperInventory.getSizeInventory).map(HelperInventory.getStackInSlot) if item != null) {
      val itemName = item.getDisplayName
      items += itemName -> (if (items.contains(itemName)) items(itemName) + 1 else 1)
    }
    if (items.size > 0) {
      tooltip.addAll(Tooltip.get("Server.Components"))
      for (itemName <- items.keys.toArray.sorted) {
        tooltip.add("- " + items(itemName) + "x " + itemName)
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":server" + tier)
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Server.id, world, 0, 0, 0)
      }
      player.swingItem()
    }
    stack
  }

}
