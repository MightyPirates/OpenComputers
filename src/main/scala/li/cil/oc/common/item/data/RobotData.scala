package li.cil.oc.common.item.data

import com.google.common.base.Charsets
import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

import scala.io.Source

object RobotData {
  val names = try {
    Source.fromInputStream(getClass.getResourceAsStream(
      "/assets/" + Settings.resourceDomain + "/robot.names"))(Charsets.UTF_8).
      getLines().map(_.takeWhile(_ != '#').trim()).filter(_ != "").toArray
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.warn("Failed loading robot name list.", t)
      Array.empty[String]
  }

  def randomName = if (names.length > 0) names((math.random * names.length).toInt) else "Robot"
}

class RobotData extends ItemData(Constants.BlockName.Robot) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var name = ""

  // Overall energy including components.
  var totalEnergy = 0

  // Energy purely stored in robot component - this is what we have to restore manually.
  var robotEnergy = 0

  var tier = 0

  var components = Array.empty[ItemStack]

  var containers = Array.empty[ItemStack]

  var lightColor = 0xF23030

  override def load(nbt: NBTTagCompound) {
    if (nbt.hasKey("display") && nbt.getCompoundTag("display").hasKey("Name")) {
      name = nbt.getCompoundTag("display").getString("Name")
    }
    if (Strings.isNullOrEmpty(name)) {
      name = RobotData.randomName
    }
    totalEnergy = nbt.getInteger(Settings.namespace + "storedEnergy")
    robotEnergy = nbt.getInteger(Settings.namespace + "robotEnergy")
    tier = nbt.getInteger(Settings.namespace + "tier")
    components = nbt.getTagList(Settings.namespace + "components", NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(ItemStack.loadItemStackFromNBT)
    containers = nbt.getTagList(Settings.namespace + "containers", NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(ItemStack.loadItemStackFromNBT)
    if (nbt.hasKey(Settings.namespace + "lightColor")) {
      lightColor = nbt.getInteger(Settings.namespace + "lightColor")
    }
  }

  override def save(nbt: NBTTagCompound) {
    if (!Strings.isNullOrEmpty(name)) {
      if (!nbt.hasKey("display")) {
        nbt.setTag("display", new NBTTagCompound())
      }
      nbt.getCompoundTag("display").setString("Name", name)
    }
    nbt.setInteger(Settings.namespace + "storedEnergy", totalEnergy)
    nbt.setInteger(Settings.namespace + "robotEnergy", robotEnergy)
    nbt.setInteger(Settings.namespace + "tier", tier)
    nbt.setNewTagList(Settings.namespace + "components", components.toIterable)
    nbt.setNewTagList(Settings.namespace + "containers", containers.toIterable)
    nbt.setInteger(Settings.namespace + "lightColor", lightColor)
  }

  def copyItemStack() = {
    val stack = createItemStack()
    // Forget all node addresses and so on. This is used when 'picking' a
    // robot in creative mode.
    val newInfo = new RobotData(stack)
    newInfo.components.foreach(cs => Option(api.Driver.driverFor(cs)) match {
      case Some(driver) if driver == DriverScreen =>
        val nbt = driver.dataTag(cs)
        for (tagName <- nbt.func_150296_c().toArray) {
          nbt.removeTag(tagName.asInstanceOf[String])
        }
      case _ =>
    })
    // Don't show energy info (because it's unreliable) but fill up the
    // internal buffer. This is for creative use only, anyway.
    newInfo.totalEnergy = 0
    newInfo.robotEnergy = 50000
    newInfo.save(stack)
    stack
  }
}
