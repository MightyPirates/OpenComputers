package li.cil.oc.common.block

import cpw.mods.fml.common.Optional
import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.Tooltip
import li.cil.oc.{Settings, OpenComputers}
import mcp.mobius.waila.api.IWailaConfigHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{StatCollector, Icon}
import net.minecraftforge.common.ForgeDirection
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.world.World
import li.cil.oc.client.Textures

class Disassembler(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "Disassembler"

  private val icons = Array.fill[Icon](6)(null)

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":disassembler_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":disassembler_side")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)

    Textures.Disassembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":disassembler_side_on")
    Textures.Disassembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":disassembler_top_on")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Disassembler)

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Disassembler.id, world, x, y, z)
      }
      true
    }
    else false
  }
}
