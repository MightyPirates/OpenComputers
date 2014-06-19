package li.cil.oc.server.network

import java.lang.reflect.{InvocationTargetException, Method, Modifier}

import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.MethodWhitelist
import li.cil.oc.api.machine.Robot
import li.cil.oc.api.network
import li.cil.oc.api.network.{Context, ManagedPeripheral}
import li.cil.oc.server.driver.CompoundBlockEnvironment

import scala.collection.{immutable, mutable}

object Callbacks {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, Callback]]

  def apply(host: Any) = host match {
    case multi: CompoundBlockEnvironment => analyze(host)
    case peripheral: ManagedPeripheral => analyze(host)
    case _ => cache.getOrElseUpdate(host.getClass, analyze(host))
  }

  private def analyze(host: Any) = {
    val callbacks = mutable.Map.empty[String, Callback]
    val whitelists = mutable.Buffer.empty[Set[String]]
    val seeds = host match {
      case multi: CompoundBlockEnvironment => multi.environments.map {
        case (_, environment) =>
          environment match {
            case list: MethodWhitelist => whitelists += Option(list.whitelistedMethods).fold(Set.empty[String])(_.toSet)
            case _ =>
          }
          environment.getClass: Class[_]
      }
      case single => Seq(host.getClass: Class[_])
    }
    val whitelist = whitelists.reduceOption(_.intersect(_)).getOrElse(Set.empty[String])
    def shouldAdd(name: String) = !callbacks.contains(name) && (whitelist.isEmpty || whitelist.contains(name))
    for (seed <- seeds) {
      var c: Class[_] = seed
      while (c != classOf[Object]) {
        val ms = c.getDeclaredMethods

        ms.filter(_.isAnnotationPresent(classOf[network.Callback])).foreach(m =>
          if (m.getParameterTypes.size != 2 ||
            (m.getParameterTypes()(0) != classOf[Context] && m.getParameterTypes()(0) != classOf[Robot]) ||
            m.getParameterTypes()(1) != classOf[network.Arguments]) {
            OpenComputers.log.error("Invalid use of Callback annotation on %s.%s: invalid argument types or count.".format(m.getDeclaringClass.getName, m.getName))
          }
          else if (m.getReturnType != classOf[Array[AnyRef]]) {
            OpenComputers.log.error("Invalid use of Callback annotation on %s.%s: invalid return type.".format(m.getDeclaringClass.getName, m.getName))
          }
          else if (!Modifier.isPublic(m.getModifiers)) {
            OpenComputers.log.error("Invalid use of Callback annotation on %s.%s: method must be public.".format(m.getDeclaringClass.getName, m.getName))
          }
          else {
            val a = m.getAnnotation[network.Callback](classOf[network.Callback])
            val name = if (a.value != null && a.value.trim != "") a.value else m.getName
            if (shouldAdd(name)) {
              callbacks += name -> new ComponentCallback(m, a.direct, a.limit, a.doc)
            }
          }
        )

        c = c.getSuperclass
      }
    }
    host match {
      case multi: CompoundBlockEnvironment => multi.environments.map {
        case (_, environment) => environment match {
          case peripheral: ManagedPeripheral =>
            for (name <- peripheral.methods() if shouldAdd(name)) {
              callbacks += name -> new PeripheralCallback(name)
            }
          case _ =>
        }
      }
      case peripheral: ManagedPeripheral =>
        for (name <- peripheral.methods() if shouldAdd(name)) {
          callbacks += name -> new PeripheralCallback(name)
        }
      case _ =>
    }
    callbacks.toMap
  }

  // ----------------------------------------------------------------------- //

  abstract class Callback(val direct: Boolean, val limit: Int, val doc: String = "") {
    def apply(instance: AnyRef, context: Context, args: network.Arguments): Array[AnyRef]
  }

  class ComponentCallback(val method: Method, direct: Boolean, limit: Int, doc: String) extends Callback(direct, limit, doc) {
    override def apply(instance: AnyRef, context: Context, args: network.Arguments) = try {
      method.invoke(instance, context, args).asInstanceOf[Array[AnyRef]]
    } catch {
      case e: InvocationTargetException => throw e.getCause
    }
  }

  class PeripheralCallback(val name: String) extends Callback(true, 100) {
    override def apply(instance: AnyRef, context: Context, args: network.Arguments) =
      instance match {
        case peripheral: ManagedPeripheral => peripheral.invoke(name, context, args)
        case _ => throw new NoSuchMethodException()
      }
  }

}
