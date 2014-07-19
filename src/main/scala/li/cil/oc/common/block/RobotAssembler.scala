package li.cil.oc.common.block

import li.cil.oc.client.Textures
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class RobotAssembler(val parent: SpecialDelegator) extends SpecialDelegate {
  override protected def customTextures = Array(
    None,
    Some("AssemblerTop"),
    Some("AssemblerSide"),
    Some("AssemblerSide"),
    Some("AssemblerSide"),
    Some("AssemblerSide")
  )

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN || side == ForgeDirection.UP

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    Textures.RobotAssembler.iconSideAssembling = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerSideAssembling")
    Textures.RobotAssembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerSideOn")
    Textures.RobotAssembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerTopOn")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.RobotAssembler())

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
