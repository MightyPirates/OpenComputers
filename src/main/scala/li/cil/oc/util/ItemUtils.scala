package li.cil.oc.util

import net.minecraft.item.ItemStack
import li.cil.oc.{OpenComputers, Blocks, Settings, api}
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import com.google.common.base.Strings
import scala.io.Source
import java.util.logging.Level
import li.cil.oc.api.Persistable

object ItemUtils {
  def caseTier(stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    if (descriptor == api.Items.get("case1")) Tier.One
    else if (descriptor == api.Items.get("case2")) Tier.Two
    else if (descriptor == api.Items.get("case3")) Tier.Three
    else Tier.None
  }

  abstract class ItemData extends Persistable {
    def load(stack: ItemStack) {
      if (stack.hasTagCompound) {
        load(stack.getTagCompound)
      }
    }

    def save(stack: ItemStack) {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound("tag"))
      }
      save(stack.getTagCompound)
    }

    def createItemStack() = {
      val stack = Blocks.robotProxy.createItemStack()
      save(stack)
      stack
    }
  }

  class RobotData extends ItemData {
    def this(stack: ItemStack) = {
      this()
      load(stack)
    }

    var name = ""

    var energy = 0

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
      energy = nbt.getInteger(Settings.namespace + "storedEnergy")
      if (nbt.hasKey(Settings.namespace + "components")) {
        tier = nbt.getInteger(Settings.namespace + "tier")
        components = nbt.getTagList(Settings.namespace + "components").map(ItemStack.loadItemStackFromNBT).toArray
        containers = nbt.getTagList(Settings.namespace + "containers").map(ItemStack.loadItemStackFromNBT).toArray
      }
      else {
        // Old robot, upgrade to new modular model.
        tier = 0
        components = Array(
          api.Items.get("screen1").createItemStack(1),
          api.Items.get("keyboard").createItemStack(1),
          api.Items.get("inventoryUpgrade").createItemStack(1),
          api.Items.get("experienceUpgrade").createItemStack(1),
          api.Items.get("graphicsCard1").createItemStack(1),
          api.Items.get("cpu1").createItemStack(1),
          api.Items.get("ram2").createItemStack(1)
        )
        containers = Array(
          api.Items.get("cardContainer2").createItemStack(1),
          api.Items.get("upgradeContainer3").createItemStack(1),
          api.Items.get("diskDrive").createItemStack(1)
        )
        // TODO migration: xp to xp upgrade
        //        experience = nbt.getDouble(Settings.namespace + "xp")
      }
    }

    override def save(nbt: NBTTagCompound) {
      if (name != null) {
        if (!nbt.hasKey("display")) {
          nbt.setTag("display", new NBTTagCompound())
        }
        nbt.getCompoundTag("display").setString("Name", name)
      }
      nbt.setInteger(Settings.namespace + "storedEnergy", energy)
      nbt.setInteger(Settings.namespace + "tier", tier)
      nbt.setNewTagList(Settings.namespace + "components", components.toIterable)
      nbt.setNewTagList(Settings.namespace + "containers", containers.toIterable)
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

    def randomName = names((math.random * names.length).toInt)
  }
}
