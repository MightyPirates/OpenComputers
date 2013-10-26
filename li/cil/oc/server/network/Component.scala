package li.cil.oc.server.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import li.cil.oc.api
import li.cil.oc.api.network.environment.{Environment, LuaCallback}
import li.cil.oc.api.network.{Message, Visibility}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import scala.collection.{immutable, mutable}

class Component(host: Environment, name: String, reachability: Visibility) extends Node(host, name, reachability) with api.network.Component {
  private val luaCallbacks = Component.callbacks(host.getClass)

  private var visibility_ = reachability

  def visibility = visibility_

  def visibility(value: Visibility) = {
    if (value.ordinal() > visibility.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name +
        "' node with reachability '" + visibility + "'. It will be limited to the node's reachability.")
    }
    if (value != visibility_ && FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) {
      if (network != null) visibility_ match {
        case Visibility.Neighbors => value match {
          case Visibility.Network =>
            val neighbors = network.neighbors(this).toSet
            val visible = network.nodes(this)
            val delta = visible.filterNot(neighbors.contains)
            delta.foreach(node => network.sendToAddress(this, node.address, "computer.signal", "component_added"))
          case Visibility.None => network.sendToNeighbors(this, "computer.signal", "component_removed")
          case _ => // Cannot happen, but avoids compiler warnings.
        }
        case Visibility.Network => value match {
          case Visibility.Neighbors =>
            val neighbors = network.neighbors(this).toSet
            val visible = network.nodes(this)
            val delta = visible.filterNot(neighbors.contains)
            delta.foreach(node => network.sendToAddress(this, node.address, "computer.signal", "component_removed"))
          case Visibility.None => network.sendToVisible(this, "computer.signal", "component_removed")
          case _ => // Cannot happen, but avoids compiler warnings.
        }
        case Visibility.None => value match {
          case Visibility.Neighbors => network.sendToNeighbors(this, "computer.signal", "component_added")
          case Visibility.Network => network.sendToVisible(this, "computer.signal", "component_added")
          case _ => // Cannot happen, but avoids compiler warnings.
        }
      }
      visibility_ = value
    }
  }

  def canBeSeenBy(other: api.network.Node) = network == null || (visibility match {
    case Visibility.None => false
    case Visibility.Network => true
    case Visibility.Neighbors => other.network.neighbors(other).exists(_ == this)
  })

  // ----------------------------------------------------------------------- //

  override def receive(message: Message) = {
    if (message.name == "computer.started" && canBeSeenBy(message.source))
      network.sendToAddress(this, message.source.address, "computer.signal", "component_added")

    if (message.name == "component.methods")
      if (canBeSeenBy(message.source))
        Array(luaCallbacks.keys.toSeq: _*)
      else null
    else if (message.name == "component.call")
      luaCallbacks.get(message.name) match {
        case Some(callback) => callback(host, message)
        case _ => throw new NoSuchMethodException()
      }
    else super.receive(message)
  }

  // ----------------------------------------------------------------------- //

  override def save(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("visibility"))
      visibility_ = Visibility.values()(nbt.getInteger("visibility"))
  }

  override def load(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger("visibility", visibility_.ordinal())
  }
}

object Component {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, (Object, api.network.Message) => Array[Object]]]

  def callbacks(clazz: Class[_]) = cache.getOrElseUpdate(clazz, analyze(clazz))

  private def analyze(clazz: Class[_]) = {
    val callbacks = mutable.Map.empty[String, (Object, api.network.Message) => Array[Object]]
    var c = clazz
    while (c != classOf[Object]) {
      val ms = c.getDeclaredMethods

      ms.filter(_.isAnnotationPresent(classOf[LuaCallback])).foreach(m =>
        if (m.getParameterTypes.size != 1 || m.getParameterTypes.head != classOf[api.network.Message]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (method must take exactly one Message parameter).")
        }
        else if (m.getReturnType != classOf[Array[Object]]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (method must return an array of objects).")
        }
        else {
          val a = m.getAnnotation[LuaCallback](classOf[LuaCallback])
          if (a.value == null || a.value == "") {
            throw new IllegalArgumentException("Invalid use of LuaCallback annotation (name must not be null or empty).")
          }
          else if (!callbacks.contains(a.value)) {
            callbacks += a.value -> ((o, e) => m.invoke(o, e).asInstanceOf[Array[Object]])
          }
        }
      )

      c = c.getSuperclass
    }
    callbacks.toMap
  }
}