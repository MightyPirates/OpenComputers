package li.cil.oc.integration.appeng

import java.util

import appeng.api.config.Actionable
import appeng.api.networking.{IGridHost, IGridNode}
import appeng.api.networking.crafting.{ICraftingJob, ICraftingLink, ICraftingRequester}
import appeng.api.networking.security.IActionHost
import appeng.api.storage.data.IAEItemStack
import appeng.api.util.{AECableType, AEPartLocation}
import com.google.common.collect.ImmutableSet
import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Node
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.EventHandler
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.{DatabaseAccess, NbtDataStream}
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.RegistryKey
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.{Registry => VanillaRegistry}
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.server.ServerLifecycleHooks

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.existentials

// Note to self: this class is used by ExtraCells (and potentially others), do not rename / drastically change it.
trait NetworkControl[AETile >: Null <: TileEntity with IActionHost with IGridHost] {
  def tile: AETile
  def pos: AEPartLocation

  def node: Node

  private def aeCraftItem(aeItem: IAEItemStack): IAEItemStack = {
    val patterns = AEUtil.getGridCrafting(tile.getGridNode(pos).getGrid).getCraftingFor(aeItem, null, 0, tile.getLevel)
    patterns.find(pattern => pattern.getOutputs.exists(_.isSameType(aeItem))) match {
      case Some(pattern) => pattern.getOutputs.find(_.isSameType(aeItem)).get
      case _ => aeItem.copy.setStackSize(0) // Should not be possible, but hey...
    }
  }

  private def aePotentialItem(aeItem: IAEItemStack): IAEItemStack = {
    if (aeItem.getStackSize > 0 || !aeItem.isCraftable)
      aeItem
    else
      aeCraftItem(aeItem)
  }

  private def isSequentialTable(map: scala.collection.mutable.HashMap[_, _]): Boolean = {
    // if this element has n=number, it is the table wrapping showing how large the array is
    map.forall {
      case (key: String, _: Number) => key == "n"
      case (key: Number, _: AnyRef) => key.intValue >= 1
      case _ => false
    }
  }

  private def reduceSequentialTable[K, V](map: scala.collection.mutable.HashMap[K, V]): AnyRef = {
    // in place of a table pack, we want a hash map of tuples
    val tuples = new util.LinkedList[AnyRef]()
    map.collect {
      case (key: AnyRef, value: AnyRef) =>
        if (!key.isInstanceOf[String] || key.asInstanceOf[String] != "n") {
          tuples.add(reduceLuaValue(value))
        }
    }
    tuples.toArray
  }

  private def reduceHashTable(map: scala.collection.mutable.HashMap[_, _]): AnyRef = {
    val hash = new java.util.HashMap[AnyRef, AnyRef]()
    map.foreach {
      case (key: AnyRef, value: AnyRef) => {
        hash += key -> value
      }
    }
    hash
  }

  private def reduceLuaValue(any: AnyRef): AnyRef = {
    any match {
      case m: scala.collection.mutable.HashMap[_, _] =>
        if (isSequentialTable(m))
          reduceSequentialTable(m)
        else
          reduceHashTable(m)
      case _ => any
    }
  }

  private def getFilter(args: Arguments, index: Int): java.util.Map[AnyRef, AnyRef] = {
    val hash = new java.util.HashMap[AnyRef, AnyRef]()
    Registry.convert(Array[AnyRef](args.optTable (index, Map.empty[AnyRef, AnyRef] )))
      .head match {
        case map: mutable.Map[_, _] => map.collect {
          case (key: AnyRef, value: AnyRef) => hash += reduceLuaValue(key) -> reduceLuaValue(value)
        }
        case _ =>
      }
    hash
  }

  private def allItems: Iterable[IAEItemStack] = {
    val storage = AEUtil.getGridStorage(tile.getGridNode(pos).getGrid)
    val inventory = storage.getInventory(AEUtil.itemStorageChannel)
    inventory.getStorageList
  }

  private def allCraftables: Iterable[IAEItemStack] = allItems.collect{ case aeItem if aeItem.isCraftable => aeCraftItem(aeItem) }

  private def convert(aeItem: IAEItemStack): java.util.Map[AnyRef, AnyRef] = {
    // I would prefer to move the convert code to the registry for IAEItemStack
    // but craftables need the device that crafts them
    val hash = new java.util.HashMap[AnyRef, AnyRef]()
    Registry.convert(Array[AnyRef](aePotentialItem(aeItem).createItemStack()))
      .head
      .asInstanceOf[java.util.Map[Object, Object]]
      .collect {
      case (key, value) => hash += key -> value
    }
    hash.update("isCraftable", Boolean.box(aeItem.isCraftable))
    hash.update("size", Int.box(aeItem.getStackSize.toInt))
    hash
  }

  @Callback(doc = """function():table -- Get a list of tables representing the available CPUs in the network.""")
  def getCpus(context: Context, args: Arguments): Array[AnyRef] = {
    val buffer = new mutable.ListBuffer[Map[String, Any]]
    AEUtil.getGridCrafting(tile.getGridNode(pos).getGrid).getCpus.foreach(cpu => {
      buffer.append(Map(
        "name" -> cpu.getName,
        "storage" -> cpu.getAvailableStorage,
        "coprocessors" -> cpu.getCoProcessors,
        "busy" -> cpu.isBusy))
    })
    result(buffer.toArray)
  }

  @Callback(doc = """function([filter:table]):table -- Get a list of known item recipes. These can be used to issue crafting requests.""")
  def getCraftables(context: Context, args: Arguments): Array[AnyRef] = {
    val filter = getFilter(args, 0)
    result(allCraftables
      .collect{ case aeCraftItem if filter.isEmpty || matches(convert(aeCraftItem), filter) => new NetworkControl.Craftable(tile, pos, aeCraftItem) }
      .toArray)
  }

  @Callback(doc = """function([filter:table]):table -- Get a list of the stored items in the network.""")
  def getItemsInNetwork(context: Context, args: Arguments): Array[AnyRef] = {
    val filter = getFilter(args, 0)
    result(allItems
      .map(convert)
      .filter(matches(_, filter))
      .toArray)
  }

  @Callback(doc = """function([filter:table, dbAddress:string, startSlot:number, count:number]): bool -- Store items in the network matching the specified filter in the database with the specified address.""")
  def store(context: Context, args: Arguments): Array[AnyRef] = {
    val filter = getFilter(args, 0)
    val database = args.optString(1, null) match {
      case address: String => DatabaseAccess.database(node, address)
      case _ => DatabaseAccess.databases(node).headOption.getOrElse(throw new IllegalArgumentException("no database upgrade found"))
    }
    val items = allItems.collect{ case aeItem if matches(convert(aeItem), filter) => aePotentialItem(aeItem)}.toArray
    val offset = args.optSlot(database.data, 2, 0)
    val count = args.optInteger(3, Int.MaxValue) min (database.size - offset) min items.length
    var slot = offset
    for (i <- 0 until count) {
      val stack = Option(items(i)).map(_.createItemStack.copy()).orNull
      while (!database.getStackInSlot(slot).isEmpty && slot < database.size) slot += 1
      if (database.getStackInSlot(slot).isEmpty) {
        database.setStackInSlot(slot, stack)
      }
    }
    result(true)
  }

  @Callback(doc = """function():table -- Get a list of the stored fluids in the network.""")
  def getFluidsInNetwork(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridStorage(tile.getGridNode(pos).getGrid).getInventory(AEUtil.fluidStorageChannel).getStorageList.filter(stack =>
      stack != null).
        map(_.getFluidStack).toArray)

  @Callback(doc = """function():number -- Get the average power injection into the network.""")
  def getAvgPowerInjection(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).getAvgPowerInjection)

  @Callback(doc = """function():number -- Get the average power usage of the network.""")
  def getAvgPowerUsage(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).getAvgPowerUsage)

  @Callback(doc = """function():number -- Get the idle power usage of the network.""")
  def getIdlePowerUsage(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).getIdlePowerUsage)

  @Callback(doc = """function():number -- Get the maximum stored power in the network.""")
  def getMaxStoredPower(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).getMaxStoredPower)

  @Callback(doc = """function():number -- Get the stored power in the network. """)
  def getStoredPower(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).getStoredPower)

  @Callback(doc = """function():boolean -- True if the AE network is considered online""")
  def isNetworkPowered(context: Context, args: Arguments): Array[AnyRef] =
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).isNetworkPowered)

  @Callback(direct = false, doc = """function():number -- Returns the energy demand on the AE network""")
  def getEnergyDemand(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(1.5)
    result(AEUtil.getGridEnergy(tile.getGridNode(pos).getGrid).getEnergyDemand(Double.MaxValue))
  }

  private def matches(stack: java.util.Map[AnyRef, AnyRef], filter: scala.collection.mutable.Map[AnyRef, AnyRef]): Boolean = {
    if (stack == null) return false
    filter.forall {
      case (key: AnyRef, value: AnyRef) => contains(stack, key, value)
      case _ => false
    }
  }

  private def contains(stack: java.util.Map[AnyRef, AnyRef], key: AnyRef, value: AnyRef): Boolean = {
    stack.containsKey(key) && valueMatch(value, stack.get(key))
  }

  private def valueMatch(a: AnyRef, b: AnyRef): Boolean = {
    if ((a == null) != (b == null)) return false
    if (a == b) return true
    (a, b) match {
      case (a_number: Number, b_number: Number) => a_number.intValue == b_number.intValue
      case (a_vec: Array[_], b_vec: Array[_]) => a_vec.forall {
        case a_map: util.HashMap[_, _] => a_map.forall {
          case (a_key: AnyRef, a_value: AnyRef) => b_vec.collectFirst[Boolean] {
            case b_map: scala.collection.mutable.HashMap[_, _] => b_map.collectFirst[Boolean] {
              case (b_key: AnyRef, b_value: AnyRef) => valueMatch(a_key, b_key) && valueMatch(a_value, b_value)
            }.isDefined
          }.isDefined
          case _ => false
        }
      }
      case _ => false
    }
  }
}

object NetworkControl {

  object LinkCache {
    val linkCache = new mutable.HashMap[String, ICraftingLink]
    val statusCache = new  mutable.HashMap[String, CraftingStatus]

    def store(link: ICraftingLink): ICraftingLink = {
      statusCache.remove(link.getCraftingID) match {
        case Some(status: CraftingStatus) => status.setLink(link)
        case _ => linkCache += link.getCraftingID -> link
      }
      link
    }

    def store(status: CraftingStatus, id: String): Unit = {
      linkCache.remove(id) match {
        case Some(link: ICraftingLink) => status.setLink(link)
        case _ => statusCache += id -> status
      }
    }
  }

  class Craftable(var controller: TileEntity with IActionHost with IGridHost,
                  var pos: AEPartLocation,
                  var stack: IAEItemStack)
                    extends AbstractValue with ICraftingRequester with IGridHost {
    def this() = this(null, null, null)

    private val links = mutable.Set.empty[ICraftingLink]

    // ----------------------------------------------------------------------- //

    override def getRequestedJobs: ImmutableSet[ICraftingLink] = ImmutableSet.copyOf(links.toIterable)

    override def jobStateChange(link: ICraftingLink): Unit = links -= link

    // rv1
    def injectCratedItems(link: ICraftingLink, stack: IAEItemStack, p3: Actionable): IAEItemStack = stack

    // rv2
    def injectCraftedItems(link: ICraftingLink, stack: IAEItemStack, p3: Actionable): IAEItemStack = stack

    override def getActionableNode: IGridNode = controller.getActionableNode

    override def getGridNode(dir: AEPartLocation): IGridNode = controller.getGridNode(pos)
    override def getCableConnectionType(dir: AEPartLocation): AECableType = controller.getCableConnectionType(dir)
    override def securityBreak(): Unit = controller.securityBreak()

    // ----------------------------------------------------------------------- //

    private def withController(f: (TileEntity with IActionHost with IGridHost) => Array[AnyRef]): Array[AnyRef] = {
      if (delayData != null) {
        result((), "waiting for ae network to load")
      } else {
        if (controller == null || controller.isRemoved) {
          result((), "no controller")
        } else {
          f(controller)
        }
      }
    }

    private def withGridNode(f: (IGridNode) => Array[AnyRef]): Array[AnyRef] = {
      withController(c => Option(c.getGridNode(pos)) match {
        case Some(grid: IGridNode) => f(grid)
        case _ => result((), "no ae grid")
      })
    }

    @Callback(doc = """function():table -- Returns the item stack representation of the crafting result.""")
    def getItemStack(context: Context, args: Arguments): Array[AnyRef] = Array(stack.createItemStack())

    @Callback(doc = """function():number -- Returns the number of requests in progress.""")
    def requesting(context: Context, args: Arguments): Array[AnyRef] = {
      withGridNode(gridNode => {
        val craftingGrid = AEUtil.getGridCrafting(gridNode.getGrid)
        result(craftingGrid.requesting(stack))
      })
    }

    @Callback(doc = """function([amount:int=1, prioritizePower:boolean=true, cpuName:string]):userdata -- Requests item to be crafted, returning an object that allows tracking the crafting status.""")
    def request(context: Context, args: Arguments): Array[AnyRef] = {
      withGridNode(gridNode => {
        val prioritizePower = args.optBoolean(1, true)
        val count = args.optInteger(0, 1)
        val cpuName = args.optString(2, "")

        val request = stack.copy
        request.setStackSize(count)

        val craftingGrid = AEUtil.getGridCrafting(gridNode.getGrid)

        val source = new MachineSource(controller)
        val future = craftingGrid.beginCraftingJob(controller.getLevel, gridNode.getGrid, source, request, null)
        val cpu = if (!cpuName.isEmpty) {
          craftingGrid.getCpus.collectFirst({
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
      })
    }

    // ----------------------------------------------------------------------- //

    private val DIMENSION_KEY = "dimension"
    private val X_KEY = "x"
    private val Y_KEY = "y"
    private val Z_KEY = "z"
    private val LINKS_KEY = "links"
    private val POS_KEY = "pos"

    private val MAX_BACKOFF_TICKS = 20 * 5 // 5 seconds
    private val BACKOFF_SCALE = 2 // multiply by this factor on each failure

    private class EphemeralDelayData(val dimension: RegistryKey[World], val x: Int, val y: Int, val z: Int) {
      var delay: Int = 1
    }
    private var delayData: EphemeralDelayData = _ // null unless delay loading is active

    // return true when we do not want to try again, either because we completely failed or we succeeded
    // return false when things appears just not ready yet
    private def tryLoadGrid(dimension: RegistryKey[World], x: Int, y: Int, z: Int): Boolean = {
      val world = ServerLifecycleHooks.getCurrentServer.getLevel(dimension)
      if (world == null) {
        return false // maybe the dimension isn't loaded yet
      }
      val tileEntity = world.getBlockEntity(new BlockPos(x, y, z))
      if (tileEntity == null) {
        return false // maybe the chunk isn't loaded yet
      }
      if (!tileEntity.isInstanceOf[TileEntity with IActionHost with IGridHost]) {
        return true // failure: looks like the tile was swapped before we could see it
      }
      val gridHost = tileEntity.asInstanceOf[TileEntity with IActionHost with IGridHost]
      val gridNode = gridHost.getGridNode(pos)
      if (gridNode == null) {
        return false // this is typical as the ae network is still loading
      }
      val craftingGrid = AEUtil.getGridCrafting(gridNode.getGrid)
      if (craftingGrid == null) {
        return true // failure: not known right now what would cause this, bail out
      }
      craftingGrid.addNode(gridNode, this)
      controller = gridHost
      true // finally! no more retries needed
    }

    private def delayLoadGrid(): Unit = {
      if (delayData != null) {// weird if it was null
        if (tryLoadGrid(delayData.dimension, delayData.x, delayData.y, delayData.z)) {
          delayData = null // no longer needed
        } else {
          pushDelayLoadBackoff(delayData.delay * BACKOFF_SCALE)
        }
      }
    }

    private def pushDelayLoadBackoff(delay: Int): Unit = {
      if (delayData != null) { // should not be called if null
        delayData.delay = delay min MAX_BACKOFF_TICKS
        EventHandler.scheduleServer(delayLoadGrid, delayData.delay)
      }
    }

    override def loadData(nbt: CompoundNBT) {
      super.loadData(nbt)
      stack = AEUtil.itemStorageChannel.createStack(ItemStack.of(nbt))
      links ++= nbt.getList(LINKS_KEY, NBT.TAG_COMPOUND).map(
        (nbt: CompoundNBT) => LinkCache.store(AEUtil.aeApi.get.storage.loadCraftingLink(nbt, this)))
      pos = AEPartLocation.fromOrdinal(NbtDataStream.getOptInt(nbt, POS_KEY, AEPartLocation.INTERNAL.ordinal))
      if (nbt.contains(DIMENSION_KEY)) {
        val dimension = RegistryKey.create(VanillaRegistry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString(DIMENSION_KEY)))
        val x = nbt.getInt(X_KEY)
        val y = nbt.getInt(Y_KEY)
        val z = nbt.getInt(Z_KEY)
        delayData = new EphemeralDelayData(dimension, x, y, z)
        // all of this delay load could have been done nested
        // but i don't want infinite lambda nesting in cases where the load is never ready
        pushDelayLoadBackoff(1)
      }
    }

    override def saveData(nbt: CompoundNBT) {
      super.saveData(nbt)
      stack.createItemStack().save(nbt)
      nbt.setNewTagList(LINKS_KEY, links.map((link) => {
        val comp = new CompoundNBT()
        link.writeToNBT(comp)
        comp
      }))
      if (pos != null)
        nbt.putInt(POS_KEY, pos.ordinal)
      if (controller != null && !controller.isRemoved) {
        nbt.putString(DIMENSION_KEY, controller.getLevel.dimension.location.toString)
        nbt.putInt(X_KEY, controller.getBlockPos.getX)
        nbt.putInt(Y_KEY, controller.getBlockPos.getY)
        nbt.putInt(Z_KEY, controller.getBlockPos.getZ)
      }
    }
  }

  class CraftingStatus extends AbstractValue {
    private var isComputing: Boolean = true
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

    def asCraft(f: (ICraftingLink) => Array[AnyRef]): Array[AnyRef] = {
      if (isComputing) result((), "computing")
      else link match {
        case Some(craft: ICraftingLink) if !failed => f(craft)
        case _ => result(false, reason)
      }
    }

    @Callback(doc = """function():boolean -- Get whether the crafting request has been canceled.""")
    def isCanceled(context: Context, args: Arguments): Array[AnyRef] = {
      asCraft(craft => result(craft.isCanceled))
    }

    @Callback(doc = """function():boolean -- Get whether the crafting request is done.""")
    def isDone(context: Context, args: Arguments): Array[AnyRef] = {
      asCraft(craft => result(craft.isDone))
    }

    @Callback(doc = """function():boolean -- Cancels the request. Returns false if the craft cannot be canceled or nil if the link is computing""")
    def cancel(context: Context, args: Arguments): Array[AnyRef] = {
      asCraft(craft => {
        if (craft.isDone) {
          return result(false, "job already completed")
        }
        craft.cancel()
        result(true)
      })
    }

    private val COMPUTING_KEY: String = "computing"
    private val LINK_ID_KEY: String = "link"
    private val FAILED_KEY: String = "failed"
    private val REASON_KEY: String = "reason"

    override def saveData(nbt: CompoundNBT) {
      super.saveData(nbt)
      nbt.putBoolean(COMPUTING_KEY, isComputing)
      if (link.nonEmpty)
        nbt.putString(LINK_ID_KEY, link.get.getCraftingID)
      nbt.putBoolean(FAILED_KEY, failed)
      nbt.putString(REASON_KEY, reason)
    }

    override def loadData(nbt: CompoundNBT) {
      super.loadData(nbt)

      isComputing = NbtDataStream.getOptBoolean(nbt, COMPUTING_KEY, isComputing)
      val id = NbtDataStream.getOptString(nbt, LINK_ID_KEY, "")
      if (id.nonEmpty) {
        LinkCache.store(this, id)
      }
      failed = NbtDataStream.getOptBoolean(nbt, FAILED_KEY, failed)
      reason = NbtDataStream.getOptString(nbt, REASON_KEY, reason)
    }
  }
}