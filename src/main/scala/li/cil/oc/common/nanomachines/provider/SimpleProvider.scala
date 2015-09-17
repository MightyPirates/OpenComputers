package li.cil.oc.common.nanomachines.provider

import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.nanomachines.BehaviorProvider
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._

abstract class SimpleProvider extends BehaviorProvider {
  // One-time generated UUID to identify our behaviors.
  def Id: String

  def doCreateBehaviors(player: EntityPlayer): Iterable[Behavior]

  def doWriteToNBT(behavior: Behavior, nbt: NBTTagCompound): Unit = {}

  def doReadFromNBT(player: EntityPlayer, nbt: NBTTagCompound): Behavior

  override def createBehaviors(player: EntityPlayer): java.lang.Iterable[Behavior] = asJavaIterable(doCreateBehaviors(player))

  override def writeToNBT(behavior: Behavior): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    nbt.setString("provider", Id)
    doWriteToNBT(behavior: Behavior, nbt: NBTTagCompound)
    nbt
  }

  override def readFromNBT(player: EntityPlayer, nbt: NBTTagCompound): Behavior = {
    if (nbt.getString("provider") == Id) {
      doReadFromNBT(player, nbt)
    }
    else null
  }
}
