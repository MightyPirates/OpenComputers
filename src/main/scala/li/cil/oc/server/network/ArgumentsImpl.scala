package li.cil.oc.server.network

import li.cil.oc.api.network

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class ArgumentsImpl(val args: Seq[AnyRef]) extends network.Arguments {
  def iterator() = args.iterator

  def count() = args.length

  def checkAny(index: Int) = {
    checkIndex(index, "value")
    args(index) match {
      case Unit | None => null
      case arg => arg
    }
  }

  def checkBoolean(index: Int) = {
    checkIndex(index, "boolean")
    args(index) match {
      case value: java.lang.Boolean => value
      case value => throw typeError(index, value, "boolean")
    }
  }

  def checkDouble(index: Int) = {
    checkIndex(index, "number")
    args(index) match {
      case value: java.lang.Double => value
      case value => throw typeError(index, value, "number")
    }
  }

  def checkInteger(index: Int) = {
    checkIndex(index, "number")
    args(index) match {
      case value: java.lang.Double => value.intValue
      case value => throw typeError(index, value, "number")
    }
  }

  def checkString(index: Int) = {
    checkIndex(index, "string")
    args(index) match {
      case value: java.lang.String => value
      case value: Array[Byte] => new String(value, "UTF-8")
      case value => throw typeError(index, value, "string")
    }
  }

  def checkByteArray(index: Int) = {
    checkIndex(index, "string")
    args(index) match {
      case value: java.lang.String => value.getBytes("UTF-8")
      case value: Array[Byte] => value
      case value => throw typeError(index, value, "string")
    }
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

  def isBoolean(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.Boolean => true
      case _ => false
    })

  def isDouble(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.Double => true
      case _ => false
    })

  def isInteger(index: Int) =
    index >= 0 && index < count && (args(index) match {
      case value: java.lang.Integer => true
      case value: java.lang.Double => true
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

  private def checkIndex(index: Int, name: String) =
    if (index < 0) throw new IndexOutOfBoundsException()
    else if (args.length <= index) throw new IllegalArgumentException(
      "bad arguments #%d (%s expected, got no value)".
        format(index + 1, name))

  private def typeError(index: Int, have: AnyRef, want: String) =
    new IllegalArgumentException(
      "bad argument #%d (%s expected, got %s)".
        format(index + 1, want, typeName(have)))

  private def typeName(value: AnyRef): String = value match {
    case null | Unit | None => "nil"
    case _: java.lang.Boolean => "boolean"
    case _: java.lang.Double => "double"
    case _: java.lang.String => "string"
    case _: Array[Byte] => "string"
    case value: java.util.Map[_, _] => "table"
    case value: Map[_, _] => "table"
    case value: mutable.Map[_, _] => "table"
    case _ => value.getClass.getSimpleName
  }
}
