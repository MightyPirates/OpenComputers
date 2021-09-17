package li.cil.oc.integration.forestry

import java.util

import cpw.mods.fml.common.Loader
import forestry.api.apiculture.{BeeManager, IBeeHousing}
import forestry.plugins.PluginApiculture
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{EnvironmentHost, Node, Visibility}
import li.cil.oc.api.{Network, internal, prefab}
import li.cil.oc.server.component.result
import li.cil.oc.server.component.traits.{NetworkAware, SideRestricted, WorldAware}
import li.cil.oc.util.{BlockPosition, InventoryUtils}
import li.cil.oc.util.ExtendedArguments.extendedArguments
import li.cil.oc.util.ExtendedWorld._
import net.bdew.gendustry.api.blocks.IIndustrialApiary
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class UpgradeBeekeeper(val host: EnvironmentHost with internal.Agent) extends prefab.ManagedEnvironment with DeviceInfo with WorldAware with SideRestricted with NetworkAware {
  override val node: Node = Network.newNode(this, Visibility.Network).
    withComponent("beekeeper", Visibility.Neighbors).
    withConnector().
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "BeeKeeper",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Breeding bees for you (almost)"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
  override def position: BlockPosition = BlockPosition(host)
  override protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection = args.checkSideAny(n)

  private def withApiary(side: ForgeDirection, f: IBeeHousing => Array[AnyRef]) =
    if (host.mainInventory.getSizeInventory > 0) {
      position.world.get.getTileEntity(position.offset(side)) match {
        case housing: IBeeHousing => f(housing)
        case _ => result(false, "Not facing an apiary")
      }
    }
    else result(false, "No inventory?")

  @Callback(doc = """function(side:number):boolean -- Swap the queen from the selected slot with the apiary at the specified side.""")
  def swapQueen(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withApiary(facing, housing => {
      val selected = host.mainInventory.getStackInSlot(host.selectedSlot)
      val oldQueen = housing.getBeeInventory.getQueen
      housing.getBeeInventory.setQueen(selected)
      host.mainInventory.setInventorySlotContents(host.selectedSlot, oldQueen)
      result(true)
    })
  }

  @Callback(doc = """function(side:number):boolean -- Swap the drone from the selected slot with the apiary at the specified side.""")
  def swapDrone(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withApiary(facing, housing => {
      val selected = host.mainInventory.getStackInSlot(host.selectedSlot)
      val oldQueen = housing.getBeeInventory.getDrone
      housing.getBeeInventory.setDrone(selected)
      host.mainInventory.setInventorySlotContents(host.selectedSlot, oldQueen)
      result(true)
    })
  }

  @Callback(doc = """function(side:number):number -- Get current progress percent for the apiary at the specified side.""")
  def getBeeProgress(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withApiary(facing, housing => result(housing.getBeekeepingLogic.getBeeProgressPercent))
  }

  @Callback(doc = """function(side:number):boolean -- Checks if current bee in the apiary at the specified side can work now.""")
  def canWork(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withApiary(facing, housing => result(housing.getBeekeepingLogic.canWork))
  }

  @Callback(doc = """function(honeyslot:number):boolean -- Analyzes bee in selected slot, uses honey from the specified slot.""")
  def analyze(context: Context, args: Arguments): Array[AnyRef] = {
    val inventory = host.mainInventory
    val specimenSlot = host.selectedSlot
    val specimen = inventory.getStackInSlot(specimenSlot)
    if (!BeeManager.beeRoot.isMember(specimen))
      return result(false, "Not a bee")

    val honeySlot = args.checkSlot(inventory, 0)
    val honeyStack = inventory.getStackInSlot(honeySlot)
    if (honeyStack== null || honeyStack.stackSize == 0 || (honeyStack.getItem != PluginApiculture.items.honeydew && honeyStack.getItem != PluginApiculture.items.honeyDrop))
      return result(false, "No honey!")

    val individual = BeeManager.beeRoot.getMember(specimen)
    if (!individual.isAnalyzed) {
      individual.analyze
      val nbttagcompound = new NBTTagCompound
      individual.writeToNBT(nbttagcompound)
      specimen.setTagCompound(nbttagcompound)
      inventory.setInventorySlotContents(specimenSlot, specimen)
      honeyStack.stackSize -= 1
      inventory.setInventorySlotContents(honeySlot, honeyStack)
    }
    result(true)
  }

  private def findSameStack(upgrade: ItemStack, inv: ISidedInventory):Int = {
    for (slot <- 2 to 5) {
      val u = inv.getStackInSlot(slot)
      if (u != null && u.getItem == upgrade.getItem && upgrade.getItemDamage == u.getItemDamage)
        return slot
    }
    0
  }

  private def findEmptySlot(inv: ISidedInventory):Int = {
    for (slot <- 2 to 5) {
      if (inv.getStackInSlot(slot) == null)
        return slot
    }
    0
  }

  @Callback(doc = """function(side:number):boolean -- Tries to add industrial upgrade from the selected slot to industrial apiary at the given side.""")
  def addIndustrialUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
    if (!Loader.isModLoaded("gendustry")) return result(false, "Gendustry not loaded!")
    val inventory = host.mainInventory
    val facing = checkSideForAction(args, 0)
    val upgrade = inventory.getStackInSlot(host.selectedSlot)
    if (upgrade == null) return result(false, "No upgrade in selected slot")
    position.world.get.getTileEntity(position.offset(facing)) match {
      case ia : IIndustrialApiary =>
        val inv = ia.asInstanceOf[ISidedInventory]
        if (!inv.isItemValidForSlot(2, upgrade))
          return result(false, "Upgrade does not fit")

        var slot = findSameStack(upgrade, inv)
        if (slot == 0)
          slot = findEmptySlot(inv)

        val u = inv.getStackInSlot(slot)
        if (u == null)
          inv.setInventorySlotContents(slot, upgrade)
        else {
          u.stackSize += upgrade.stackSize
          inv.setInventorySlotContents(slot, u)
        }
        inventory.setInventorySlotContents(host.selectedSlot, null)
        result(true)

      case _ => result(false, "Not facing an industrial apiary")
    }
  }
  @Callback(doc = """function(side:number, slot: number):table -- Get industrial upgrade in the given slot of the industrial apiary at the given side.""")
  def getIndustrialUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
    if (!Loader.isModLoaded("gendustry")) return result(Unit, "Gendustry not loaded!")
    val facing = checkSideForAction(args, 0)
    position.world.get.getTileEntity(position.offset(facing)) match {
      case ia: IIndustrialApiary =>
        val inv = ia.asInstanceOf[ISidedInventory]
        val slot = args.checkInteger(1) + 1
        if (slot < 2 || slot > 5)
          return result(Unit, "Wrong slot index (should be 1-4)")

        result(inv.getStackInSlot(slot))

      case _ => result(Unit, "Not facing an industrial apiary")
    }
  }
  @Callback(doc = """function(side:number, slot: number):boolean -- Remove industrial upgrade from the given slot of the industrial apiary at the given side.""")
  def removeIndustrialUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
    if (!Loader.isModLoaded("gendustry")) return result(false, "Gendustry not loaded!")
    val facing = checkSideForAction(args, 0)
    position.world.get.getTileEntity(position.offset(facing)) match {
      case ia: IIndustrialApiary =>
        val inv = ia.asInstanceOf[ISidedInventory]
        val slot = args.checkInteger(1) + 1
        if (slot < 2 || slot > 5)
          return result(false, "Wrong slot index (should be 1-4)")

        val u = inv.getStackInSlot(slot)
        val res = InventoryUtils.insertIntoInventory(u, host.mainInventory)
        inv.setInventorySlotContents(slot, if (u.stackSize > 0) u else null)
        result(res)

      case _ => result(false, "Not facing an industrial apiary")
    }
  }
}
