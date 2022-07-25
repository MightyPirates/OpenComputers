package li.cil.oc.util

import com.google.common.base.Charsets
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.util.Direction
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object ExtendedNBT {

  implicit def toNbt(value: Boolean): ByteNBT = ByteNBT.valueOf(value)

  implicit def toNbt(value: Byte): ByteNBT = ByteNBT.valueOf(value)

  implicit def toNbt(value: Short): ShortNBT = ShortNBT.valueOf(value)

  implicit def toNbt(value: Int): IntNBT = IntNBT.valueOf(value)

  implicit def toNbt(value: Long): LongNBT = LongNBT.valueOf(value)

  implicit def toNbt(value: Float): FloatNBT = FloatNBT.valueOf(value)

  implicit def toNbt(value: Double): DoubleNBT = DoubleNBT.valueOf(value)

  implicit def toNbt(value: Array[Byte]): ByteArrayNBT = new ByteArrayNBT(value)

  implicit def toNbt(value: Array[Int]): IntArrayNBT = new IntArrayNBT(value)

  implicit def toNbt(value: Array[Boolean]): ByteArrayNBT = new ByteArrayNBT(value.map(if (_) 1: Byte else 0: Byte))

  implicit def toNbt(value: String): StringNBT = StringNBT.valueOf(value)

  implicit def toNbt(value: ItemStack): CompoundNBT = {
    val nbt = new CompoundNBT()
    if (value != null) {
      value.save(nbt)
    }
    nbt
  }

  implicit def toNbt(value: CompoundNBT => Unit): CompoundNBT = {
    val nbt = new CompoundNBT()
    value(nbt)
    nbt
  }

  implicit def toNbt(value: Map[String, _]): CompoundNBT = {
    val nbt = new CompoundNBT()
    for ((key, value) <- value) value match {
      case value: Boolean => nbt.put(key, value)
      case value: Byte => nbt.put(key, value)
      case value: Short => nbt.put(key, value)
      case value: Int => nbt.put(key, value)
      case value: Long => nbt.put(key, value)
      case value: Float => nbt.put(key, value)
      case value: Double => nbt.put(key, value)
      case value: Array[Byte] => nbt.put(key, value)
      case value: Array[Int] => nbt.put(key, value)
      case value: String => nbt.put(key, value)
      case value: ItemStack => nbt.put(key, value)
      case _ =>
    }
    nbt
  }

  def typedMapToNbt(map: Map[_, _]): INBT = {
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
        case NBT.TAG_BYTE => ByteNBT.valueOf(nbtValue match {
          case Some(v: Number) => v.byteValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_SHORT => ShortNBT.valueOf(nbtValue match {
          case Some(v: Number) => v.shortValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_INT => IntNBT.valueOf(nbtValue match {
          case Some(v: Number) => v.intValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_LONG => LongNBT.valueOf(nbtValue match {
          case Some(v: Number) => v.longValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_FLOAT => FloatNBT.valueOf(nbtValue match {
          case Some(v: Number) => v.floatValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_DOUBLE => DoubleNBT.valueOf(nbtValue match {
          case Some(v: Number) => v.doubleValue()
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_BYTE_ARRAY => new ByteArrayNBT(asList(nbtValue).map {
          case n: Number => n.byteValue()
          case _ => throw new IllegalArgumentException("Illegal value.")
        }.toArray)

        case NBT.TAG_STRING => StringNBT.valueOf(nbtValue match {
          case Some(v: String) => v
          case Some(v: Array[Byte]) => new String(v, Charsets.UTF_8)
          case _ => throw new IllegalArgumentException("Illegal or missing value.")
        })

        case NBT.TAG_LIST =>
          val list = new ListNBT()
          asList(nbtValue).map(v => asMap(Option(v))).foreach(v => list.add(typedMapToNbt(v)))
          list

        case NBT.TAG_COMPOUND =>
          val nbt = new CompoundNBT()
          val values = asMap[String](nbtValue)
          for ((name, entry) <- values) {
            try nbt.put(name, typedMapToNbt(asMap[Any](Option(entry)))) catch {
              case t: Throwable => throw new IllegalArgumentException(s"Error converting entry '$name': ${t.getMessage}")
            }
          }
          nbt

        case NBT.TAG_INT_ARRAY =>
          new IntArrayNBT(asList(nbtValue).map {
            case n: Number => n.intValue()
            case _ => throw new IllegalArgumentException()
          }.toArray)

        case _ => throw new IllegalArgumentException(s"Unsupported NBT type '$n'.")
      }
      case Some(t) => throw new IllegalArgumentException(s"Illegal NBT type '$t'.")
      case _ => throw new IllegalArgumentException(s"Missing NBT type.")
    }
  }

  implicit def booleanIterableToNbt(value: Iterable[Boolean]): Iterable[ByteNBT] = value.map(toNbt)

  implicit def byteIterableToNbt(value: Iterable[Byte]): Iterable[ByteNBT] = value.map(toNbt)

  implicit def shortIterableToNbt(value: Iterable[Short]): Iterable[ShortNBT] = value.map(toNbt)

  implicit def intIterableToNbt(value: Iterable[Int]): Iterable[IntNBT] = value.map(toNbt)

  implicit def intArrayIterableToNbt(value: Iterable[Array[Int]]): Iterable[IntArrayNBT] = value.map(toNbt)

  implicit def longIterableToNbt(value: Iterable[Long]): Iterable[LongNBT] = value.map(toNbt)

  implicit def floatIterableToNbt(value: Iterable[Float]): Iterable[FloatNBT] = value.map(toNbt)

  implicit def doubleIterableToNbt(value: Iterable[Double]): Iterable[DoubleNBT] = value.map(toNbt)

  implicit def byteArrayIterableToNbt(value: Iterable[Array[Byte]]): Iterable[ByteArrayNBT] = value.map(toNbt)

  implicit def stringIterableToNbt(value: Iterable[String]): Iterable[StringNBT] = value.map(toNbt)

  implicit def writableIterableToNbt(value: Iterable[CompoundNBT => Unit]): Iterable[CompoundNBT] = value.map(toNbt)

  implicit def itemStackIterableToNbt(value: Iterable[ItemStack]): Iterable[CompoundNBT] = value.map(toNbt)

  implicit def extendINBT(nbt: INBT): ExtendedINBT = new ExtendedINBT(nbt)

  implicit def extendCompoundNBT(nbt: CompoundNBT): ExtendedCompoundNBT = new ExtendedCompoundNBT(nbt)

  implicit def extendListNBT(nbt: ListNBT): ExtendedListNBT = new ExtendedListNBT(nbt)

  class ExtendedINBT(val nbt: INBT) {
    def toTypedMap: Map[String, _] = Map("type" -> nbt.getId, "value" -> (nbt match {
      case tag: ByteNBT => tag.getAsByte
      case tag: ShortNBT => tag.getAsShort
      case tag: IntNBT => tag.getAsInt
      case tag: LongNBT => tag.getAsLong
      case tag: FloatNBT => tag.getAsFloat
      case tag: DoubleNBT => tag.getAsDouble
      case tag: ByteArrayNBT => tag.getAsByteArray
      case tag: StringNBT => tag.getAsString
      case tag: ListNBT => tag.map((entry: INBT) => entry.toTypedMap)
      case tag: CompoundNBT => tag.getAllKeys.collect {
        case key: String => key -> tag.get(key).toTypedMap
      }.toMap
      case tag: IntArrayNBT => tag.getAsIntArray
      case _ => throw new IllegalArgumentException()
    }))
  }

  class ExtendedCompoundNBT(val nbt: CompoundNBT) {
    def setNewCompoundTag(name: String, f: (CompoundNBT) => Any) = {
      val t = new CompoundNBT()
      f(t)
      nbt.put(name, t)
      nbt
    }

    def setNewTagList(name: String, values: Iterable[INBT]) = {
      val t = new ListNBT()
      t.append(values)
      nbt.put(name, t)
      nbt
    }

    def setNewTagList(name: String, values: INBT*): CompoundNBT = setNewTagList(name, values)

    def getDirection(name: String) = {
      nbt.getByte(name) match {
        case id if id < 0 || id > Direction.values.length => None
        case id => Option(Direction.from3DDataValue(id))
      }
    }

    def setDirection(name: String, d: Option[Direction]): Unit = {
      d match {
        case Some(side) => nbt.putByte(name, side.ordinal.toByte)
        case _ => nbt.putByte(name, -1: Byte)
      }
    }

    def getBooleanArray(name: String) = nbt.getByteArray(name).map(_ == 1)

    def setBooleanArray(name: String, value: Array[Boolean]) = nbt.put(name, toNbt(value))
  }

  class ExtendedListNBT(val nbt: ListNBT) {
    def appendNewCompoundTag(f: (CompoundNBT) => Unit) {
      val t = new CompoundNBT()
      f(t)
      nbt.add(t)
    }

    def append(values: Iterable[INBT]) {
      for (value <- values) {
        nbt.add(value)
      }
    }

    def append(values: INBT*): Unit = append(values)

    def foreach[Tag <: INBT](f: Tag => Unit) {
      val iterable = nbt.copy(): ListNBT
      while (iterable.size > 0) {
        f((iterable.remove(0): INBT).asInstanceOf[Tag])
      }
    }

    def map[Tag <: INBT, Value](f: Tag => Value): IndexedSeq[Value] = {
      val iterable = nbt.copy(): ListNBT
      val buffer = mutable.ArrayBuffer.empty[Value]
      while (iterable.size > 0) {
        buffer += f((iterable.remove(0): INBT).asInstanceOf[Tag])
      }
      buffer
    }

    def toTagArray[Tag: ClassTag] = map((t: Tag) => t).toArray
  }

}
