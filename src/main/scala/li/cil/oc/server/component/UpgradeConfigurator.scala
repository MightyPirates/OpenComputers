package li.cil.oc.server.component

import java.util

import com.enderio.core.common.util.DyeColor
import crazypants.enderio.conduit.{ConnectionMode, IConduitBundle}
import crazypants.enderio.conduit.item.IItemConduit
import crazypants.enderio.conduit.item.filter.{IItemFilter, ItemFilter}
import crazypants.enderio.conduit.liquid.{AbstractEnderLiquidConduit, AbstractTankConduit, FluidFilter, ILiquidConduit}
import crazypants.enderio.conduit.redstone.{IInsulatedRedstoneConduit, IRedstoneConduit}
import crazypants.enderio.machine.RedstoneControlMode
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{EnvironmentHost, Node, Visibility}
import li.cil.oc.api.{Network, internal, prefab}
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.server.component.traits.{NetworkAware, SideRestricted, WorldAware}
import li.cil.oc.util.{BlockPosition, DatabaseAccess}
import li.cil.oc.util.ExtendedArguments.extendedArguments
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.{FluidRegistry, FluidStack}

import scala.collection.convert.WrapAsJava._

object UpgradeConfigurator {

  trait Common extends DeviceInfo {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "External device configurator",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "Sonic Screwdriver"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }

  class Adapter(val host: EnvironmentHost) extends prefab.ManagedEnvironment with Configurator with Common {
    override val node: Node = Network.newNode(this, Visibility.Network).
      withComponent("configurator", Visibility.Network).
      create()

    override def position: BlockPosition = BlockPosition(host)
    override protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection = args.checkSideAny(n)
  }

  class Drone(val host: EnvironmentHost with internal.Agent) extends prefab.ManagedEnvironment with Configurator with Common {
    override val node: Node = Network.newNode(this, Visibility.Network).
      withComponent("configurator", Visibility.Neighbors).
      create()

    override def position: BlockPosition = BlockPosition(host)
    override protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection = args.checkSideAny(n)
  }

  class Robot(val host: EnvironmentHost with tileentity.Robot) extends prefab.ManagedEnvironment with Configurator with RobotConfigurator with Common {
    override val node: Node = Network.newNode(this, Visibility.Network).
      withComponent("configurator", Visibility.Neighbors).
      create()

    override def position: BlockPosition = BlockPosition(host)
    override def inventory: IInventory = host.mainInventory
    override def selectedSlot: Int = host.selectedSlot
    override def selectedSlot_=(value: Int): Unit = host.setSelectedSlot(value)
    override protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection = host.toGlobal(args.checkSideForAction(n))
  }

  class Microcontroller(val host: EnvironmentHost with tileentity.Microcontroller) extends prefab.ManagedEnvironment with Configurator with Common {
    override val node: Node = Network.newNode(this, Visibility.Network).
      withComponent("configurator", Visibility.Neighbors).
      create()

    override def position: BlockPosition = BlockPosition(host)
    override protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection = host.toLocal(args.checkSideForAction(n))
  }

  trait ConfiguratorBase extends WorldAware
  {
    def conduitAt(position: BlockPosition): Option[IConduitBundle] =
      position.world match {
        case Some(world) => world.getTileEntity(position.x, position.y, position.z) match {
          case conduit: IConduitBundle => Some(conduit)
        }
        case _ => None
      }
    def withItemConduit(side: ForgeDirection, f: IItemConduit => Array[AnyRef]): Array[AnyRef] =
      conduitAt(position.offset(side)) match {
        case Some(conduit) if conduit.hasType(classOf[IItemConduit])
          => f(conduit.getConduit(classOf[IItemConduit]))
        case _ => result(Unit, "no item conduit here")
      }

    def withEnderFluidConduit(side: ForgeDirection, f: AbstractEnderLiquidConduit => Array[AnyRef]): Array[AnyRef] =
      conduitAt(position.offset(side)) match {
        case Some(conduit) if conduit.hasType(classOf[AbstractEnderLiquidConduit])
          => f(conduit.getConduit(classOf[AbstractEnderLiquidConduit]))
        case _ => result(Unit, "no ender conduit here")
      }

    def withFluidConduit(side: ForgeDirection, f: ILiquidConduit => Array[AnyRef]): Array[AnyRef] =
      conduitAt(position.offset(side)) match {
        case Some(conduit) if conduit.hasType(classOf[ILiquidConduit])
        => f(conduit.getConduit(classOf[ILiquidConduit]))
        case _ => result(Unit, "no liquid conduit here")
      }
    def withInsulatedRedstoneConduit(side: ForgeDirection, f: IInsulatedRedstoneConduit => Array[AnyRef]): Array[AnyRef] =
      conduitAt(position.offset(side)) match {
        case Some(conduit) if conduit.hasType(classOf[IInsulatedRedstoneConduit])
        => f(conduit.getConduit(classOf[IInsulatedRedstoneConduit]))
        case _ => result(Unit, "no liquid conduit here")
      }
  }

  trait Configurator extends ConfiguratorBase with SideRestricted with NetworkAware {

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection: number):table -- Get conduit configuration at the specified conduitDirection""")
    def getConduitConfiguration(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      conduitAt(position.offset(facing)) match {
        case Some(conduit) =>
          var info = Map[AnyRef,AnyRef]("HasFacade" -> conduit.hasFacade.asInstanceOf[AnyRef])
          if (conduit.hasType(classOf[IItemConduit]))
            info += "ItemConduit" -> convert(dir, conduit.getConduit(classOf[IItemConduit]))
          if (conduit.hasType(classOf[ILiquidConduit]))
            info += "LiquidConduit" -> convert(dir, conduit.getConduit(classOf[ILiquidConduit]))
          if (conduit.hasType(classOf[IRedstoneConduit]))
            info += "RedstoneConduit" -> convert(dir, conduit.getConduit(classOf[IRedstoneConduit]))
          result(info)

        case _ => result(null, "No conduit here")
      }
    }
    else
      result(null, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection: number, isInput:boolean, color:string):boolean -- Set item conduit input/output color""")
    def setItemConduitColor(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withItemConduit(facing, c =>{
        val color = DyeColor.valueOf(args.checkString(3).toUpperCase)
        val isInput = args.checkBoolean(2)
        if (isInput) c.setInputColor(dir, color) else c.setOutputColor(dir, color)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, priority:number):boolean -- Set item conduit output priority""")
    def setItemConduitOutputPriority(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withItemConduit(facing, c => {
        c.setOutputPriority(dir, args.checkInteger(2))
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection: number, database: string, dbSlot:number, filterIndex:number, isInput:boolean):boolean -- Set item from database slot to conduit input or output filter""")
    def setItemConduitFilter(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      val dbAddress = args.checkString(2)
      withItemConduit(facing, c =>
        DatabaseAccess.withDatabase(node, dbAddress, database => {
          val dbSlot = args.checkSlot(database.data, 3)
          val dbStack = database.getStackInSlot(dbSlot)
          val filterSlot = args.checkInteger(4)
          val isInput = args.checkBoolean(5)
          val f = if (isInput) c.getInputFilter(dir) else c.getOutputFilter(dir)
          if (f != null && f.getSlotCount > filterSlot && f.isInstanceOf[ItemFilter]) {
            f.asInstanceOf[ItemFilter].setInventorySlotContents(filterSlot, dbStack)
            result(true)
          }
          else result(false, "Wrong or no item filter")
        })
      )
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, mode:string):boolean -- Set item conduit connection mode (IN_OUT,INPUT,OUTPUT,DISABLED,NOT_SET)""")
    def setItemConduitConnectionMode(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withItemConduit(facing, c => {
        val mode = ConnectionMode.valueOf(args.checkString(2).toUpperCase)
        c.setConnectionMode(dir, mode)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, color:string):boolean -- Set item conduit extraction redstone signal color""")
    def setItemConduitExtractionRedstoneColor(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withItemConduit(facing, c => {
        val color = DyeColor.valueOf(args.checkString(2).toUpperCase)
        c.setExtractionSignalColor(dir, color)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, mode:string):boolean -- Set item conduit extraction redstone signal mode (IGNORE,ON,OFF,NEVER). """)
    def setItemConduitExtractionRedstoneMode(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withItemConduit(facing, c => {
        val mode = RedstoneControlMode.valueOf(args.checkString(2).toUpperCase)
        c.setExtractionRedstoneMode(mode, dir)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, fluid:string, blacklist:boolean, isInput:boolean[, filterIndex:number]):boolean -- Set Ender liquid conduit filter""")
    def setEnderLiquidConduitFilter(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      val fluidName = args.checkString(2)
      val filterIndex = args.optInteger(5, 0)
      val ff = new FluidFilter
      ff.setBlacklist(args.checkBoolean(3))
      ff.setFluid(filterIndex, new FluidStack(FluidRegistry.getFluid(fluidName), 0))
      withEnderFluidConduit(facing, c => {
        c.setFilter(dir, ff, args.checkBoolean(4))
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, isInput:boolean, color:string):boolean -- Set Color for Ender liquid conduit input or output.""")
    def setEnderLiquidConduitColor(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withEnderFluidConduit(facing, c => {
        val isInput = args.checkBoolean(2)
        val color = DyeColor.valueOf(args.checkString(3).toUpperCase)
        if (isInput) c.setInputColor(dir, color) else c.setOutputColor(dir, color)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, mode:string):boolean -- Set liquid conduit connection mode (IN_OUT,INPUT,OUTPUT,DISABLED,NOT_SET)""")
    def setLiquidConduitConnectionMode(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withFluidConduit(facing, c => {
        val mode = ConnectionMode.valueOf(args.checkString(2).toUpperCase)
        c.setConnectionMode(dir, mode)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, color:string):boolean -- Set fluid conduit extraction redstone signal color""")
    def setLiquidConduitExtractionRedstoneColor(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withFluidConduit(facing, c => {
        val color = DyeColor.valueOf(args.checkString(2).toUpperCase)
        c.setExtractionSignalColor(dir, color)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, mode:string):boolean -- Set fluid conduit extraction redstone signal mode at side facing conduitDirection. mode:(IGNORE,ON,OFF,NEVER)""")
    def setLiquidConduitExtractionRedstoneMode(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withFluidConduit(facing, c => {
        val mode = RedstoneControlMode.valueOf(args.checkString(2).toUpperCase)
        c.setExtractionRedstoneMode(mode, dir)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, color:string):boolean -- Set redstone conduit signal color at side facing conduitDirection""")
    def setRedstoneConduitSignalColor(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withInsulatedRedstoneConduit(facing, c => {
        val color = DyeColor.valueOf(args.checkString(2).toUpperCase)
        c.setSignalColor(dir, color)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, strength:boolean):boolean -- Set redstone conduit signal strength. (true = strong signal, false = weak)""")
    def setRedstoneConduitSignalStrength(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val dir = args.checkSideAny(1)
      withInsulatedRedstoneConduit(facing, c => {
        val strength = args.checkBoolean(2)
        c.setOutputStrength(dir, strength)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")

    private def convert(dir: ForgeDirection, ic: IItemConduit): Map[AnyRef, AnyRef] = Map(
        "OutputColor" -> ic.getOutputColor(dir).getName,
        "InputColor" -> ic.getInputColor(dir).getName,
        "RoundRobinEnabled" -> ic.isRoundRobinEnabled(dir).asInstanceOf[AnyRef],
        "SelfFeedEnabled" -> ic.isSelfFeedEnabled(dir).asInstanceOf[AnyRef],
        "ExtractionRedstoneConditionMet" -> ic.isExtractionRedstoneConditionMet(dir).asInstanceOf[AnyRef],
        "OutputPriority" -> ic.getOutputPriority(dir).asInstanceOf[AnyRef],
        "InputFilter" -> convert(ic.getInputFilter(dir)),
        "OutputFilter" -> convert(ic.getOutputFilter(dir)),
        "ConnectionMode" -> ic.getConnectionMode(dir),
        "ExtractionRedstoneMode" -> ic.getExtractionRedstoneMode(dir),
        "ExtractionRedstoneColor" -> ic.getExtractionSignalColor(dir)
        )

    private def convert(dir: ForgeDirection, ic: IRedstoneConduit): Map[AnyRef, AnyRef] = Map(
        "SignalColor" -> ic.getSignalColor(dir),
        "IsOutputStrong" -> ic.isProvidingStrongPower(dir).toString,
        "ConnectionMode" -> ic.getConnectionMode(dir)
    )

    private def convert(dir: ForgeDirection, ic: ILiquidConduit): Map[AnyRef, AnyRef] = ic match {
      case c: AbstractEnderLiquidConduit => Map[AnyRef,AnyRef](
        "InputFilter" -> convert(c.getFilter(dir, true)),
        "OutputFilter" -> convert(c.getFilter(dir, false)),
        "InputColor" -> c.getInputColor(dir).getName,
        "OutputColor" -> c.getOutputColor(dir).getName,
        "ConnectionMode" -> c.getConnectionMode(dir),
        "ExtractionRedstoneMode" -> c.getExtractionRedstoneMode(dir),
        "ExtractionRedstoneColor" -> c.getExtractionSignalColor(dir)
      )

      case c: AbstractTankConduit => Map[AnyRef,AnyRef](
        "FluidType" -> c.getFluidType.getLocalizedName,
        "ConnectionMode" -> c.getConnectionMode(dir),
        "ExtractionRedstoneMode" -> c.getExtractionRedstoneMode(dir),
        "ExtractionRedstoneColor" -> c.getExtractionSignalColor(dir)
      )
      case _ => null
    }
    private def convert(f: FluidFilter): Map[AnyRef, AnyRef] = if (f != null) Map (
      "blacklist" -> f.isBlacklist.asInstanceOf[AnyRef],
      "1" -> Option (f.getFluidStackAt (0) ).fold ("") (_.getLocalizedName),
      "2" -> Option (f.getFluidStackAt (1) ).fold ("") (_.getLocalizedName),
      "3" -> Option (f.getFluidStackAt (2) ).fold ("") (_.getLocalizedName),
      "4" -> Option (f.getFluidStackAt (3) ).fold ("") (_.getLocalizedName),
      "5" -> Option (f.getFluidStackAt (4) ).fold ("") (_.getLocalizedName) )
    else
      null


    private def convert(f: IItemFilter): Map[AnyRef, AnyRef] = f match {
      case f: ItemFilter =>
        var filterInfo = Map[AnyRef,AnyRef](
          "Advanced" -> f.isAdvanced.asInstanceOf[AnyRef],
          "Blacklist" -> f.isBlacklist.asInstanceOf[AnyRef],
          "MatchMeta" -> f.isMatchMeta.asInstanceOf[AnyRef],
          "MatchNBT" -> f.isMatchNBT.asInstanceOf[AnyRef],
          "UseOreDict" -> f.isUseOreDict.asInstanceOf[AnyRef],
          "Sticky" -> f.isSticky.asInstanceOf[AnyRef],
          "FuzzyMode" -> f.getFuzzyMode.toString
        )
        var items = List[ItemStack]()
        for (i <- 0 to f.getSizeInventory) {
          if (f.getStackInSlot(i) != null)
            items = f.getStackInSlot(i) :: items
        }
        filterInfo += "FilterItems" -> items.toArray
        filterInfo

      case _ => Map[AnyRef,AnyRef]()
    }
  }
  trait RobotConfigurator extends ConfiguratorBase with SideRestricted with NetworkAware with traits.InventoryAware {

    //noinspection ScalaUnusedSymbol
    @Callback(doc = """function(side:number, conduitDirection:number, input:boolean):boolean -- Replace item conduit input or output filter with the filter in selected slot""")
    def replaceConduitFilter(context: Context, args: Arguments): Array[AnyRef] = if (Mods.EnderIO.isModAvailable) {
      val facing = checkSideForAction(args, 0)
      val stack = inventory.getStackInSlot(selectedSlot)
      val dir = args.checkSideAny(1)
      val isInput = args.checkBoolean(2)
      withItemConduit(facing, c => {
        val old = if (isInput) c.getInputFilterUpgrade(dir) else c.getOutputFilterUpgrade(dir)
        if (isInput) c.setInputFilterUpgrade(dir, stack) else c.setOutputFilterUpgrade(dir, stack)
        inventory.setInventorySlotContents(selectedSlot, old)
        result(true)
      })
    }
    else
      result(false, "EnderIO not loaded")
  }
}
