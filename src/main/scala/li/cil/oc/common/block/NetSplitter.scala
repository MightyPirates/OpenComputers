package li.cil.oc.common.block

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.BlockPosition
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class NetSplitter extends RedstoneAware {
  override protected def customTextures = Array(
    Some("NetSplitterTop"),
    Some("NetSplitterTop"),
    Some("NetSplitterSide"),
    Some("NetSplitterSide"),
    Some("NetSplitterSide"),
    Some("NetSplitterSide")
  )

  @SideOnly(Side.CLIENT)
  override def registerBlockIcons(iconRegister: IIconRegister): Unit = {
    super.registerBlockIcons(iconRegister)
    Textures.NetSplitter.iconOn = iconRegister.registerIcon(Settings.resourceDomain + ":NetSplitterOn")
  }

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection): Boolean = false

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World, metadata: Int) = new tileentity.NetSplitter()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (Wrench.holdsApplicableWrench(player, BlockPosition(x, y, z))) {
      val sideToToggle = if (player.isSneaking) side.getOpposite else side
      world.getTileEntity(x, y, z) match {
        case splitter: tileentity.NetSplitter =>
          if (!world.isRemote) {
            val oldValue = splitter.openSides(sideToToggle.ordinal())
            splitter.setSideOpen(sideToToggle, !oldValue)
          }
          true
        case _ => false
      }
    }
    else super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
  }
}
