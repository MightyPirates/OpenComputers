package li.cil.oc.common.block

import java.util

import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class Switch(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "Switch"

  private val icons = Array.fill[Icon](6)(null)

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":switch_top")

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":switch_side")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)

    Textures.Switch.iconSideActivity = iconRegister.registerIcon(Settings.resourceDomain + ":switch_side_active")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Switch)

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getBlockTileEntity(x, y, z) match {
      case switch: tileentity.Switch =>
        if (!player.isSneaking) {
          if (!world.isRemote) {
            player.openGui(OpenComputers, GuiType.Switch.id, world, x, y, z)
          }
          true
        }
        else false
    }
  }
}
