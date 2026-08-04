package li.cil.oc.server.component

import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.api.{Network, internal, prefab}
import li.cil.oc.common.component.UpgradeSkinComponent
import li.cil.oc.common.tileentity
import li.cil.oc.server.component.traits.{InventoryAware, WorldAware}
import li.cil.oc.util.BlockPosition
import li.cil.oc.{Constants, Settings, api}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import java.util
import scala.collection.convert.WrapAsJava._

object UpgradeSkin {

  trait Common extends DeviceInfo {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "Skin Upgrade",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "Skinner V1"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }

  // Drones are fancy enough on their own. At least for now.
  /*class Drone(val host: EnvironmentHost with internal.Agent) extends prefab.ManagedEnvironment with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("skin", Visibility.Neighbors).
      create()

    // ----------------------------------------------------------------------- //
  }*/

  class Robot(val host: EnvironmentHost with tileentity.Robot, tier: Int) extends prefab.ManagedEnvironment with InventoryAware with WorldAware with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("skin", Visibility.Neighbors).
      withConnector().
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override def inventory = host.mainInventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.setSelectedSlot(value)

    private var skins = Array(new UpgradeSkinComponent("body"), new UpgradeSkinComponent("leftJoint1"), new UpgradeSkinComponent("leftJoint2"),
      new UpgradeSkinComponent("leftJoint3"), new UpgradeSkinComponent("rightJoint1"), new UpgradeSkinComponent("rightJoint2"), new UpgradeSkinComponent("rightJoint3"))
    private var hideBody = true
    private var hideNameTag = false
    private var hideUpgrades = false

    def swapSkin(skinComponent: UpgradeSkinComponent, onlyDeposit: Boolean): Boolean = {
      var skin = skinComponent.skin
      val stack = inventory.getStackInSlot(selectedSlot)
      var extracted = None : Option[ItemStack]
      var changed = false
      var maxStackSize = inventory.getInventoryStackLimit

      if(stack != null) {
        if(!onlyDeposit) {
          extracted = Some(stack.splitStack(1))
        }
        maxStackSize = math.min(maxStackSize, stack.getMaxStackSize)
      }

      val shouldMerge = stack != null && stack.stackSize < maxStackSize && skin.isDefined &&
        stack.isItemEqual(skin.get) && ItemStack.areItemStackTagsEqual(stack, skin.get)
      if(skin.isDefined && (stack == null || stack.stackSize == 0 || shouldMerge)) {
        if(stack == null) {
          inventory.setInventorySlotContents(selectedSlot, skin.get)
        } else if(stack.stackSize == 0) {
          inventory.setInventorySlotContents(selectedSlot, skin.get)
          stack.stackSize += 1
        } else {
          stack.stackSize += 1
        }
        changed = true
        skin = None
      }

      if(!onlyDeposit) {
        if (stack != null && skin.isEmpty && api.Items.get(extracted.get) == api.Items.get(Constants.BlockName.Print)) {
          skin = extracted
          changed = true
        } else if (stack != null) {
          stack.stackSize += 1
        }

        if (stack != null && stack.stackSize == 0) {
          inventory.setInventorySlotContents(selectedSlot, null)
        }
      }
      skinComponent.skin = skin
      inventory.markDirty()
      host.markDirty()
      updateClient()
      changed
    }

    def setSkinRotation(skinComponent: UpgradeSkinComponent, args: Arguments): Array[AnyRef] = {
      var rotX = 0f
      var rotY = 0f
      var rotZ = 0f
      if(args.isInteger(1)) {
        rotX = args.checkInteger(1)
      } else if(args.isDouble(1)) {
        rotX = args.checkDouble(1).toFloat
      } else {
        return result(Unit, "Number expected: rotX")
      }
      if(args.isInteger(2)) {
        rotY = args.checkInteger(2)
      } else if(args.isDouble(2)) {
        rotY = args.checkDouble(2).toFloat
      } else {
        return result(Unit, "Number expected: rotY")
      }
      if(args.isInteger(3)) {
        rotZ = args.checkInteger(3)
      } else if(args.isDouble(3)) {
        rotZ = args.checkDouble(3).toFloat
      } else {
        return result(Unit, "Number expected: rotZ")
      }

      if(skinComponent.animationTicksLeft == 0) {
        val maxRot = math.max(math.max(math.abs(rotX - skinComponent.oldRotX), math.abs(rotY - skinComponent.oldRotY)), math.abs(rotZ - skinComponent.oldRotZ))
        val moveTicks = math.max((Settings.get.turnDelay * 20).toInt, 1) // normalize the duration the rotation takes with the distance the rotation takes (90deg = turnDelay * 20)
        skinComponent.prepareRotationAnimation(rotX, rotY, rotZ, moveTicks)
        host.markDirty()
        updateClient()
        result(true)
      } else {
        result(Unit, "Skin is already rotating!")
      }
    }

    def hideSomething(apply: Boolean => Unit, args: Arguments): Array[AnyRef] = {
      if(args.isBoolean(0)) {
        apply(args.checkBoolean(0))
      } else {
        apply(true)
      }
      host.markDirty()
      updateClient()
      result(true)
    }

    private def updateClient(): Unit = host match {
      case robot: internal.Robot => robot.synchronizeSlot(robot.componentSlot(node.address))
      case _ =>
    }

    def checkIfTierIsHighEnoughForPart(part: Int): Boolean = {
      ((part == 2 || part == 5) && tier >= 1) || ((part == 3 || part == 6) && tier >= 2) || (part == 0 || part == 1 || part == 4)
    }

    @Callback(doc = """function(part:number):boolean -- Swaps the skin of the specified part with the content of the currently selected inventory slot.""")
    def swapSkin(context: Context, args: Arguments): Array[AnyRef] = {
      val part = args.checkInteger(0)
      if(checkIfTierIsHighEnoughForPart(part)) {
        result(swapSkin(skins(part), false))
      } else {
        result(Unit, part + " is not a valid part index")
      }
    }

    @Callback(doc = """function(part:number):string -- Returns the name of the skin of the specified part or nil of no skin is present for that part.""")
    def getSkin(context: Context, args: Arguments): Array[AnyRef] = {
      val part = args.checkInteger(0)
      if(checkIfTierIsHighEnoughForPart(part)) {
        result(if (skins(part).skin.isDefined) skins(part).skin.get.getDisplayName else null)
      } else {
        result(Unit, part + " is not a valid part index")
      }
    }

    @Callback(doc = """function(part:number):boolean -- Removes the skin from the specified part.""")
    def removeSkin(context: Context, args: Arguments): Array[AnyRef] = {
      val part = args.checkInteger(0)
      if(checkIfTierIsHighEnoughForPart(part)) {
        result(swapSkin(skins(part), true))
      } else {
        result(Unit, part + " is not a valid part index")
      }
    }

    @Callback(doc = """function(part:number, rotX:number, rotY:number, rotZ:number[, blocking:boolean = true]):boolean -- Sets the rotation for the skin of the specified part.""")
    def setSkinRotation(context: Context, args: Arguments): Array[AnyRef] = {
      val part = args.checkInteger(0)
      if(checkIfTierIsHighEnoughForPart(part)) {
        if(skins(part).animationTicksLeft == 0) {
          if (!node.tryChangeBuffer(-Settings.get.robotMoveCost)) {
            result(Unit, "not enough energy")
          } else {
            val res = setSkinRotation(skins(part), args)
            if (!args.isBoolean(4) || args.checkBoolean(4)) {
              context.pause(Settings.get.moveDelay)
            }
            res
          }
        } else {
          result(false)
        }
      } else {
        result(Unit, part + " is not a valid part index")
      }
    }

    @Callback(doc = """function(part:number):number, number, number -- Returns the rotation of the skin of the specified part.""")
    def getSkinRotation(context: Context, args: Arguments): Array[AnyRef] = {
      val part = args.checkInteger(0)
      if(checkIfTierIsHighEnoughForPart(part)) {
        result(skins(part).rotX, skins(part).rotY, skins(part).rotZ)
      } else {
        result(Unit, part + " is not a valid part index")
      }
    }

    @Callback(doc = """function([hide:boolean=true]):boolean -- Sets if the body of the robot should be hidden when a body skin is applied.""")
    def hideBody(context: Context, args: Arguments): Array[AnyRef] = {
      hideSomething(b => hideBody = b, args)
    }

    @Callback(doc = """function():boolean -- Returns if the body of the robot should be hidden when a body skin is applied.""")
    def isBodyHidden(context: Context, args: Arguments): Array[AnyRef] = {
      result(hideBody)
    }

    @Callback(doc = """function([hide:boolean=true]):boolean -- Sets if the nametag of the robot should be hidden.""")
    def hideNameTag(context: Context, args: Arguments): Array[AnyRef] = {
      hideSomething(b => hideNameTag = b, args)
    }

    @Callback(doc = """function():boolean -- Returns if the nametag of the robot should be hidden.""")
    def isNameTagHidden(context: Context, args: Arguments): Array[AnyRef] = {
      result(hideNameTag)
    }

    @Callback(doc = """function([hide:boolean=true]):boolean -- Sets if the upgrades of the robot should be hidden.""")
    def hideUpgrades(context: Context, args: Arguments): Array[AnyRef] = {
      hideSomething(b => hideUpgrades = b, args)
    }

    @Callback(doc = """function():boolean -- Returns if the upgrades of the robot should be hidden.""")
    def isUpgradeHidden(context: Context, args: Arguments): Array[AnyRef] = {
      result(hideUpgrades)
    }

    override val canUpdate = true

    override def update(): Unit = {
      super.update()
      var shouldUpdateHost = false
      for ( skin <- skins ) {
        if(skin.animationTicksLeft > 0) {
          skin.animationTicksLeft -= 1
          if(skin.animationTicksLeft == 0) {
            skin.animationTicksTotal = 0
            shouldUpdateHost = true
          }
        }
      }

      if(shouldUpdateHost) {
        host.markDirty()
        host.markChanged()
        updateClient()
      }
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      for ( skin <- skins ) {
        skin.save(nbt)
      }
      nbt.setBoolean("hideBody", hideBody)
      nbt.setBoolean("hideNameTag", hideNameTag)
      nbt.setBoolean("hideUpgrades", hideUpgrades)
    }

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      for ( skin <- skins ) {
        skin.load(nbt)
      }
      if(nbt.hasKey("hideBody")) {
        hideBody = nbt.getBoolean("hideBody")
      }
      if(nbt.hasKey("hideNameTag")) {
        hideNameTag = nbt.getBoolean("hideNameTag")
      }
      if(nbt.hasKey("hideUpgrades")) {
        hideUpgrades = nbt.getBoolean("hideUpgrades")
      }
    }

    override def onDisconnect(node: Node) {
      super.onDisconnect(node)
      if (node == this.node) {
        def spawnStack(stack: ItemStack): Unit = {
          val world = host.world
          val entity = new EntityItem(world, host.xPosition, host.yPosition, host.zPosition, stack.copy())
          entity.motionY = 0.04
          entity.delayBeforeCanPickup = 5
          world.spawnEntityInWorld(entity)
        }
        for(skin <- skins) {
          if(skin.skin.isDefined) {
            spawnStack(skin.skin.get)
            skin.skin = None
          }
          skin.rotX = 0
          skin.rotY = 0
          skin.rotZ = 0
          skin.animationTicksLeft = 0
          skin.animationTicksTotal = 0
        }
      }
    }
  }
}
