package li.cil.oc.common.block

import java.util

import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Disassembler(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    None,
    Some("DisassemblerTop"),
    Some("DisassemblerSide"),
    Some("DisassemblerSide"),
    Some("DisassemblerSide"),
    Some("DisassemblerSide")
  )

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName, (Settings.get.disassemblerBreakChance * 100).toInt.toString))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    Textures.Disassembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":DisassemblerSideOn")
    Textures.Disassembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":DisassemblerTopOn")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Disassembler())

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
