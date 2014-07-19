package li.cil.oc.common.block

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class Rack(val parent: SpecialDelegator) extends RedstoneAware with SpecialDelegate {
  val unlocalizedName = "ServerRack"

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(Textures.Rack.icons(side.ordinal))

  override def registerIcons(iconRegister: IIconRegister) = {
    Textures.Rack.icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    Textures.Rack.icons(ForgeDirection.UP.ordinal) = Textures.Rack.icons(ForgeDirection.DOWN.ordinal)

    Textures.Rack.icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":rack_side")
    Textures.Rack.icons(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":rack_front")
    Textures.Rack.icons(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":rack_side")
    Textures.Rack.icons(ForgeDirection.EAST.ordinal) = Textures.Rack.icons(ForgeDirection.WEST.ordinal)
  }

  @SideOnly(Side.CLIENT)
  override def mixedBrightness(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    world.getTileEntity(x, y, z) match {
      case rack: tileentity.Rack =>
        def brightness(x: Int, y: Int, z: Int) = world.getLightBrightnessForSkyBlocks(x, y, z, parent.getLightValue(world, x, y, z))
        val value = brightness(x + rack.facing.offsetX, y + rack.facing.offsetY, z + rack.facing.offsetZ)
        val skyBrightness = (value >> 20) & 15
        val blockBrightness = (value >> 4) & 15
        ((skyBrightness * 3 / 4) << 20) | ((blockBrightness * 3 / 4) << 4)
      case _ => -1
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Rack())

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Rack.id, world, x, y, z)
      }
      true
    }
    else false
  }
}
