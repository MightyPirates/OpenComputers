package li.cil.oc.common.block

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Transposer extends SimpleBlock {
  override protected def customTextures = Array(
    Some("TransposerTop"),
    Some("TransposerTop"),
    Some("TransposerSide"),
    Some("TransposerSide"),
    Some("TransposerSide"),
    Some("TransposerSide")
  )

  @SideOnly(Side.CLIENT)
  override def registerBlockIcons(iconRegister: IIconRegister): Unit = {
    super.registerBlockIcons(iconRegister)
    Textures.Transposer.iconOn = iconRegister.registerIcon(Settings.resourceDomain + ":TransposerOn")
  }

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection): Boolean = false

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Transposer()
}
