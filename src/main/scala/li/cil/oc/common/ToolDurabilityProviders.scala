package li.cil.oc.common

import java.lang.reflect.{Method, Modifier}

import li.cil.oc.OpenComputers
import net.minecraft.item.ItemStack

import scala.collection.mutable

object ToolDurabilityProviders {
  private val providers = mutable.ArrayBuffer.empty[Method]

  def add(name: String): Unit = try {
    providers += getStaticMethod(name, classOf[ItemStack])
  }
  catch {
    case t: Throwable => OpenComputers.log.warn("Failed registering tool durability provider.", t)
  }

  def getDurability(stack: ItemStack): Option[Double] = {
    for (provider <- providers) {
      val durability = tryInvokeStatic(provider, stack)(Double.NaN)
      if (!durability.isNaN) return Option(durability)
    }
    // Fall back to vanilla damage values.
    if (stack.isItemStackDamageable) Option(1.0 - stack.getItemDamage.toDouble / stack.getMaxDamage.toDouble)
    else None
  }

  private def getStaticMethod(name: String, signature: Class[_]*) = {
    val nameSplit = name.lastIndexOf('.')
    val className = name.substring(0, nameSplit)
    val methodName = name.substring(nameSplit + 1)
    val clazz = Class.forName(className)
    val method = clazz.getDeclaredMethod(methodName, signature: _*)
    if (!Modifier.isStatic(method.getModifiers)) throw new IllegalArgumentException(s"Method $name is not static.")
    method
  }

  private def tryInvokeStatic[T](method: Method, args: AnyRef*)(default: T): T = try method.invoke(null, args: _*).asInstanceOf[T] catch {
    case t: Throwable =>
      OpenComputers.log.warn(s"Error invoking callback ${method.getDeclaringClass.getCanonicalName + "." + method.getName}.", t)
      default
  }
}
