package li.cil.oc.util.mods

import java.lang.reflect.InvocationTargetException
import net.minecraft.block.Block
import net.minecraft.item.{ItemBlock, ItemStack}
import scala.language.existentials

object RedstoneInMotion {
  private val (controller, setup, move, motionException, obstructionException, obstructionX, obstructionY, obstructionZ, directions, blocks) = try {
    val controller = Class.forName("JAKJ.RedstoneInMotion.CarriageControllerEntity")
    val motionException = Class.forName("JAKJ.RedstoneInMotion.CarriageMotionException")
    val obstructionException = Class.forName("JAKJ.RedstoneInMotion.CarriageObstructionException")
    val directions = Class.forName("JAKJ.RedstoneInMotion.Directions").getEnumConstants
    val blocks = Class.forName("JAKJ.RedstoneInMotion.Blocks")

    val methods = controller.getDeclaredMethods
    val setup = methods.find(_.getName == "SetupMotion").get
    val move = methods.find(_.getName == "Move").get

    val obstructionX = obstructionException.getDeclaredField("X")
    val obstructionY = obstructionException.getDeclaredField("Y")
    val obstructionZ = obstructionException.getDeclaredField("Z")

    (Option(controller), setup, move, motionException, obstructionException, obstructionX, obstructionY, obstructionZ, directions, blocks)
  }
  catch {
    case _: Throwable => (None, null, null, null, null, null, null, null, null, null)
  }

  def available = controller.isDefined

  def isCarriageController(value: AnyRef) = controller match {
    case Some(clazz) => clazz.isAssignableFrom(value.getClass)
    case _ => false
  }

  def isCarriageController(stack: ItemStack) = available && stack != null && (stack.getItem match {
    case itemBlock: ItemBlock =>
      val block = Block.blocksList(itemBlock.getBlockID)
      block != null && driveBlock != null && block.blockID == driveBlock.blockID && itemBlock.getMetadata(stack.getItemDamage) == 2
    case _ => false
  })

  def move(controller: AnyRef, direction: Int, simulating: Boolean, anchored: Boolean): (Boolean, Array[AnyRef]) = {
    if (!isCarriageController(controller))
      throw new IllegalArgumentException("Not a carriage controller.")

    try {
      try {
        setup.invoke(controller, directions(direction), Boolean.box(simulating), Boolean.box(anchored))
        move.invoke(controller)
        (true, Array.empty)
      }
      catch {
        case e: InvocationTargetException => throw e.getCause
      }
    }
    catch {
      case e: Exception if obstructionException.isAssignableFrom(e.getClass) =>
        val x = obstructionX.get(e)
        val y = obstructionY.get(e)
        val z = obstructionZ.get(e)
        (false, Array(e.getMessage, x, y, z))
      case e: Exception if motionException.isAssignableFrom(e.getClass) =>
        (false, Array(e.getMessage: AnyRef))
    }
  }

  private def driveBlock = try {
    blocks.getField("CarriageDrive").get(null).asInstanceOf[Block]
  }
  catch {
    case _: Throwable => null
  }
}
