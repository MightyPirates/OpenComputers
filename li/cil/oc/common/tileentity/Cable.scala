package li.cil.oc.common.tileentity

import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection

class Cable extends Environment {
  val node = Network.newNode(this, Visibility.None).create()

  def neighbors = {
    var result = 0
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      worldObj.getBlockTileEntity(xCoord + side.offsetX, yCoord + side.offsetY, zCoord + side.offsetZ) match {
        case environment: Environment => result |= side.flag
        case _ =>
      }
    }
    result
  }

  def bounds = Cable.bounds(neighbors).copy()

  override def getRenderBoundingBox = bounds.offset(xCoord, yCoord, zCoord)
}

object Cable {
  private val bounds = {
    // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
    (0 to 0xFF >> 2).map(mask => {
      val bounds = AxisAlignedBB.getBoundingBox(-0.125, -0.125, -0.125, 0.125, 0.125, 0.125)
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        if ((side.flag & mask) != 0) {
          if (side.offsetX < 0) bounds.minX += side.offsetX * 0.375
          else bounds.maxX += side.offsetX * 0.375
          if (side.offsetY < 0) bounds.minY += side.offsetY * 0.375
          else bounds.maxY += side.offsetY * 0.375
          if (side.offsetZ < 0) bounds.minZ += side.offsetZ * 0.375
          else bounds.maxZ += side.offsetZ * 0.375
        }
      }
      bounds.setBounds(
        bounds.minX + 0.5, bounds.minY + 0.5, bounds.minZ + 0.5,
        bounds.maxX + 0.5, bounds.maxY + 0.5, bounds.maxZ + 0.5)
    }).toArray
  }
}