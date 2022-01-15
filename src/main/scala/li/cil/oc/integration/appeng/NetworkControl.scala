package li.cil.oc.integration.appeng

import java.lang

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.networking.IGridNode
import appeng.api.networking.crafting.{CraftingItemList, ICraftingLink, ICraftingRequester}
import appeng.api.networking.security.{BaseActionSource, IActionHost, MachineSource}
import appeng.api.networking.storage.IBaseMonitor
import appeng.api.storage.IMEMonitorHandlerReceiver
import appeng.api.storage.data.{IAEItemStack, IItemList}
import appeng.api.util.AECableType
import appeng.me.cluster.implementations.CraftingCPUCluster
import appeng.me.helpers.IGridProxyable
import appeng.tile.crafting.TileCraftingMonitorTile
import appeng.util.item.AEItemStack
import com.google.common.collect.ImmutableSet
import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Node
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.appeng.NetworkControl._
import li.cil.oc.integration.ec.ECUtil
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.existentials

//noinspection ScalaUnusedSymbol
// Note to self: this class is used by ExtraCells (and potentially others), do not rename / drastically change it.
trait NetworkControl[AETile >: Null <: TileEntity with IGridProxyable with IActionHost] {
  def tile: AETile

  def node: Node

  private def allItems: Iterable[IAEItemStack] = tile.getProxy.getStorage.getItemInventory.getStorageList
  private def allCraftables: Iterable[IAEItemStack] = allItems.collect{ case aeItem if aeItem.isCraftable => aeCraftItem(aeItem, tile) }

  @Callback(doc = "function():table -- Get a list of tables representing the available CPUs in the network.")
  def getCpus(context: Context, args: Arguments): Array[AnyRef] = {
    val buffer = new mutable.ListBuffer[Map[String, Any]]
    var index = 0
    tile.getProxy.getCrafting.getCpus.foreach(cpu => {
      buffer.append(Map(
        "name" -> cpu.getName,
        "storage" -> cpu.getAvailableStorage,
        "coprocessors" -> cpu.getCoProcessors,
        "busy" -> cpu.isBusy,
        "cpu" -> new Cpu(tile, index, cpu.asInstanceOf[CraftingCPUCluster])
      ))
      index += 1
    })
    result(buffer.toArray)
  }

  @Callback(doc = "function([filter:table]):table -- Get a list of known item recipes. These can be used to issue crafting requests.")
  def getCraftables(context: Context, args: Arguments): Array[AnyRef] = {
    val filter = args.optTable(0, Map.empty[AnyRef, AnyRef]).collect {
      case (key: String, value: AnyRef) => (key, value)
    }
    result(allCraftables
      .collect{ case aeCraftItem if filter.isEmpty || matches(convert(aeCraftItem, tile), filter) => new NetworkControl.Craftable(tile, aeCraftItem) }
      .toArray)
  }

  @Callback(doc = "function([filter:table]):table -- Get a list of the stored items in the network.")
  def getItemsInNetwork(context: Context, args: Arguments): Array[AnyRef] = {
    val filter = args.optTable(0, Map.empty[AnyRef, AnyRef]).collect {
      case (key: String, value: AnyRef) => (key, value)
    }
    result(allItems
      .map(item => convert(item, tile))
      .filter(hash => matches(hash, filter))
      .toArray)
  }

  @Callback(doc = "function():userdata -- Get an iterator object for the list of the items in the network.")
  def allItems(context: Context, args: Arguments): Array[AnyRef] = {
    result(new NetworkContents(tile))
  }

  @Callback(doc = "function(filter:table, dbAddress:string[, startSlot:number[, count:number]]): Boolean -- Store items in the network matching the specified filter in the database with the specified address.")
  def store(context: Context, args: Arguments): Array[AnyRef] = {
    val filter = args.checkTable(0).collect {
      case (key: String, value: AnyRef) => (key, value)
    }
    DatabaseAccess.withDatabase(node, args.checkString(1), database => {
      val items = allItems
        .collect{ case aeItem if matches(convert(aeItem, tile), filter) => aePotentialItem(aeItem, tile)}.toArray
      val offset = args.optSlot(database.data, 2, 0)
      val count = args.optInteger(3, Int.MaxValue) min (database.size - offset) min items.length
      var slot = offset
      for (i <- 0 until count) {
        val stack = Option(items(i)).map(_.getItemStack.copy()).orNull
        while (database.getStackInSlot(slot) != null && slot < database.size) slot += 1
        if (database.getStackInSlot(slot) == null) {
          database.setStackInSlot(slot, stack)
        }
      }
      result(true)
    })
  }

  @Callback(doc = "function():table -- Get a list of the stored fluids in the network.")
  def getFluidsInNetwork(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getStorage.getFluidInventory.getStorageList.filter(stack =>
      !Mods.ExtraCells.isAvailable || ECUtil.canSeeFluidInNetwork(stack)).
      map(_.getFluidStack).toArray)

  @Callback(doc = "function():number -- Get the average power injection into the network.")
  def getAvgPowerInjection(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getEnergy.getAvgPowerInjection)

  @Callback(doc = "function():number -- Get the average power usage of the network.")
  def getAvgPowerUsage(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getEnergy.getAvgPowerUsage)

  @Callback(doc = "function():number -- Get the idle power usage of the network.")
  def getIdlePowerUsage(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getEnergy.getIdlePowerUsage)

  @Callback(doc = "function():number -- Get the maximum stored power in the network.")
  def getMaxStoredPower(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getEnergy.getMaxStoredPower)

  @Callback(doc = "function():number -- Get the stored power in the network. ")
  def getStoredPower(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getEnergy.getStoredPower)

  private def matches(stack: java.util.HashMap[String, AnyRef], filter: scala.collection.mutable.Map[String, AnyRef]): Boolean = {
    if (stack == null) return false
    filter.forall {
      case (key: String, value: AnyRef) => 
        val stack_value = stack.get(key)
        value match {
          case number: Number => stack_value match {
            case stack_number: Number => number.intValue == stack_number.intValue
            case any => number.toString.equals(any.toString)
          }
          case any => any.toString.equals(stack_value.toString)
        }
    }
  }
}

object NetworkControl {

  //noinspection ScalaUnusedSymbol
  class Craftable(var controller: TileEntity with IGridProxyable with IActionHost, var stack: IAEItemStack) extends AbstractValue with ICraftingRequester {
    def this() = this(null, null)

    private val links = mutable.Set.empty[ICraftingLink]

    // ----------------------------------------------------------------------- //

    //noinspection RedundantCollectionConversion
    override def getRequestedJobs: ImmutableSet[ICraftingLink] = ImmutableSet.copyOf(links.toIterable)

    override def jobStateChange(link: ICraftingLink) {
      links -= link
    }

    override def injectCraftedItems(link: ICraftingLink, stack: IAEItemStack, p3: Actionable): IAEItemStack = stack

    override def getActionableNode: IGridNode = controller.getActionableNode

    override def getCableConnectionType(side: ForgeDirection): AECableType = controller.getCableConnectionType(side)

    override def securityBreak(): Unit = controller.securityBreak()

    override def getGridNode(side: ForgeDirection): IGridNode = controller.getGridNode(side)

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():table -- Returns the item stack representation of the crafting result.")
    def getItemStack(context: Context, args: Arguments): Array[AnyRef] = Array(stack.getItemStack)

    @Callback(doc = "function([amount:int[, prioritizePower:boolean[, cpuName:string]]]):userdata -- Requests the item to be crafted, returning an object that allows tracking the crafting status.")
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
      val prioritizePower = args.optBoolean(1, true)
      val cpuName = args.optString(2, "")
      val cpu = if (!cpuName.isEmpty) {
        controller.getProxy.getCrafting.getCpus.collectFirst({
          case c if cpuName.equals(c.getName) => c
        }).orNull
      } else null

      val status = new CraftingStatus()
      Future {
        try {
          val job = future.get() // Make 100% sure we wait for this outside the scheduled closure.
          EventHandler.scheduleServer(() => {
            val link = craftingGrid.submitJob(job, Craftable.this, cpu, prioritizePower, source)
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
      loadController(nbt, c => controller = c)
      links ++= nbt.getTagList("links", NBT.TAG_COMPOUND).map(
        (nbt: NBTTagCompound) => AEApi.instance.storage.loadCraftingLink(nbt, this))
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      stack.writeToNBT(nbt)
      saveController(controller, nbt)
      nbt.setNewTagList("links", links.map(_.writeToNBT _))
    }
  }

  //noinspection ScalaUnusedSymbol
  class Cpu(var controller: TileEntity with IGridProxyable, var index : Int, var cpu : CraftingCPUCluster) extends AbstractValue {
    def this() = this(null, 0, null)

    @Callback(doc = "function():boolean -- Cancel this CPU current crafting job.")
    def cancel(context: Context, args: Arguments): Array[AnyRef] = {
      if (!getCpu.isBusy)
        result(false)
      else {
        getCpu.cancel()
        result(true)
      }
    }

    @Callback(doc = "function():boolean -- Is cpu active?")
    def isActive(context: Context, args: Arguments): Array[AnyRef] = {
      result(getCpu.isActive)
    }

    @Callback(doc = "function():boolean -- Is cpu busy?")
    def isBusy(context: Context, args: Arguments): Array[AnyRef] = {
      result(getCpu.isBusy)
    }

    @Callback(doc = "function():table -- Get currently crafted items.")
    def activeItems(context: Context, args: Arguments): Array[AnyRef] = {
      val list = AEApi.instance.storage.createItemList
      getCpu.getListOfItem(list, CraftingItemList.ACTIVE)
      result(list.map(item => convert(item, controller)).toArray)
    }

    @Callback(doc = "function():table -- Get pending items.")
    def pendingItems(context: Context, args: Arguments): Array[AnyRef] = {
      val list = AEApi.instance.storage.createItemList
      getCpu.getListOfItem(list, CraftingItemList.PENDING)
      result(list.map(item => convert(item, controller)).toArray)
    }

    @Callback(doc = "function():table -- Get stored items.")
    def storedItems(context: Context, args: Arguments): Array[AnyRef] = {
      val list = AEApi.instance.storage.createItemList
      getCpu.getListOfItem(list, CraftingItemList.STORAGE)
      result(list.map(item => convert(item, controller)).toArray)
    }

    @Callback(doc = "function():table -- Get crafting final output.")
    def finalOutput(context: Context, args: Arguments): Array[AnyRef] = {
      val monitor = getCpu.getTiles.find(t => t.isInstanceOf[TileCraftingMonitorTile])
      if (monitor.isEmpty)
        result(null, "No crafting monitor")
      else {
        val aeStack = monitor.get.asInstanceOf[TileCraftingMonitorTile].getJobProgress
        if (aeStack == null)
          result(null, "Nothing is crafted")
        else
          result(aeStack.getItemStack)
      }
    }
    
    private def getCpu = {
      if (cpu == null && controller != null) {
        var i = 0
        for (c <- controller.getProxy.getCrafting.getCpus) {
          if (i == index) {
            cpu = c.asInstanceOf[CraftingCPUCluster]
          }
          i += 1
        }
      }
      if (cpu == null)
        throw new Exception("Broken CPU cluster")
      cpu
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      nbt.setInteger("index", index)
      saveController(controller, nbt)
    }
    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      index = nbt.getInteger("index")
      loadController(nbt, c => controller = c)
    }
  }

  //noinspection ScalaUnusedSymbol
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

  //noinspection ConvertNullInitializerToUnderscore
  class NetworkContents(var controller: TileEntity with IGridProxyable with IActionHost) extends AbstractValue with IMEMonitorHandlerReceiver[IAEItemStack]
  {
    def this() = this(null)
    if (controller != null)
      controller.getProxy.getStorage.getItemInventory.addListener(this, null)
    private var addedListener = true
    private var items : IItemList[IAEItemStack] = null
    private var itemIterator : java.util.Iterator[IAEItemStack] = null
    private var index = 0

    override def call(context: Context, arguments: Arguments): Array[AnyRef] = {
      if (controller == null)
        return null
      if (!addedListener) {
        controller.getProxy.getStorage.getItemInventory.addListener(this, null)
        addedListener = true
      }
      if (items == null) {
        items = controller.getProxy.getStorage.getItemInventory.getStorageList
        if (items != null)
          itemIterator = items.iterator
        if (itemIterator != null)
          for (_ <- 1 to index) {
            if (itemIterator.hasNext)
              itemIterator.next
          }
      }
      if (this.itemIterator == null && this.items != null)
        this.itemIterator = items.iterator
      if (!this.itemIterator.hasNext)
        return null
      index += 1
      Array[AnyRef](convert(itemIterator.next(), controller))
    }

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      addedListener = false
      loadController(nbt, c => controller = c)
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      nbt.setInteger("index", index)
      saveController(controller, nbt)
    }

    private var valid = true

    override def dispose(context: Context): Unit = {
      valid = false
    }

    override def isValid(verificationToken: Any): Boolean = valid

    override def onListUpdate(): Unit = {
      this.items = null
    }
    override def postChange(monitor: IBaseMonitor[IAEItemStack], change: lang.Iterable[IAEItemStack], actionSource: BaseActionSource): Unit = {
      this.items = null
    }
  }

  def aeCraftItem(aeItem: IAEItemStack, tile: TileEntity with IGridProxyable): IAEItemStack = {
    val patterns = tile.getProxy.getCrafting.getCraftingFor(aeItem, null, 0, tile.getWorldObj)
    patterns.find(pattern => pattern.getOutputs.exists(_.isSameType(aeItem))) match {
      case Some(pattern) => pattern.getOutputs.find(_.isSameType(aeItem)).get
      case _ => aeItem.copy.setStackSize(0) // Should not be possible, but hey...
    }
  }

  def aePotentialItem(aeItem: IAEItemStack, tile: TileEntity with IGridProxyable): IAEItemStack = {
    if (aeItem.getStackSize > 0 || !aeItem.isCraftable)
      aeItem
    else
      aeCraftItem(aeItem, tile)
  }

  def convert(aeItem: IAEItemStack, tile: TileEntity with IGridProxyable): java.util.HashMap[String, AnyRef] = {
    def hashConvert(value: java.util.HashMap[_, _]) = {
      val hash = new java.util.HashMap[String, AnyRef]
      value.collect{ case (k:String, v:AnyRef) => hash += k -> v }
      hash
    }
    val potentialItem = aePotentialItem(aeItem, tile)
    val result = Registry.convert(Array[AnyRef](potentialItem.getItemStack))
      .collect { case hash: java.util.HashMap[_,_] => hashConvert(hash) }
    if (result.length > 0) {
      val hash = result(0)
      // it would have been nice to put these fields in a registry convert
      // but the potential ae item needs the tile and position data
      hash.update("size", Long.box(aeItem.getStackSize))
      hash.update("isCraftable", Boolean.box(aeItem.isCraftable))
      return hash
    }
    null
  }

  private def loadController(nbt: NBTTagCompound, f : TileEntity with IGridProxyable with IActionHost => Unit ) : Unit = {
    if (nbt.hasKey("dimension")) {
      val dimension = nbt.getInteger("dimension")
      val x = nbt.getInteger("x")
      val y = nbt.getInteger("y")
      val z = nbt.getInteger("z")
      EventHandler.scheduleServer(() => {
        val world = DimensionManager.getWorld(dimension)
        val tileEntity = world.getTileEntity(x, y, z)
        if (tileEntity != null && tileEntity.isInstanceOf[TileEntity with IGridProxyable with IActionHost]) {
          f(tileEntity.asInstanceOf[TileEntity with IGridProxyable with IActionHost])
        }
      })
    }
  }

  private def saveController(controller: TileEntity, nbt: NBTTagCompound): Unit = {
    if (controller != null && !controller.isInvalid) {
      nbt.setInteger("dimension", controller.getWorldObj.provider.dimensionId)
      nbt.setInteger("x", controller.xCoord)
      nbt.setInteger("y", controller.yCoord)
      nbt.setInteger("z", controller.zCoord)
    }
  }
}