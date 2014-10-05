package li.cil.oc.common.block

import li.cil.oc.client.Textures
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Assembler extends SimpleBlock with traits.SpecialBlock with traits.PowerAcceptor {
  setLightLevel(0.34f)

  override protected def customTextures = Array(
    None,
    Some("AssemblerTop"),
    Some("AssemblerSide"),
    Some("AssemblerSide"),
    Some("AssemblerSide"),
    Some("AssemblerSide")
  )

  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    Textures.RobotAssembler.iconSideAssembling = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerSideAssembling")
    Textures.RobotAssembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerSideOn")
    Textures.RobotAssembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerTopOn")
  }

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN || side == ForgeDirection.UP

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.RobotAssembler()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
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
