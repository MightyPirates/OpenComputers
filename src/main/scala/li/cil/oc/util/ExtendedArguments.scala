package li.cil.oc.util

import com.google.common.base.Charsets
import li.cil.oc.api.internal.Robot
import li.cil.oc.api.machine.Arguments
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTSizeTracker
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.language.implicitConversions

object ExtendedArguments {

  implicit def extendedArguments(args: Arguments) = new ExtendedArguments(args)

  class ExtendedArguments(val args: Arguments) {
    def optionalItemCount(n: Int) =
      if (args.count > n && args.checkAny(n) != null) {
        math.max(0, math.min(64, args.checkInteger(n)))
      }
      else 64

    def optionalFluidCount(n: Int) =
      if (args.count > n && args.checkAny(n) != null) {
        math.max(0, args.checkInteger(n))
      }
      else 1000

    def checkSlot(inventory: IInventory, n: Int) = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot >= inventory.getSizeInventory) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot
    }

    def checkSlot(robot: Robot, n: Int) = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot >= robot.inventorySize) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot + 1 + robot.containerCount
    }

    def checkTank(robot: Robot, n: Int) = {
      val tank = args.checkInteger(n) - 1
      if (tank < 0 || tank >= robot.tankCount) {
        throw new IllegalArgumentException("invalid tank index")
      }
      tank
    }

    def checkSideForAction(n: Int) = checkSide(n, ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.DOWN)

    def checkSideForMovement(n: Int) = checkSide(n, ForgeDirection.SOUTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.DOWN)

    def checkSideForFace(n: Int, facing: ForgeDirection) = checkSide(n, ForgeDirection.VALID_DIRECTIONS.filter(_ != facing.getOpposite): _*)

    def checkSide(n: Int, allowed: ForgeDirection*) = {
      val side = args.checkInteger(n)
      if (side < 0 || side > 5) {
        throw new IllegalArgumentException("invalid side")
      }
      val direction = ForgeDirection.getOrientation(side)
      if (allowed.isEmpty || (allowed contains direction)) direction
      else throw new IllegalArgumentException("unsupported side")
    }

    def checkItemStack(n: Int) = {
      val map = args.checkTable(n)
      map.get("name") match {
        case name: String =>
          val damage = map.get("damage") match {
            case number: Number => number.intValue()
            case _ => 0
          }
          val tag = map.get("tag") match {
            case ba: Array[Byte] => toNbtTagCompound(ba)
            case s: String => toNbtTagCompound(s.getBytes(Charsets.UTF_8))
            case _ => None
          }
          makeStack(name, damage, tag)
        case _ => throw new IllegalArgumentException("invalid item stack")
      }
    }
  }

  private def makeStack(name: String, damage: Int, tag: Option[NBTTagCompound]) = {
    Item.itemRegistry.getObject(name) match {
      case item: Item =>
        val stack = new ItemStack(item, 1, damage)
        tag.foreach(stack.setTagCompound)
        stack
      case _ => throw new IllegalArgumentException("invalid item stack")
    }
  }

  private def toNbtTagCompound(data: Array[Byte]) = Option(CompressedStreamTools.func_152457_a(data, NBTSizeTracker.field_152451_a))
}
