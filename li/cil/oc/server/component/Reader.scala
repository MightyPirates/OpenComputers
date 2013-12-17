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

class Reader(val owner: MCTileEntity) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("reader", Visibility.Neighbors).
    withConnector().
    create()


  // ----------------------------------------------------------------------- //

  @LuaCallback("read")
  def read(context: RobotContext, args: Arguments): Array[AnyRef] = {
    owner match {
      case rotatable: Rotatable => {
        val te = rotatable.getWorldObj.getBlockTileEntity(rotatable.xCoord+rotatable.facing.offsetX,rotatable.yCoord+rotatable.facing.offsetY,rotatable.zCoord+rotatable.facing.offsetZ)
        te match{
          case sign:TileEntitySign=>{
            val text = sign.signText.mkString("\n")

            return result(text)
          }
          case _=>
        }
      }
      case _ =>
    }
    result(Unit, "no sign")
  }

  @LuaCallback("write")
  def write(context: Context, args: Arguments): Array[AnyRef] = {
    owner match {
      case rotatable: Rotatable => {
        val te = rotatable.getWorldObj.getBlockTileEntity(rotatable.xCoord+rotatable.facing.offsetX,rotatable.yCoord+rotatable.facing.offsetY,rotatable.zCoord+rotatable.facing.offsetZ)
        te match{
          case sign:TileEntitySign=>{
            val text = args.checkString(0).split("\n")
            val number = Math.min(4,text.size)
            for(i <-0 to number-1){
              var line = text(i)
              if(line.size>15){
                line = line.substring(0,15)
              }
              sign.signText(i)= line
            }

            sign.worldObj.markBlockForUpdate(rotatable.xCoord+rotatable.facing.offsetX,rotatable.yCoord+rotatable.facing.offsetY,rotatable.zCoord+rotatable.facing.offsetZ)
            return result(true)
          }
          case _=>
        }
      }
      case _ =>
    }
    result(Unit, "no sign")
  }


  // ----------------------------------------------------------------------- //

  override val canUpdate = false


  // ----------------------------------------------------------------------- //


}
