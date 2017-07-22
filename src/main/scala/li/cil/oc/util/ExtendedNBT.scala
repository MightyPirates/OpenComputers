package li.cil.oc.util

import com.google.common.base.Charsets
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object ExtendedNBT {

  implicit def toNbt(value: Boolean): NBTTagByte = new NBTTagByte(if (value) 1 else 0)

  implicit def toNbt(value: Byte): NBTTagByte = new NBTTagByte(value)

  implicit def toNbt(value: Short): NBTTagShort = new NBTTagShort(value)

  implicit def toNbt(value: Int): NBTTagInt = new NBTTagInt(value)

  implicit def toNbt(value: Long): NBTTagLong = new NBTTagLong(value)

  implicit def toNbt(value: Float): NBTTagFloat = new NBTTagFloat(value)

  implicit def toNbt(value: Double): NBTTagDouble = new NBTTagDouble(value)

  implicit def toNbt(value: Array[Byte]): NBTTagByteArray = new NBTTagByteArray(value)

  implicit def toNbt(value: Array[Int]): NBTTagIntArray = new NBTTagIntArray(value)

  implicit def toNbt(value: Array[Boolean]): NBTTagByteArray = new NBTTagByteArray(value.map(if (_) 1: Byte else 0: Byte))

  implicit def toNbt(value: String): NBTTagString = new NBTTagString(value)

  implicit def toNbt(value: ItemStack): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    if (value != null) {
      value.writeToNBT(nbt)
    }
    nbt
  }

  implicit def toNbt(value: NBTTagCompound => Unit): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    value(nbt)
    nbt
  }

  implicit def toNbt(value: Map[String, _]): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    for ((key, value) <- value) value match {
      case value: Boolean => nbt.setTag(key, value)
      case value: Byte => nbt.setTag(key, value)
      case value: Short => nbt.setTag(key, value)
      case value: Int => nbt.setTag(key, value)
      case value: Long => nbt.setTag(key, value)
      case value: Float => nbt.setTag(key, value)
      case value: Double => nbt.setTag(key, value)
      case value: Array[Byte] => nbt.setTag(key, value)
      case value: Array[Int] => nbt.setTag(key, value)
      case value: String => nbt.setTag(key, value)
      case value: ItemStack => nbt.setTag(key, value)
      case _ =>
    }
    nbt
  }

  def typedMapToNbt(map: Map[_, _]): NBTBase = {
    def mapToList(value: Array[(_, _)]) = value.collect {
      // Ignore, can be stuff like the 'n' introduced by Lua's `pack`.
      case (k: Number, v) => k -> v
    }.sortBy(_._1.intValue()).map(_._2)
    def asList(value: Option[Any]): IndexedSeq[_] = value match {
      case Some(v: Array[_]) => v
      case Some(v: Map[_, _]) => mapToList(v.toArray)
      case Some(v: mutable.Map[_, _]) => mapToList(v.toArray)
      case Some(v: java.util.Map[_, _]) => mapToList(mapAsScalaMap(v).toArray)
      case Some(v: String) => v.getBytes(Charsets.UTF_8)
      case _ => throw new IllegalArgumentException("Illegal or missing value.")
    }
    def asMap[K](value: Option[Any]): Map[K, _] = value match {
      case Some(v: Map[K, _]@unchecked) => v
      case Some(v: mutable.Map[K, _]@unchecked) => v.toMap
      case Some(v: java.util.Map[K, _]@unchecked) => mapAsScalaMap(v).toMap
      case _ => throw new IllegalArgumentException("Illegal value.")
    }
    val typeAndValue = asMap[String](Option(map))
    val nbtType = typeAndValue.get("type")
    val nbtValue = typeAndValue.get("value")
    nbtType match {
      case Some(n: Number) => n.intValue() match {
        case NBT.TAG_BYTE => new NBTTagByte(nbtValue match {
          case Some(v: Number) => v.byteValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_SHORT => new NBTTagShort(nbtValue match {
          case Some(v: Number) => v.shortValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_INT => new NBTTagInt(nbtValue match {
          case Some(v: Number) => v.intValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_LONG => new NBTTagLong(nbtValue match {
          case Some(v: Number) => v.longValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_FLOAT => new NBTTagFloat(nbtValue match {
          case Some(v: Number) => v.floatValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_DOUBLE => new NBTTagDouble(nbtValue match {
          case Some(v: Number) => v.doubleValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_BYTE_ARRAY => new NBTTagByteArray(asList(nbtValue).map {
          case n: Number => n.byteValue()
          case _ => throw new IllegalArgumentException("Illegal value.")
        }.toArray)

        case NBT.TAG_STRING => new NBTTagString(nbtValue match {
          case Some(v: String) => v
          case Some(v: Array[Byte]) => new String(v, Charsets.UTF_8)
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_LIST =>
          val list = new NBTTagList()
          asList(nbtValue).map(v => asMap(Option(v))).foreach(v => list.appendTag(typedMapToNbt(v)))
          list

        case NBT.TAG_COMPOUND =>
          val nbt = new NBTTagCompound()
          val values = asMap[String](nbtValue)
          for ((name, entry) <- values) {
            try nbt.setTag(name, typedMapToNbt(asMap[Any](Option(entry)))) catch {
              case t: Throwable => throw new IllegalArgumentException(s"Error converting entry '$name': ${t.getMessage}")
            }
          }
          nbt

        case NBT.TAG_INT_ARRAY =>
          new NBTTagIntArray(asList(nbtValue).map {
            case n: Number => n.intValue()
            case _ => throw new IllegalArgumentException()
          }.toArray)

        case _ => throw new IllegalArgumentException(s"Unsupported NBT type '$n'.")
      }
      case Some(t) => throw new IllegalArgumentException(s"Illegal NBT type '$t'.")
      case _ => throw new IllegalArgumentException(s"Missing NBT type.")
    }
  }

  implicit def booleanIterableToNbt(value: Iterable[Boolean]): Iterable[NBTTagByte] = value.map(toNbt)

  implicit def byteIterableToNbt(value: Iterable[Byte]): Iterable[NBTTagByte] = value.map(toNbt)

  implicit def shortIterableToNbt(value: Iterable[Short]): Iterable[NBTTagShort] = value.map(toNbt)

  implicit def intIterableToNbt(value: Iterable[Int]): Iterable[NBTTagInt] = value.map(toNbt)

  implicit def intArrayIterableToNbt(value: Iterable[Array[Int]]): Iterable[NBTTagIntArray] = value.map(toNbt)

  implicit def longIterableToNbt(value: Iterable[Long]): Iterable[NBTTagLong] = value.map(toNbt)

  implicit def floatIterableToNbt(value: Iterable[Float]): Iterable[NBTTagFloat] = value.map(toNbt)

  implicit def doubleIterableToNbt(value: Iterable[Double]): Iterable[NBTTagDouble] = value.map(toNbt)

  implicit def byteArrayIterableToNbt(value: Iterable[Array[Byte]]): Iterable[NBTTagByteArray] = value.map(toNbt)

  implicit def stringIterableToNbt(value: Iterable[String]): Iterable[NBTTagString] = value.map(toNbt)

  implicit def writableIterableToNbt(value: Iterable[NBTTagCompound => Unit]): Iterable[NBTTagCompound] = value.map(toNbt)

  implicit def itemStackIterableToNbt(value: Iterable[ItemStack]): Iterable[NBTTagCompound] = value.map(toNbt)

  implicit def extendNBTBase(nbt: NBTBase): ExtendedNBTBase = new ExtendedNBTBase(nbt)

  implicit def extendNBTTagCompound(nbt: NBTTagCompound): ExtendedNBTTagCompound = new ExtendedNBTTagCompound(nbt)

  implicit def extendNBTTagList(nbt: NBTTagList): ExtendedNBTTagList = new ExtendedNBTTagList(nbt)

  class ExtendedNBTBase(val nbt: NBTBase) {
    def toTypedMap: Map[String, _] = Map("type" -> nbt.getId, "value" -> (nbt match {
      case tag: NBTTagByte => tag.func_150290_f()
      case tag: NBTTagShort => tag.func_150289_e()
      case tag: NBTTagInt => tag.func_150287_d()
      case tag: NBTTagLong => tag.func_150291_c()
      case tag: NBTTagFloat => tag.func_150288_h()
      case tag: NBTTagDouble => tag.func_150286_g()
      case tag: NBTTagByteArray => tag.func_150292_c()
      case tag: NBTTagString => tag.func_150285_a_()
      case tag: NBTTagList => tag.map((entry: NBTBase) => entry.toTypedMap)
      case tag: NBTTagCompound => tag.func_150296_c().collect {
        case key: String => key -> tag.getTag(key).toTypedMap
      }.toMap
      case tag: NBTTagIntArray => tag.func_150302_c()
      case _ => throw new IllegalArgumentException()
    }))
  }

  class ExtendedNBTTagCompound(val nbt: NBTTagCompound) {
    def setNewCompoundTag(name: String, f: (NBTTagCompound) => Any) = {
      val t = new NBTTagCompound()
      f(t)
      nbt.setTag(name, t)
      nbt
    }

    def setNewTagList(name: String, values: Iterable[NBTBase]) = {
      val t = new NBTTagList()
      t.append(values)
      nbt.setTag(name, t)
      nbt
    }

    def setNewTagList(name: String, values: NBTBase*): NBTTagCompound = setNewTagList(name, values)

    def getDirection(name: String) = {
      nbt.getByte(name) match {
        case id if id < 0 => None
        case id =>
          val side = ForgeDirection.getOrientation(id)
          // Backwards compatibility.
          if (side == ForgeDirection.UNKNOWN) None
          else Option(side)
      }
    }

    def setDirection(name: String, d: Option[ForgeDirection]): Unit = {
      d match {
        case Some(side) => nbt.setByte(name, side.ordinal.toByte)
        case _ => nbt.setByte(name, -1: Byte)
      }
    }

    def getBooleanArray(name: String) = nbt.getByteArray(name).map(_ == 1)

    def setBooleanArray(name: String, value: Array[Boolean]) = nbt.setTag(name, toNbt(value))
  }

  class ExtendedNBTTagList(val nbt: NBTTagList) {
    def appendNewCompoundTag(f: (NBTTagCompound) => Unit) {
      val t = new NBTTagCompound()
      f(t)
      nbt.appendTag(t)
    }

    def append(values: Iterable[NBTBase]) {
      for (value <- values) {
        nbt.appendTag(value)
      }
    }

    def append(values: NBTBase*): Unit = append(values)

    def foreach[Tag <: NBTBase](f: Tag => Unit) {
      val iterable = nbt.copy.asInstanceOf[NBTTagList]
      while (iterable.tagCount > 0) {
        f(iterable.removeTag(0).asInstanceOf[Tag])
      }
    }

    def map[Tag <: NBTBase, Value](f: Tag => Value): IndexedSeq[Value] = {
      val iterable = nbt.copy.asInstanceOf[NBTTagList]
      val buffer = mutable.ArrayBuffer.empty[Value]
      while (iterable.tagCount > 0) {
        buffer += f(iterable.removeTag(0).asInstanceOf[Tag])
      }
      buffer
    }

    def toArray[Tag: ClassTag] = map((t: Tag) => t).toArray
  }

}
