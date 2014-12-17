package li.cil.oc.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object ExtendedNBT {

  implicit def toNbt(value: Boolean): NBTTagByte = new NBTTagByte(if (value) 1 else 0)

  implicit def toNbt(value: Byte): NBTTagByte = new NBTTagByte(value)

  implicit def toNbt(value: Short): NBTTagShort = new NBTTagShort(value)

  implicit def toNbt(value: Int): NBTTagInt = new NBTTagInt(value)

  implicit def toNbt(value: Array[Int]): NBTTagIntArray = new NBTTagIntArray(value)

  implicit def toNbt(value: Long): NBTTagLong = new NBTTagLong(value)

  implicit def toNbt(value: Float): NBTTagFloat = new NBTTagFloat(value)

  implicit def toNbt(value: Double): NBTTagDouble = new NBTTagDouble(value)

  implicit def toNbt(value: Array[Byte]): NBTTagByteArray = new NBTTagByteArray(value)

  implicit def toNbt(value: Array[Boolean]): NBTTagByteArray = new NBTTagByteArray(value.map(if (_) 1: Byte else 0: Byte))

  implicit def toNbt(value: String): NBTTagString = new NBTTagString(value)

  implicit def toNbt(value: {def writeToNBT(nbt: NBTTagCompound): Unit}): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    value.writeToNBT(nbt)
    nbt
  }

  implicit def toNbt(value: ItemStack): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    if (value != null) {
      value.writeToNBT(nbt)
    }
    nbt
  }

  implicit def toNbt(value: Map[String, _]): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    for ((key, value) <- value) value match {
      case value: Boolean => nbt.setTag(key, value)
      case value: Byte => nbt.setTag(key, value)
      case value: Short => nbt.setTag(key, value)
      case value: Int => nbt.setTag(key, value)
      case value: Array[Int] => nbt.setTag(key, value)
      case value: Long => nbt.setTag(key, value)
      case value: Float => nbt.setTag(key, value)
      case value: Double => nbt.setTag(key, value)
      case value: Array[Byte] => nbt.setTag(key, value)
      case value: String => nbt.setTag(key, value)
      case value: ItemStack => nbt.setTag(key, value)
      case _ =>
    }
    nbt
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

  implicit def writableIterableToNbt(value: Iterable[ {def writeToNBT(nbt: NBTTagCompound): Unit}]): Iterable[NBTTagCompound] = value.map(toNbt)

  implicit def itemStackIterableToNbt(value: Iterable[ItemStack]): Iterable[NBTTagCompound] = value.map(toNbt)

  implicit def extendNBTTagCompound(nbt: NBTTagCompound): ExtendedNBTTagCompound = new ExtendedNBTTagCompound(nbt)

  implicit def extendNBTTagList(nbt: NBTTagList): ExtendedNBTTagList = new ExtendedNBTTagList(nbt)

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
      val buffer = ArrayBuffer.empty[Value]
      while (iterable.tagCount > 0) {
        buffer += f(iterable.removeTag(0).asInstanceOf[Tag])
      }
      buffer
    }

    def toArray[Tag: ClassTag] = map((t: Tag) => t).toArray
  }

}
