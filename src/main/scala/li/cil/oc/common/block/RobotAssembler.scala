package li.cil.oc.common.block

import java.util

import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class RobotAssembler(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "RobotAssembler"

  private val icons = Array.fill[Icon](6)(null)

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN || side == ForgeDirection.UP

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":assembler_top")

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":assembler_side")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)

    Textures.RobotAssembler.iconSideAssembling = iconRegister.registerIcon(Settings.resourceDomain + ":assembler_side_assembling")
    Textures.RobotAssembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":assembler_side_on")
    Textures.RobotAssembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":assembler_top_on")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.RobotAssembler)

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.RobotAssembler.id, world, x, y, z)
      }
      true
    }
    else false
  }
}
