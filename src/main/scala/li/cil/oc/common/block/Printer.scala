package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Printer extends SimpleBlock with traits.SpecialBlock with traits.StateAware with traits.GUI {
  override protected def customTextures = Array(
    None,
    Some("PrinterTop"),
    Some("PrinterSide"),
    Some("PrinterSide"),
    Some("PrinterSide"),
    Some("PrinterSide")
  )

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.Printer

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Printer()
}
