package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import net.minecraft.nbt.NBTTagCompound

trait TextBuffer extends Environment {
  lazy val buffer = {
    val screenItem = api.Items.get("screen1").createItemStack(1)
    val buffer = api.Driver.driverFor(screenItem, getClass).createEnvironment(screenItem, this).asInstanceOf[api.component.TextBuffer]
    val (maxWidth, maxHeight) = Settings.screenResolutionsByTier(tier)
    buffer.setMaximumResolution(maxWidth, maxHeight)
    buffer.setMaximumColorDepth(Settings.screenDepthsByTier(tier))
    buffer
  }

  override def node = buffer.node

  def tier: Int

  override def updateEntity() {
    super.updateEntity()
    if (isClient || isConnected) {
      buffer.update()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    buffer.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    buffer.save(nbt)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    buffer.load(nbt)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    buffer.save(nbt)
  }
}
