package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.{TileEntity => MCTileEntity, TileEntitySign, TileEntityFurnace}
import scala.Some
import li.cil.oc.common.tileentity.Rotatable
import li.cil.oc.util.RotationHelper._
import net.minecraftforge.common.ForgeDirection
import li.cil.oc.util.RotationHelper

class Reader(val owner: MCTileEntity) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("reader", Visibility.Neighbors).
    withConnector().
    create()


  // ----------------------------------------------------------------------- //

  @LuaCallback("read")
  def read(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val player = context.player()
    val d = RotationHelper.fromYaw(player.rotationYaw)
    val te = player.getEntityWorld.getBlockTileEntity(player.posX.floor.toInt + d.offsetX, player.posY.floor.toInt + d.offsetY, player.posZ.floor.toInt + d.offsetZ)
    te match {
      case sign: TileEntitySign => {
        val text = sign.signText.mkString("\n")

        return result(text)
      }
      case _ =>
    }

    result(Unit, "no sign")
  }

  @LuaCallback("write")
  def write(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val player = context.player()
    val d = RotationHelper.fromYaw(player.rotationYaw)
    val te = player.getEntityWorld.getBlockTileEntity(player.posX.floor.toInt + d.offsetX, player.posY.floor.toInt + d.offsetY, player.posZ.floor.toInt + d.offsetZ)
    te match {
          case sign: TileEntitySign => {
            val text = args.checkString(0).split("\n")
            val number = Math.min(4, text.size)
            for (i <- 0 to number - 1) {
              var line = text(i)
              if (line.size > 15) {
                line = line.substring(0, 15)
              }
              sign.signText(i) = line
            }

            sign.worldObj.markBlockForUpdate(player.posX.floor.toInt + d.offsetX, player.posY.floor.toInt + d.offsetY, player.posZ.floor.toInt + d.offsetZ)
            return result(true)
          }
          case _ =>
        }
    result(Unit, "no sign")
  }


  // ----------------------------------------------------------------------- //

  override val canUpdate = false


  // ----------------------------------------------------------------------- //


}
