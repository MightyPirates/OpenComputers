package li.cil.oc.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt._

import scala.language.{implicitConversions, reflectiveCalls}

object ExtendedNBT {

  implicit def toNbt(value: Byte): NBTTagByte = new NBTTagByte(value)

  implicit def toNbt(value: Short): NBTTagShort = new NBTTagShort(value)

  implicit def toNbt(value: Int): NBTTagInt = new NBTTagInt(value)

  implicit def toNbt(value: Array[Int]): NBTTagIntArray = new NBTTagIntArray(value)

  implicit def toNbt(value: Long): NBTTagLong = new NBTTagLong(value)

  implicit def toNbt(value: Float): NBTTagFloat = new NBTTagFloat(value)

  implicit def toNbt(value: Double): NBTTagDouble = new NBTTagDouble(value)

  implicit def toNbt(value: Array[Byte]): NBTTagByteArray = new NBTTagByteArray(value)

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

    def foreach(f: (NBTTagList, Int) => Unit): Unit = (0 until nbt.tagCount).map(f(nbt, _))

    def map[Value](f: (NBTTagList, Int) => Value) = (0 until nbt.tagCount).map(f(nbt, _))
  }

}
