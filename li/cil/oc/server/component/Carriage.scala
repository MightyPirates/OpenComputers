package li.cil.oc.server.component

import java.lang.reflect.InvocationTargetException
import li.cil.oc.api
import li.cil.oc.api.network._
import net.minecraft.nbt.NBTTagCompound
import scala.Some
import scala.language.existentials

class Carriage(controller: Object) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("carriage").
    create()

  private val (directions, setup, move) = try {
    val directions = Class.forName("JAKJ.RedstoneInMotion.Directions").getEnumConstants
    val clazz = Class.forName("JAKJ.RedstoneInMotion.CarriageControllerEntity")
    val methods = clazz.getDeclaredMethods
    val setup = methods.find(_.getName == "SetupMotion").orNull
    val move = methods.find(_.getName == "Move").orNull
    (directions, setup, move)
  } catch {
    case _: Throwable => (null, null, null)
  }

  private var shouldMove = false
  private var direction = 0
  private var simulating = false
  private var anchored = false
  private var moving = false

  // ----------------------------------------------------------------------- //

  @LuaCallback("move")
  def move(context: Context, args: Arguments): Array[Object] = {
    if (directions == null || setup == null || move == null)
      throw new Exception("Redstone in Motion not found")
    if (shouldMove || moving)
      throw new Exception("already moving")
    direction = args.checkInteger(0)
    if (direction < 0 || direction > directions.length)
      throw new ArrayIndexOutOfBoundsException("invalid direction")
    simulating = args.checkBoolean(1)
    anchored = args.checkBoolean(2)
    shouldMove = true
    result(true)
  }

  // ----------------------------------------------------------------------- //

  override def update() {
    super.update()
    if (shouldMove) {
      shouldMove = false
      moving = true
      var error: Option[Throwable] = None
      try {
        setup.invoke(controller, directions(direction), Boolean.box(simulating), Boolean.box(anchored))
        move.invoke(controller)
      } catch {
        case e: InvocationTargetException => error = Some(e.getCause)
        case e: Throwable => error = Some(e)
      }
      moving = false
      error match {
        case Some(e) => node.sendToReachable("computer.signal", "carriage_moved", Unit, Option(e.getMessage).getOrElse(e.toString))
        case _ => if (simulating || anchored) node.sendToReachable("computer.signal", "carriage_moved", Boolean.box(true))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (moving) {
      moving = false
      node.sendToReachable("computer.signal", "carriage_moved", Boolean.box(true))
    }
  }

  // ----------------------------------------------------------------------- //

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setBoolean("moving", moving)
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    moving = nbt.getBoolean("moving")
  }
}
