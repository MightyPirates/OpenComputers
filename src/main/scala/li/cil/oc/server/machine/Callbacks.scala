package li.cil.oc.server.machine

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.MethodWhitelist
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.FilteredEnvironment
import li.cil.oc.api.network.ManagedPeripheral
import li.cil.oc.server.driver.CompoundBlockEnvironment

import scala.collection.immutable
import scala.collection.mutable

object Callbacks {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, Callback]]

  def apply(host: Any) = host match {
    case multi: CompoundBlockEnvironment => dynamicAnalyze(host)
    case peripheral: ManagedPeripheral => dynamicAnalyze(host)
    case filtered: FilteredEnvironment => dynamicAnalyze(host)
    case _ => cache.getOrElseUpdate(host.getClass, dynamicAnalyze(host))
  }

  // Clear the cache; used when world is unloaded, mostly to allow reacting to
  // stuff (aka configs) that may influence which @Callbacks are enabled.
  def clear(): Unit = {
    cache.clear()
  }

  def fromClass(environment: Class[_]) = staticAnalyze(environment)

  private def dynamicAnalyze(host: Any) = {
    val whitelists = mutable.Buffer.empty[Set[String]]
    val callbacks = mutable.Map.empty[String, Callback]

    // Lazy val to allow referencing it in closures before it's actually
    // initialized after the base whitelist has been compiled.
    lazy val whitelist = whitelists.reduceOption(_.intersect(_)).getOrElse(Set.empty)
    def shouldAdd(name: String) = !callbacks.contains(name) && (whitelist.isEmpty || whitelist.contains(name))

    def process(environment: Any) = {
      environment match {
        case list: MethodWhitelist => whitelists += Option(list.whitelistedMethods).fold(Set.empty[String])(_.toSet)
        case _ =>
      }
      val priority = environment match {
        case named: NamedBlock => named.priority
        case _ => 0
      }
      val filter = environment match {
        case filtered: FilteredEnvironment => (s: String) => shouldAdd(s) && filtered.isCallbackEnabled(s)
        case _ => shouldAdd _
      }
      environment match {
        case peripheral: ManagedPeripheral =>
          (priority, () => {
            for (name <- peripheral.methods() if filter(name)) {
              callbacks += name -> new PeripheralCallback(name)
            }
            staticAnalyze(environment.getClass, Option(filter), Option(callbacks))
          })
        case _ =>
          (priority, () => staticAnalyze(environment.getClass, Option(filter), Option(callbacks)))
      }
    }

    // First collect whitelist and priority information, then sort and
    // fetch callbacks.
    (host match {
      case multi: CompoundBlockEnvironment => multi.environments.map(env => process(env._2))
      case single => Seq(process(single))
    }).sortBy(-_._1).map(_._2).foreach(_ ())

    callbacks.toMap
  }

  private def staticAnalyze(seed: Class[_], shouldAdd: Option[String => Boolean] = None, optCallbacks: Option[mutable.Map[String, Callback]] = None) = {
    val callbacks = optCallbacks.getOrElse(mutable.Map.empty[String, Callback])
    var c: Class[_] = seed
    while (c != null && c != classOf[Object]) {
      val ms = c.getDeclaredMethods

      ms.filter(_.isAnnotationPresent(classOf[machine.Callback])).foreach(m =>
        if (m.getParameterTypes.size != 2 ||
          m.getParameterTypes()(0) != classOf[Context] ||
          m.getParameterTypes()(1) != classOf[Arguments]) {
          OpenComputers.log.error(s"Invalid use of Callback annotation on ${m.getDeclaringClass.getName}.${m.getName}: invalid argument types or count.")
        }
        else if (m.getReturnType != classOf[Array[AnyRef]]) {
          OpenComputers.log.error(s"Invalid use of Callback annotation on ${m.getDeclaringClass.getName}.${m.getName}: invalid return type.")
        }
        else if (!Modifier.isPublic(m.getModifiers)) {
          OpenComputers.log.error(s"Invalid use of Callback annotation on ${m.getDeclaringClass.getName}.${m.getName}: method must be public.")
        }
        else {
          val a = m.getAnnotation[machine.Callback](classOf[machine.Callback])
          val name = if (a.value != null && a.value.trim != "") a.value else m.getName
          if (shouldAdd.fold(true)(_ (name))) {
            callbacks += name -> new ComponentCallback(m, a)
          }
        }
      )

      c = c.getSuperclass
    }
    callbacks
  }

  // ----------------------------------------------------------------------- //

  abstract class Callback(val annotation: machine.Callback) {
    def apply(instance: AnyRef, context: Context, args: Arguments): Array[AnyRef]
  }

  class ComponentCallback(val method: Method, annotation: machine.Callback) extends Callback(annotation) {
    final val callWrapper = CallbackWrapper.createCallbackWrapper(method)

    override def apply(instance: AnyRef, context: Context, args: Arguments) = callWrapper.call(instance, context, args)
  }

  class PeripheralCallback(name: String) extends Callback(new PeripheralAnnotation(name)) {
    override def apply(instance: AnyRef, context: Context, args: Arguments) =
      instance match {
        case peripheral: ManagedPeripheral => peripheral.invoke(name, context, args)
        case _ => throw new NoSuchMethodException()
      }
  }

}
