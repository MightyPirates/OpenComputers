package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.util.mods.RedstoneInMotion
import net.minecraft.nbt.NBTTagCompound

class Carriage(controller: AnyRef) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("carriage").
    create()

  private val names = Map(
    "negy" -> 0, "posy" -> 1, "negz" -> 2, "posz" -> 3, "negx" -> 4, "posx" -> 5,
    "down" -> 0, "up" -> 1, "north" -> 2, "south" -> 3, "west" -> 4, "east" -> 5)

  private var anchored = false
  private var direction = 0
  private var simulating = false

  private var shouldMove = false
  private var moving = false

  private var signalDelay = 10

  // ----------------------------------------------------------------------- //

  @LuaCallback("move")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    direction = checkDirection(args)
    simulating = if (args.count > 1) args.checkBoolean(1) else false
    shouldMove = true
    context.pause(0.1)
    result(true)
  }

  @LuaCallback("simulate")
  def simulate(context: Context, args: Arguments): Array[AnyRef] = {
    // IMPORTANT: we have to do the simulation asynchronously, too, because
    // that may also try to persist the computer that called us, and it must
    // not be running when we do that.
    direction = checkDirection(args)
    simulating = true
    shouldMove = true
    context.pause(0.1)
    result(true)
  }

  @LuaCallback("getAnchored")
  def getAnchored(context: Context, args: Arguments): Array[AnyRef] =
    result(anchored)

  @LuaCallback("setAnchored")
  def setAnchored(context: Context, args: Arguments): Array[AnyRef] = {
    anchored = args.checkBoolean(0)
    result(anchored)
  }

  private def checkDirection(args: Arguments) = {
    if (!RedstoneInMotion.available)
      throw new Exception("Redstone in Motion not found")
    if (shouldMove || moving)
      throw new Exception("already moving")
    if (args.isString(0)) {
      val name = args.checkString(0).toLowerCase
      if (!names.contains(name))
        throw new IllegalArgumentException("invalid direction")
      names(name)
    }
    else {
      val index = args.checkInteger(0)
      if (index < 0 || index > 5)
        throw new ArrayIndexOutOfBoundsException("invalid direction")
      index
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    if (node != null && node.network != null && moving) {
      signalDelay = signalDelay - 1
      if (signalDelay <= 0) {
        moving = false
        node.sendToReachable("computer.signal", "carriage_moved", Boolean.box(true))
      }
    }
    super.update()
    if (shouldMove) {
      shouldMove = false
      moving = true
      try {
        val (ok, reason) = RedstoneInMotion.move(controller, direction, simulating, anchored)
        if (!ok || simulating || anchored) {
          // We won't get re-connected, so we won't send in onConnect. Do it here.
          node.sendToReachable("computer.signal", Seq("carriage_moved", Boolean.box(ok)) ++ reason: _*)
        }
      }
      catch {
        case e: Throwable =>
          node.sendToReachable("computer.signal", "carriage_moved", Boolean.box(false), Option(e.getMessage).getOrElse(e.toString))
      }
      finally {
        moving = false
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)
    nbt.setBoolean("moving", moving)
    nbt.setBoolean("anchored", anchored)
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    moving = nbt.getBoolean("moving")
    anchored = nbt.getBoolean("anchored")
  }
}
