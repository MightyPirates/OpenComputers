package li.cil.oc.integration.appeng

import appeng.api.config.Actionable
import appeng.api.networking.crafting.ICraftingLink
import appeng.api.networking.crafting.ICraftingRequester
import appeng.api.networking.security.IActionHost
import appeng.api.networking.security.MachineSource
import appeng.api.storage.data.IAEItemStack
import appeng.core.Api
import appeng.me.helpers.IGridProxyable
import appeng.tile.misc.TileInterface
import appeng.tile.networking.TileController
import appeng.util.item.AEItemStack
import com.google.common.collect.ImmutableSet
import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DriverController extends DriverTileEntity with EnvironmentAware {
  private type AETile = TileEntity with IGridProxyable with IActionHost

  def getTileEntityClass = {
    if (Api.instance.blocks.blockController != null && Api.instance.blocks.blockController.item != null)
      classOf[TileController]
    else
      classOf[TileInterface]
  }

  def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[AETile])

  override def providedEnvironment(stack: ItemStack) =
    if (stack != null &&
      Api.instance != null &&
      Api.instance.blocks != null &&
      Api.instance.blocks.blockController != null &&
      Block.getBlockFromItem(stack.getItem) == Api.instance.blocks.blockController.block) classOf[Environment] else null

  class Environment(tileEntity: AETile) extends ManagedTileEntityEnvironment[AETile](tileEntity, "me_controller") with NamedBlock {
    override def preferredName = "me_controller"

    override def priority = 0

    @Callback(doc = "function():table -- Get a list of tables representing the available CPUs in the network.")
    def getCpus(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getCrafting.getCpus.map(cpu => Map(
        "name" -> cpu.getName,
        "storage" -> cpu.getAvailableStorage,
        "coprocessors" -> cpu.getCoProcessors,
        "busy" -> cpu.isBusy)))

    @Callback(doc = "function():table -- Get a list of known item recipes. These can be used to issue crafting requests.")
    def getCraftables(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getProxy.getStorage.getItemInventory.getStorageList.
        filter(_.isCraftable).map(stack => {
        val patterns = tileEntity.getProxy.getCrafting.getCraftingFor(stack, null, 0, tileEntity.getWorldObj)
        val result = patterns.find(pattern => pattern.getOutputs.exists(_.isSameType(stack))) match {
          case Some(pattern) => pattern.getOutputs.find(_.isSameType(stack)).get
          case _ => stack.copy.setStackSize(0) // Should not be possible, but hey...
        }
        new Craftable(tileEntity, result)
      }).toArray)
    }

    @Callback(doc = "function():table -- Get a list of the stored items in the network.")
    def getItemsInNetwork(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getStorage.getItemInventory.getStorageList.map(_.getItemStack).toArray)

    @Callback(doc = "function():table -- Get a list of the stored fluids in the network.")
    def getFluidsInNetwork(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getStorage.getFluidInventory.getStorageList.map(_.getFluidStack).toArray)

    @Callback(doc = "function():number -- Get the average power injection into the network.")
    def getAvgPowerInjection(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getEnergy.getAvgPowerInjection)

    @Callback(doc = "function():number -- Get the average power usage of the network.")
    def getAvgPowerUsage(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getEnergy.getAvgPowerUsage)

    @Callback(doc = "function():number -- Get the idle power usage of the network.")
    def getIdlePowerUsage(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getEnergy.getIdlePowerUsage)

    @Callback(doc = "function():number -- Get the maximum stored power in the network.")
    def getMaxStoredPower(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getEnergy.getMaxStoredPower)

    @Callback(doc = "function():number -- Get the stored power in the network. ")
    def getStoredPower(context: Context, args: Arguments): Array[AnyRef] =
      result(tileEntity.getProxy.getEnergy.getStoredPower)
  }

  class Craftable(var controller: AETile, var stack: IAEItemStack) extends AbstractValue with ICraftingRequester {
    def this() = this(null, null)

    private val links = mutable.Set.empty[ICraftingLink]

    // ----------------------------------------------------------------------- //

    override def getRequestedJobs = ImmutableSet.copyOf(links.toIterable)

    override def jobStateChange(link: ICraftingLink) {
      links -= link
    }

    // rv1
    def injectCratedItems(link: ICraftingLink, stack: IAEItemStack, p3: Actionable) = stack

    // rv2
    def injectCraftedItems(link: ICraftingLink, stack: IAEItemStack, p3: Actionable) = stack

    override def getActionableNode = controller.getActionableNode

    override def getCableConnectionType(side: ForgeDirection) = controller.getCableConnectionType(side)

    override def securityBreak() = controller.securityBreak()

    override def getGridNode(side: ForgeDirection) = controller.getGridNode(side)

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():table -- Returns the item stack representation of the crafting result.")
    def getItemStack(context: Context, args: Arguments): Array[AnyRef] = Array(stack.getItemStack)

    @Callback(doc = "function([int amount]):userdata -- Requests the item to be crafted, returning an object that allows tracking the crafting status.")
    def request(context: Context, args: Arguments): Array[AnyRef] = {
      if (controller == null || controller.isInvalid) {
        return result(Unit, "no controller")
      }

      val count = args.optInteger(0, 1)
      val request = stack.copy
      request.setStackSize(count)

      val craftingGrid = controller.getProxy.getCrafting
      val source = new MachineSource(controller)
      val future = craftingGrid.beginCraftingJob(controller.getWorldObj, controller.getProxy.getGrid, source, request, null)

      val status = new CraftingStatus()
      Future {
        try {
          val job = future.get() // Make 100% sure we wait for this outside the scheduled closure.
          EventHandler.schedule(() => {
            val link = craftingGrid.submitJob(job, Craftable.this, null, true, source)
            if (link != null) {
              status.setLink(link)
              links += link
            }
            else {
              status.fail("missing resources?")
            }
          })
        }
        catch {
          case e: Exception =>
            OpenComputers.log.debug("Error submitting job to AE2.", e)
            status.fail(e.toString)
        }
      }

      result(status)
    }

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      stack = AEItemStack.loadItemStackFromNBT(nbt)
      if (nbt.hasKey("dimension")) {
        val dimension = nbt.getInteger("dimension")
        val x = nbt.getInteger("x")
        val y = nbt.getInteger("y")
        val z = nbt.getInteger("z")
        EventHandler.schedule(() => {
          val world = DimensionManager.getWorld(dimension)
          val tileEntity = world.getTileEntity(x, y, z)
          if (tileEntity != null && tileEntity.isInstanceOf[AETile]) {
            controller = tileEntity.asInstanceOf[AETile]
          }
        })
      }
      links ++= nbt.getTagList("links", NBT.TAG_COMPOUND).map(
        (nbt: NBTTagCompound) => Api.instance.storage.loadCraftingLink(nbt, this))
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      stack.writeToNBT(nbt)
      if (controller != null && !controller.isInvalid) {
        nbt.setInteger("dimension", controller.getWorldObj.provider.dimensionId)
        nbt.setInteger("x", controller.xCoord)
        nbt.setInteger("y", controller.yCoord)
        nbt.setInteger("z", controller.zCoord)
      }
      nbt.setNewTagList("links", links)
    }
  }

  class CraftingStatus extends AbstractValue {
    private var isComputing = true
    private var link: Option[ICraftingLink] = None
    private var failed = false
    private var reason = "no link"

    def setLink(value: ICraftingLink) {
      isComputing = false
      link = Option(value)
    }

    def fail(reason: String) {
      isComputing = false
      failed = true
      this.reason = s"request failed ($reason)"
    }

    @Callback(doc = "function():boolean -- Get whether the crafting request has been canceled.")
    def isCanceled(context: Context, args: Arguments): Array[AnyRef] = {
      if (isComputing) return result(false, "computing")
      link.fold(result(failed, reason))(l => result(l.isCanceled))
    }

    @Callback(doc = "function():boolean -- Get whether the crafting request is done.")
    def isDone(context: Context, args: Arguments): Array[AnyRef] = {
      if (isComputing) return result(false, "computing")
      link.fold(result(!failed, reason))(l => result(l.isDone))
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      failed = link.fold(true)(!_.isDone)
      nbt.setBoolean("failed", failed)
      if (failed && reason != null) {
        nbt.setString("reason", reason)
      }
    }

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      isComputing = false
      failed = nbt.getBoolean("failed")
      if (failed && nbt.hasKey("reason")) {
        reason = nbt.getString("reason")
      }
    }
  }

}