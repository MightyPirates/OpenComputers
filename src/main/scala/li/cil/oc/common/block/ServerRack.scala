package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class ServerRack extends RedstoneAware with traits.SpecialBlock with traits.PowerAcceptor {
  // TODO remove
//  override protected def customTextures = Array(
//    None,
//    None,
//    Some("ServerRackSide"),
//    Some("ServerRackFront"),
//    Some("ServerRackSide"),
//    Some("ServerRackSide")
//  )
//
//  override def registerBlockIcons(iconRegister: IIconRegister) = {
//    super.registerBlockIcons(iconRegister)
//    System.arraycopy(icons, 0, Textures.ServerRack.icons, 0, icons.length)
//  }

  @SideOnly(Side.CLIENT)
  override def getMixedBrightnessForBlock(world: IBlockAccess, pos: BlockPos) = {
    world.getTileEntity(pos) match {
      case rack: tileentity.ServerRack =>
        def brightness(pos: BlockPos) = world.getCombinedLight(pos, getLightValue(world, pos))
        val value = brightness(pos.offset(rack.facing))
        val skyBrightness = (value >> 20) & 15
        val blockBrightness = (value >> 4) & 15
        ((skyBrightness * 3 / 4) << 20) | ((blockBrightness * 3 / 4) << 4)
      case _ => super.getMixedBrightnessForBlock(world, pos)
    }
  }

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.SOUTH

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.serverRackRate

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.ServerRack()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Rack.id, world, pos.getX, pos.getY, pos.getZ)
      }
      true
    }
    else false
  }
}
