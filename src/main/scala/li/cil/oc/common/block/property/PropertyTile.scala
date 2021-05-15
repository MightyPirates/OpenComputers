package li.cil.oc.common.block.property

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.property.IUnlistedProperty

object PropertyTile {
  final val Tile = new PropertyTile()
}

// Custom unlisted property used to pass a long tile entities to a block's renderer.
class PropertyTile extends IUnlistedProperty[TileEntity] {
  override def getName = "tile"

  override def isValid(value: TileEntity) = true

  override def getType = classOf[TileEntity]

  override def valueToString(value: TileEntity) = value.toString
}
