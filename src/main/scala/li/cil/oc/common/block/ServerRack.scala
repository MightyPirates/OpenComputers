package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.OpenComputers
import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class ServerRack(val parent: SpecialDelegator) extends RedstoneAware with SpecialDelegate {
  override protected def customTextures = Array(
    None,
    None,
    Some("ServerRackSide"),
    Some("ServerRackFront"),
    Some("ServerRackSide"),
    Some("ServerRackSide")
  )

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    System.arraycopy(icons, 0, Textures.ServerRack.icons, 0, icons.length)
  }

  @SideOnly(Side.CLIENT)
  override def mixedBrightness(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    world.getBlockTileEntity(x, y, z) match {
      case rack: tileentity.ServerRack =>
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

  override def createTileEntity(world: World) = Some(new tileentity.ServerRack())

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
