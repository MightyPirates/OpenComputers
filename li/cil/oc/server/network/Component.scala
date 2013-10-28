package li.cil.oc.server.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import java.lang.reflect.InvocationTargetException
import li.cil.oc.api
import li.cil.oc.api.network.environment.{Arguments, Context, Environment, LuaCallback}
import li.cil.oc.api.network.{Message, Visibility}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.{immutable, mutable}

class Component(host: Environment, name: String, reachability: Visibility) extends Node(host, name, reachability) with api.network.Component {
  private val luaCallbacks = Component.callbacks(host.getClass)

  private var visibility_ = reachability

  def visibility = visibility_

  def setVisibility(value: Visibility) = {
    if (value.ordinal() > reachability.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name +
        "' node with reachability '" + reachability + "'. It will be limited to the node's reachability.")
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
    if (message.name == "component.methods")
      if (canBeSeenBy(message.source))
        Array(luaCallbacks.map {
          case (method, (asynchronous, _)) => (method, asynchronous)
        }.toArray)
      else null
    else if (message.name == "component.invoke") {
      (message.source.host, luaCallbacks.get(message.data()(0).asInstanceOf[String])) match {
        case (computer: Context, Some((_, callback))) =>
          callback(host, computer, new Component.MessageArguments(message))
        case _ => throw new NoSuchMethodException()
      }
    }
    else super.receive(message)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("visibility"))
      visibility_ = Visibility.values()(nbt.getInteger("visibility"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger("visibility", visibility_.ordinal())
  }
}

object Component {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, (Boolean, (Object, Context, Arguments) => Array[Object])]]

  def callbacks(clazz: Class[_]) = cache.getOrElseUpdate(clazz, analyze(clazz))

  private def analyze(clazz: Class[_]) = {
    val callbacks = mutable.Map.empty[String, (Boolean, (Object, Context, Arguments) => Array[Object])]
    var c = clazz
    while (c != classOf[Object]) {
      val ms = c.getDeclaredMethods

      ms.filter(_.isAnnotationPresent(classOf[LuaCallback])).foreach(m =>
        if (m.getParameterTypes.size != 2 ||
          m.getParameterTypes()(0) != classOf[Context] ||
          m.getParameterTypes()(1) != classOf[api.network.environment.Arguments]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (invalid signature).")
        }
        else if (m.getReturnType != classOf[Array[Object]]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (invalid signature).")
        }
        else {
          val a = m.getAnnotation[LuaCallback](classOf[LuaCallback])
          if (a.value == null || a.value == "") {
            throw new IllegalArgumentException("Invalid use of LuaCallback annotation (name must not be null or empty).")
          }
          else if (!callbacks.contains(a.value)) {
            callbacks += a.value ->(a.asynchronous(), (o: Object, c: Context, a: Arguments) => try {
              m.invoke(o, c, a).asInstanceOf[Array[Object]]
            } catch {
              case e: InvocationTargetException => throw e.getCause
            })
          }
        }
      )

      c = c.getSuperclass
    }
    callbacks.toMap
  }

  // ----------------------------------------------------------------------- //

  class MessageArguments(val message: Message) extends Arguments {

    def iterator() = message.data.drop(1).iterator

    def count() = message.data.length - 1

    def checkAny(index: Int) = {
      checkIndex(index, "value")
      message.data()(index)
    }

    def checkBoolean(index: Int): Boolean = {
      checkIndex(index, "boolean")
      message.data()(index) match {
        case value: java.lang.Boolean => value
        case value => throw typeError(index, value, "boolean")
      }
    }

    def checkDouble(index: Int): Double = {
      checkIndex(index, "number")
      message.data()(index) match {
        case value: java.lang.Double => value
        case value => throw typeError(index, value, "number")
      }
    }

    def checkInteger(index: Int): Int = {
      checkIndex(index, "number")
      message.data()(index) match {
        case value: java.lang.Double => value.intValue
        case value => throw typeError(index, value, "number")
      }
    }

    def checkString(index: Int) =
      new String(checkByteArray(index), "UTF-8")

    def checkByteArray(index: Int): Array[Byte] = {
      checkIndex(index, "string")
      message.data()(index) match {
        case value: Array[Byte] => value
        case value => throw typeError(index, value, "string")
      }
    }

    private def checkIndex(index: Int, name: String) =
      if (index < 1) throw new IndexOutOfBoundsException()
      else if (message.data.length <= index) throw new IllegalArgumentException(
        "bad arguments #%d (%s expected, got no value)".
          format(index, name))

    private def typeError(index: Int, have: AnyRef, want: String) =
      new IllegalArgumentException(
        "bad argument #%d (%s expected, got %s)".
          format(index, want, typeName(have)))

    private def typeName(value: AnyRef): String = value match {
      case null => "nil"
      case _: java.lang.Boolean => "boolean"
      case _: java.lang.Double => "double"
      case _: java.lang.String => "string"
      case _: Array[Byte] => "string"
      case _ => value.getClass.getSimpleName
    }
  }

}