package li.cil.oc.common.item.data

import com.google.common.base.Charsets
import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
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
    loadData(stack)
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

  private final val StoredEnergyTag = Settings.namespace + "storedEnergy"
  private final val RobotEnergyTag = Settings.namespace + "robotEnergy"
  private final val TierTag = Settings.namespace + "tier"
  private final val ComponentsTag = Settings.namespace + "components"
  private final val ContainersTag = Settings.namespace + "containers"
  private final val LightColorTag = Settings.namespace + "lightColor"

  override def loadData(nbt: CompoundNBT) {
    name = ItemUtils.getDisplayName(nbt).getOrElse("")
    if (Strings.isNullOrEmpty(name)) {
      name = RobotData.randomName
    }
    totalEnergy = nbt.getInt(StoredEnergyTag)
    robotEnergy = nbt.getInt(RobotEnergyTag)
    tier = nbt.getInt(TierTag)
    components = nbt.getList(ComponentsTag, NBT.TAG_COMPOUND).
      toTagArray[CompoundNBT].map(ItemStack.of(_))
    containers = nbt.getList(ContainersTag, NBT.TAG_COMPOUND).
      toTagArray[CompoundNBT].map(ItemStack.of(_))
    if (nbt.contains(LightColorTag)) {
      lightColor = nbt.getInt(LightColorTag)
    }
  }

  override def saveData(nbt: CompoundNBT) {
    if (!Strings.isNullOrEmpty(name)) {
      ItemUtils.setDisplayName(nbt, name)
    }
    nbt.putInt(StoredEnergyTag, totalEnergy)
    nbt.putInt(RobotEnergyTag, robotEnergy)
    nbt.putInt(TierTag, tier)
    nbt.setNewTagList(ComponentsTag, components.toIterable)
    nbt.setNewTagList(ContainersTag, containers.toIterable)
    nbt.putInt(LightColorTag, lightColor)
  }

  def copyItemStack() = {
    val stack = createItemStack()
    // Forget all node addresses and so on. This is used when 'picking' a
    // robot in creative mode.
    val newInfo = new RobotData(stack)
    newInfo.components.foreach(cs => Option(api.Driver.driverFor(cs)) match {
      case Some(driver) if driver == DriverScreen =>
        val nbt = driver.dataTag(cs)
        for (tagName <- nbt.getAllKeys.toArray) {
          nbt.remove(tagName.asInstanceOf[String])
        }
      case _ =>
    })
    // Don't show energy info (because it's unreliable) but fill up the
    // internal buffer. This is for creative use only, anyway.
    newInfo.totalEnergy = 0
    newInfo.robotEnergy = 50000
    newInfo.saveData(stack)
    stack
  }
}
