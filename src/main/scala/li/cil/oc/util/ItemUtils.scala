package li.cil.oc.util

import java.util.logging.Level

import com.google.common.base.Strings
import li.cil.oc.api.Persistable
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, OpenComputers, Settings, api, server}
import net.minecraft.item.{Item, ItemMap, ItemStack}
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.world.World

import scala.io.Source

object ItemUtils {
  def caseTier(stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    if (descriptor == api.Items.get("case1")) Tier.One
    else if (descriptor == api.Items.get("case2")) Tier.Two
    else if (descriptor == api.Items.get("case3")) Tier.Three
    else if (descriptor == api.Items.get("caseCreative")) Tier.Four
    else Tier.None
  }

  abstract class ItemData extends Persistable {
    def load(stack: ItemStack) {
      if (stack.hasTagCompound) {
        // Because ItemStack's load function doesn't copy the compound tag,
        // but keeps it as is, leading to oh so fun bugs!
        load(stack.getTagCompound.copy().asInstanceOf[NBTTagCompound])
      }
    }

    def save(stack: ItemStack) {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound("tag"))
      }
      save(stack.getTagCompound)
    }
  }

  class RobotData extends ItemData {
    def this(stack: ItemStack) = {
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

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey("display") && nbt.getCompoundTag("display").hasKey("Name")) {
        name = nbt.getCompoundTag("display").getString("Name")
      }
      if (Strings.isNullOrEmpty(name)) {
        name = RobotData.randomName
      }
      totalEnergy = nbt.getInteger(Settings.namespace + "storedEnergy")
      robotEnergy = nbt.getInteger(Settings.namespace + "robotEnergy")
      if (nbt.hasKey(Settings.namespace + "components")) {
        tier = nbt.getInteger(Settings.namespace + "tier")
        components = nbt.getTagList(Settings.namespace + "components").map(ItemStack.loadItemStackFromNBT).toArray
        containers = nbt.getTagList(Settings.namespace + "containers").map(ItemStack.loadItemStackFromNBT).toArray
      }
      else {
        // Old robot, upgrade to new modular model.
        tier = 0
        val experienceUpgrade = api.Items.get("experienceUpgrade").createItemStack(1)
        server.driver.item.UpgradeExperience.dataTag(experienceUpgrade).setDouble(Settings.namespace + "xp", nbt.getDouble(Settings.namespace + "xp"))
        components = Array(
          api.Items.get("screen1").createItemStack(1),
          api.Items.get("keyboard").createItemStack(1),
          api.Items.get("inventoryUpgrade").createItemStack(1),
          experienceUpgrade,
          api.Items.get("openOS").createItemStack(1),
          api.Items.get("graphicsCard1").createItemStack(1),
          api.Items.get("cpu1").createItemStack(1),
          api.Items.get("ram2").createItemStack(1)
        )
        containers = Array(
          api.Items.get("cardContainer2").createItemStack(1),
          api.Items.get("diskDrive").createItemStack(1),
          api.Items.get("upgradeContainer3").createItemStack(1)
        )
        robotEnergy = totalEnergy
      }
    }

    override def save(nbt: NBTTagCompound) {
      if (name != null) {
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
    }

    def createItemStack() = {
      val stack = Blocks.robotProxy.createItemStack()
      save(stack)
      stack
    }

    def copyItemStack() = {
      val stack = createItemStack()
      // Forget all node addresses and so on. This is used when 'picking' a
      // robot in creative mode.
      val newInfo = new RobotData(stack)
      newInfo.components.foreach(cs => Option(api.Driver.driverFor(cs)) match {
        case Some(driver) if driver == server.driver.item.Screen =>
          val nbt = driver.dataTag(cs)
          nbt.getTags.toArray.foreach {
            case tag: NBTBase => nbt.removeTag(tag.getName)
            case _ =>
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

  object RobotData {
    val names = try {
      Source.fromInputStream(getClass.getResourceAsStream(
        "/assets/" + Settings.resourceDomain + "/robot.names"))("UTF-8").
        getLines().map(_.trim).filter(!_.startsWith("#")).filter(_ != "").toArray
    }
    catch {
      case t: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Failed loading robot name list.", t)
        Array.empty[String]
    }

    def randomName = if (names.length > 0) names((math.random * names.length).toInt) else "Robot"
  }

  class NavigationUpgradeData extends ItemData {
    def this(stack: ItemStack) = {
      this()
      load(stack)
    }

    var map = new ItemStack(Item.map)

    def mapData(world: World) = try map.getItem.asInstanceOf[ItemMap].getMapData(map, world) catch {
      case _: Throwable => throw new Exception("invalid map")
    }

    override def load(stack: ItemStack) {
      if (stack.hasTagCompound) {
        load(stack.getTagCompound.getCompoundTag(Settings.namespace + "data"))
      }
    }

    override def save(stack: ItemStack) {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound("tag"))
      }
      save(stack.getCompoundTag(Settings.namespace + "data"))
    }

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey(Settings.namespace + "map")) {
        map = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map"))
      }
    }

    override def save(nbt: NBTTagCompound) {
      if (map != null) {
        nbt.setNewCompoundTag(Settings.namespace + "map", map.writeToNBT)
      }
    }
  }

}
