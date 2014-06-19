package li.cil.oc.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt._

import scala.language.implicitConversions

object ExtendedNBT {

  implicit def toNbt(value: Byte) = new NBTTagByte(value)

  implicit def toNbt(value: Short) = new NBTTagShort(value)

  implicit def toNbt(value: Int) = new NBTTagInt(value)

  implicit def toNbt(value: Array[Int]) = new NBTTagIntArray(value)

  implicit def toNbt(value: Long) = new NBTTagLong(value)

  implicit def toNbt(value: Float) = new NBTTagFloat(value)

  implicit def toNbt(value: Double) = new NBTTagDouble(value)

  implicit def toNbt(value: Array[Byte]) = new NBTTagByteArray(value)

  implicit def toNbt(value: String) = new NBTTagString(value)

  implicit def toNbt(value: ItemStack) = {
    val nbt = new NBTTagCompound()
    if (value != null) {
      value.writeToNBT(nbt)
    }
    nbt
  }

  implicit def byteIterableToNbt(value: Iterable[Byte]) = value.map(toNbt)

  implicit def shortIterableToNbt(value: Iterable[Short]) = value.map(toNbt)

  implicit def intIterableToNbt(value: Iterable[Int]) = value.map(toNbt)

  implicit def intArrayIterableToNbt(value: Iterable[Array[Int]]) = value.map(toNbt)

  implicit def longIterableToNbt(value: Iterable[Long]) = value.map(toNbt)

  implicit def floatIterableToNbt(value: Iterable[Float]) = value.map(toNbt)

  implicit def doubleIterableToNbt(value: Iterable[Double]) = value.map(toNbt)

  implicit def byteArrayIterableToNbt(value: Iterable[Array[Byte]]) = value.map(toNbt)

  implicit def stringIterableToNbt(value: Iterable[String]) = value.map(toNbt)

  implicit def itemStackIterableToNbt(value: Iterable[ItemStack]) = value.map(toNbt)

  implicit def extendNBTTagCompound(nbt: NBTTagCompound) = new ExtendedNBTTagCompound(nbt)

  implicit def extendNBTTagList(nbt: NBTTagList) = new ExtendedNBTTagList(nbt)

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
