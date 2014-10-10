package li.cil.oc.server.machine

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.MethodWhitelist
import li.cil.oc.api.machine
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedPeripheral
import li.cil.oc.server.driver.CompoundBlockEnvironment

import scala.collection.immutable
import scala.collection.mutable

object Callbacks {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, Callback]]

  def apply(host: Any) = host match {
    case multi: CompoundBlockEnvironment => dynamicAnalyze(host)
    case peripheral: ManagedPeripheral => dynamicAnalyze(host)
    case _ => cache.getOrElseUpdate(host.getClass, dynamicAnalyze(host))
  }

  def fromClass(environment: Class[_]) = staticAnalyze(Seq(environment))

  private def dynamicAnalyze(host: Any) = {
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
    val whitelist = whitelists.reduceOption(_.intersect(_)).getOrElse(Set.empty)
    val callbacks = staticAnalyze(seeds, whitelist)
    def shouldAdd(name: String) = !callbacks.contains(name) && (whitelist.isEmpty || whitelist.contains(name))
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

  private def staticAnalyze(seeds: Seq[Class[_]], whitelist: Set[String] = Set.empty) = {
    val callbacks = mutable.Map.empty[String, Callback]
    def shouldAdd(name: String) = !callbacks.contains(name) && (whitelist.isEmpty || whitelist.contains(name))
    for (seed <- seeds) {
      var c: Class[_] = seed
      while (c != null && c != classOf[Object]) {
        val ms = c.getDeclaredMethods

        ms.filter(_.isAnnotationPresent(classOf[machine.Callback])).foreach(m =>
          if (m.getParameterTypes.size != 2 ||
            m.getParameterTypes()(0) != classOf[Context] ||
            m.getParameterTypes()(1) != classOf[Arguments]) {
            OpenComputers.log.error("Invalid use of Callback annotation on %s.%s: invalid argument types or count.".format(m.getDeclaringClass.getName, m.getName))
          }
          else if (m.getReturnType != classOf[Array[AnyRef]]) {
            OpenComputers.log.error("Invalid use of Callback annotation on %s.%s: invalid return type.".format(m.getDeclaringClass.getName, m.getName))
          }
          else if (!Modifier.isPublic(m.getModifiers)) {
            OpenComputers.log.error("Invalid use of Callback annotation on %s.%s: method must be public.".format(m.getDeclaringClass.getName, m.getName))
          }
          else {
            val a = m.getAnnotation[machine.Callback](classOf[machine.Callback])
            val name = if (a.value != null && a.value.trim != "") a.value else m.getName
            if (shouldAdd(name)) {
              callbacks += name -> new ComponentCallback(m, a)
            }
          }
        )

        c = c.getSuperclass
      }
    }
    callbacks
  }

  // ----------------------------------------------------------------------- //

  abstract class Callback(val annotation: machine.Callback) {
    def apply(instance: AnyRef, context: Context, args: Arguments): Array[AnyRef]
  }

  class ComponentCallback(val method: Method, annotation: machine.Callback) extends Callback(annotation) {
    override def apply(instance: AnyRef, context: Context, args: Arguments) = try {
      method.invoke(instance, context, args).asInstanceOf[Array[AnyRef]]
    } catch {
      case e: InvocationTargetException => throw e.getCause
    }
  }

  class PeripheralCallback(name: String) extends Callback(new PeripheralAnnotation(name)) {
    override def apply(instance: AnyRef, context: Context, args: Arguments) =
      instance match {
        case peripheral: ManagedPeripheral => peripheral.invoke(name, context, args)
        case _ => throw new NoSuchMethodException()
      }
  }

}
