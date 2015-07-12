package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Assembler extends SimpleBlock with traits.SpecialBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  ModColoredLights.setLightLevel(this, 0, 3, 5)

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
    Textures.Assembler.iconSideAssembling = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerSideAssembling")
    Textures.Assembler.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerSideOn")
    Textures.Assembler.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":AssemblerTopOn")
  }

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN || side == ForgeDirection.UP

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN || side == ForgeDirection.UP

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.assemblerRate

  override def guiType = GuiType.Assembler

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Assembler()
}
