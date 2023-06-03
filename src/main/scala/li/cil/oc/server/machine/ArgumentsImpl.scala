package li.cil.oc.server.machine

import java.util

import com.google.common.base.Charsets
import li.cil.oc.api.machine.Arguments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTSizeTracker
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class ArgumentsImpl(val args: Seq[AnyRef]) extends Arguments {
  def iterator() = args.iterator

  def count() = args.length

  def checkAny(index: Int) = {
    checkIndex(index, "value")
    args(index) match {
      case Unit | None => null
      case arg => arg
    }
  }

  def optAny(index: Int, default: AnyRef) = {
    if (!isDefined(index)) default
    else checkAny(index)
  }

  def checkBoolean(index: Int) = {
    checkIndex(index, "boolean")
    args(index) match {
      case value: java.lang.Boolean => value
      case value => throw typeError(index, value, "boolean")
    }
  }

  def optBoolean(index: Int, default: Boolean) = {
    if (!isDefined(index)) default
    else checkBoolean(index)
  }

  def checkDouble(index: Int) = {
    checkIndex(index, "number")
    args(index) match {
      case value: java.lang.Number => value.doubleValue
      case value => throw typeError(index, value, "number")
    }
  }

  def optDouble(index: Int, default: Double) = {
    if (!isDefined(index)) default
    else checkDouble(index)
  }

  def checkInteger(index: Int) = {
    checkIndex(index, "integer")
    args(index) match {
      // TODO: The below is correct behaviour, but breaks existing OC1 code (f.e. file:read(math.huge))
      /* case value: java.lang.Double =>
        if (!java.lang.Double.isFinite(value) || value < java.lang.Integer.MIN_VALUE || value > java.lang.Integer.MAX_VALUE) {
          throw intError(index, value)
        } else {
          value.intValue
        }
      case value: java.lang.Float =>
        if (!java.lang.Float.isFinite(value) || value < java.lang.Integer.MIN_VALUE || value > java.lang.Integer.MAX_VALUE) {
          throw intError(index, value)
        } else {
          value.intValue
        }
      case value: java.lang.Long =>
        if (value < java.lang.Integer.MIN_VALUE || value > java.lang.Integer.MAX_VALUE) {
          throw intError(index, value)
        } else {
          value.intValue
        }
      case value: java.lang.Number => value.intValue
      */
      case value: java.lang.Double =>
        if (value > java.lang.Integer.MAX_VALUE)
          java.lang.Integer.MAX_VALUE
        else if (value < java.lang.Integer.MIN_VALUE)
          java.lang.Integer.MIN_VALUE
        else
          value.intValue
      case value: java.lang.Float =>
        if (value > java.lang.Integer.MAX_VALUE)
          java.lang.Integer.MAX_VALUE
        else if (value < java.lang.Integer.MIN_VALUE)
          java.lang.Integer.MIN_VALUE
        else
          value.intValue
      case value: java.lang.Long =>
        if (value > java.lang.Integer.MAX_VALUE)
          java.lang.Integer.MAX_VALUE
        else if (value < java.lang.Integer.MIN_VALUE)
          java.lang.Integer.MIN_VALUE
        else
          value.intValue
      case value: java.lang.Number => value.intValue
      case value => throw typeError(index, value, "integer")
    }
  }

  def optInteger(index: Int, default: Int) = {
    if (!isDefined(index)) default
    else checkInteger(index)
  }

  def checkLong(index: Int) = {
    checkIndex(index, "integer")
    args(index) match {
      // TODO: The below is correct behaviour, but breaks existing OC1 code (f.e. file:read(math.huge))
      /* case value: java.lang.Double =>
        if (!java.lang.Double.isFinite(value) || value < java.lang.Long.MIN_VALUE || value > java.lang.Long.MAX_VALUE) {
          throw intError(index, value)
        } else {
          value.longValue
        }
      case value: java.lang.Float =>
        if (!java.lang.Float.isFinite(value) || value < java.lang.Long.MIN_VALUE || value > java.lang.Long.MAX_VALUE) {
          throw intError(index, value)
        } else {
          value.longValue
        }
      case value: java.lang.Number => value.longValue
      */
      case value: java.lang.Double =>
        if (value > java.lang.Long.MAX_VALUE)
          java.lang.Long.MAX_VALUE
        else if (value < java.lang.Long.MIN_VALUE)
          java.lang.Long.MIN_VALUE
        else
          value.longValue
      case value: java.lang.Float =>
        if (value > java.lang.Long.MAX_VALUE)
          java.lang.Long.MAX_VALUE
        else if (value < java.lang.Long.MIN_VALUE)
          java.lang.Long.MIN_VALUE
        else
          value.longValue
      case value: java.lang.Number => value.longValue
      case value => throw typeError(index, value, "integer")
    }
  }

  def optLong(index: Int, default: Long) = {
    if (!isDefined(index)) default
    else checkLong(index)
  }

  def checkString(index: Int) = {
    checkIndex(index, "string")
    args(index) match {
      case value: java.lang.String => value
      case value: Array[Byte] => new String(value, Charsets.UTF_8)
      case value => throw typeError(index, value, "string")
    }
  }

  def optString(index: Int, default: String) = {
    if (!isDefined(index)) default
    else checkString(index)
  }

  def checkByteArray(index: Int) = {
    checkIndex(index, "string")
    args(index) match {
      case value: java.lang.String => value.getBytes(Charsets.UTF_8)
      case value: Array[Byte] => value
      case value => throw typeError(index, value, "string")
    }
  }

  def optByteArray(index: Int, default: Array[Byte]) = {
    if (!isDefined(index)) default
    else checkByteArray(index)
  }

  def checkTable(index: Int) = {
    checkIndex(index, "table")
    args(index) match {
      case value: java.util.Map[_, _] => value
      case value: Map[_, _] => value
      case value: mutable.Map[_, _] => value
      case value => throw typeError(index, value, "table")
    }
  }

  def optTable(index: Int, default: util.Map[_, _]) = {
    if (!isDefined(index)) default
    else checkTable(index)
  }

  def checkItemStack(index: Int) = {
    val map = checkTable(index)
    map.get("name") match {
      case name: String =>
        val damage = map.get("damage") match {
          case number: java.lang.Number => number.intValue
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

  def optItemStack(index: Int, default: ItemStack) = {
    if (!isDefined(index)) default
    else checkItemStack(index)
  }

  def isBoolean(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.Boolean => true
      case _ => false
    })

  def isDouble(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.Number => true
      case _ => false
    })

  def isInteger(index: Int) =
    index >= 0 && index < count && (args(index) match {
      // TODO: The below is correct behaviour, but may break existing OC1 code
      /* case value: java.lang.Double =>
        java.lang.Double.isFinite(value) && value >= java.lang.Integer.MIN_VALUE && value <= java.lang.Integer.MAX_VALUE
      case value: java.lang.Float =>
        java.lang.Float.isFinite(value) && value >= java.lang.Integer.MIN_VALUE && value <= java.lang.Integer.MAX_VALUE
      case value: java.lang.Long =>
        value >= java.lang.Integer.MIN_VALUE && value <= java.lang.Integer.MAX_VALUE */
      case value: java.lang.Number => true
      case _ => false
    })

  def isLong(index: Int) =
    index >= 0 && index < count && (args(index) match {
      // TODO: The below is correct behaviour, but may break existing OC1 code
      /* case value: java.lang.Double =>
        java.lang.Double.isFinite(value) && value >= java.lang.Long.MIN_VALUE && value <= java.lang.Long.MAX_VALUE
      case value: java.lang.Float =>
        java.lang.Float.isFinite(value) && value >= java.lang.Long.MIN_VALUE && value <= java.lang.Long.MAX_VALUE */
      case value: java.lang.Number => true
      case _ => false
    })

  def isString(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.String => true
      case value: Array[Byte] => true
      case _ => false
    })

  def isByteArray(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.String => true
      case value: Array[Byte] => true
      case _ => false
    })

  def isTable(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.util.Map[_, _] => true
      case value: Map[_, _] => true
      case value: mutable.Map[_, _] => true
      case _ => false
    })

  def isItemStack(index: Int) =
    isTable(index) && {
      val map = checkTable(index)
      map.get("name") match {
        case value: String => true
        case value: Array[Byte] => true
        case _ => false
      }
    }

  def toArray = args.map {
    case value: Array[Byte] => new String(value, Charsets.UTF_8)
    case value => value
  }.toArray

  private def isDefined(index: Int) = index >= 0 && index < args.length && args(index) != null

  private def checkIndex(index: Int, name: String) =
    if (index < 0) throw new IndexOutOfBoundsException()
    else if (args.length <= index) throw new IllegalArgumentException(
      s"bad arguments #${index + 1} ($name expected, got no value)")

  private def typeError(index: Int, have: AnyRef, want: String) =
    new IllegalArgumentException(
      s"bad argument #${index + 1} ($want expected, got ${typeName(have)})")

  private def intError(index: Int, have: AnyRef) =
    new IllegalArgumentException(
      s"bad argument #${index + 1} (${typeName(have)} has no integer representation)")

  private def typeName(value: AnyRef): String = value match {
    case null | Unit | None => "nil"
    case _: java.lang.Boolean => "boolean"
    case _: java.lang.Byte => "integer"
    case _: java.lang.Short => "integer"
    case _: java.lang.Integer => "integer"
    case _: java.lang.Long => "integer"
    case _: java.lang.Number => "number"
    case _: java.lang.String => "string"
    case _: Array[Byte] => "string"
    case value: java.util.Map[_, _] => "table"
    case value: Map[_, _] => "table"
    case value: mutable.Map[_, _] => "table"
    case _ => value.getClass.getSimpleName
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
