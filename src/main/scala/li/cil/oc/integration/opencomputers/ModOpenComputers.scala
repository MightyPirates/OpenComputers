package li.cil.oc.integration.opencomputers

import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.api.internal
import li.cil.oc.api.internal.Wrench
import li.cil.oc.api.manual.PathProvider
import li.cil.oc.api.prefab.ItemStackTabIconRenderer
import li.cil.oc.api.prefab.ResourceContentProvider
import li.cil.oc.api.prefab.TextureTabIconRenderer
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.markdown.segment.render.BlockImageProvider
import li.cil.oc.client.renderer.markdown.segment.render.ItemImageProvider
import li.cil.oc.client.renderer.markdown.segment.render.OreDictImageProvider
import li.cil.oc.client.renderer.markdown.segment.render.TextureImageProvider
import li.cil.oc.common.EventHandler
import li.cil.oc.common.Loot
import li.cil.oc.common.SaveHandler
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.event._
import li.cil.oc.common.item.Analyzer
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.RedstoneCard
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.template._
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.WirelessRedstone
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.server.machine.luac.NativeLua53Architecture
import li.cil.oc.server.network.Waypoints
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.util.Color
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.MinecraftForge

object ModOpenComputers extends ModProxy {
  override def getMod = Mods.OpenComputers

  override def initialize() {
    DroneTemplate.register()
    MicrocontrollerTemplate.register()
    NavigationUpgradeTemplate.register()
    RobotTemplate.register()
    ServerTemplate.register()
    TabletTemplate.register()
    TemplateBlacklist.register()

    api.IMC.registerWrenchTool("li.cil.oc.integration.opencomputers.ModOpenComputers.useWrench")
    api.IMC.registerItemCharge(
      "OpenComputers",
      "li.cil.oc.integration.opencomputers.ModOpenComputers.canCharge",
      "li.cil.oc.integration.opencomputers.ModOpenComputers.charge")
    api.IMC.registerInkProvider("li.cil.oc.integration.opencomputers.ModOpenComputers.inkCartridgeInkProvider")
    api.IMC.registerInkProvider("li.cil.oc.integration.opencomputers.ModOpenComputers.dyeInkProvider")

    ForgeChunkManager.setForcedChunkLoadingCallback(OpenComputers, ChunkloaderUpgradeHandler)

    FMLCommonHandler.instance.bus.register(EventHandler)
    FMLCommonHandler.instance.bus.register(SimpleComponentTickHandler.Instance)
    FMLCommonHandler.instance.bus.register(Tablet)

    MinecraftForge.EVENT_BUS.register(Analyzer)
    MinecraftForge.EVENT_BUS.register(AngelUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(ChunkloaderUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(EventHandler)
    MinecraftForge.EVENT_BUS.register(ExperienceUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(FileSystemAccessHandler)
    MinecraftForge.EVENT_BUS.register(GeolyzerHandler)
    MinecraftForge.EVENT_BUS.register(HoverBootsHandler)
    MinecraftForge.EVENT_BUS.register(Loot)
    MinecraftForge.EVENT_BUS.register(RobotCommonHandler)
    MinecraftForge.EVENT_BUS.register(SaveHandler)
    MinecraftForge.EVENT_BUS.register(Tablet)
    MinecraftForge.EVENT_BUS.register(Waypoints)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkCardHandler)
    MinecraftForge.EVENT_BUS.register(li.cil.oc.client.ComponentTracker)
    MinecraftForge.EVENT_BUS.register(li.cil.oc.server.ComponentTracker)

    api.Driver.add(DriverBlockEnvironments)

    api.Driver.add(DriverAPU)
    api.Driver.add(DriverComponentBus)
    api.Driver.add(DriverCPU)
    api.Driver.add(DriverDebugCard)
    api.Driver.add(DriverEEPROM)
    api.Driver.add(DriverFileSystem)
    api.Driver.add(DriverGeolyzer)
    api.Driver.add(DriverGraphicsCard)
    api.Driver.add(DriverInternetCard)
    api.Driver.add(DriverLinkedCard)
    api.Driver.add(DriverLootDisk)
    api.Driver.add(DriverMemory)
    api.Driver.add(DriverNetworkCard)
    api.Driver.add(DriverKeyboard)
    api.Driver.add(DriverRedstoneCard)
    api.Driver.add(DriverScreen)
    api.Driver.add(DriverTablet)
    api.Driver.add(DriverWirelessNetworkCard)

    api.Driver.add(DriverContainerCard)
    api.Driver.add(DriverContainerFloppy)
    api.Driver.add(DriverContainerUpgrade)

    api.Driver.add(DriverUpgradeAngel)
    api.Driver.add(DriverUpgradeBattery)
    api.Driver.add(DriverUpgradeChunkloader)
    api.Driver.add(DriverUpgradeCrafting)
    api.Driver.add(DriverUpgradeDatabase)
    api.Driver.add(DriverUpgradeExperience)
    api.Driver.add(DriverUpgradeGenerator)
    api.Driver.add(DriverUpgradeHover)
    api.Driver.add(DriverUpgradeInventory)
    api.Driver.add(DriverUpgradeInventoryController)
    api.Driver.add(DriverUpgradeLeash)
    api.Driver.add(DriverUpgradeNavigation)
    api.Driver.add(DriverUpgradePiston)
    api.Driver.add(DriverUpgradeSign)
    api.Driver.add(DriverUpgradeSolarGenerator)
    api.Driver.add(DriverUpgradeTank)
    api.Driver.add(DriverUpgradeTankController)
    api.Driver.add(DriverUpgradeTractorBeam)

    blacklistHost(classOf[internal.Adapter],
      Constants.BlockName.Geolyzer,
      Constants.BlockName.Keyboard,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.BatteryUpgradeTier1,
      Constants.ItemName.BatteryUpgradeTier2,
      Constants.ItemName.BatteryUpgradeTier3,
      Constants.ItemName.ChunkloaderUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.ExperienceUpgrade,
      Constants.ItemName.GeneratorUpgrade,
      Constants.ItemName.HoverUpgradeTier1,
      Constants.ItemName.HoverUpgradeTier2,
      Constants.ItemName.InventoryUpgrade,
      Constants.ItemName.NavigationUpgrade,
      Constants.ItemName.PistonUpgrade,
      Constants.ItemName.SolarGeneratorUpgrade,
      Constants.ItemName.TankUpgrade,
      Constants.ItemName.TractorBeamUpgrade,
      Constants.ItemName.LeashUpgrade)
    blacklistHost(classOf[internal.Drone],
      Constants.ItemName.APUTier1,
      Constants.ItemName.APUTier2,
      Constants.ItemName.GraphicsCardTier1,
      Constants.ItemName.GraphicsCardTier2,
      Constants.ItemName.GraphicsCardTier3,
      Constants.BlockName.Keyboard,
      Constants.ItemName.NetworkCard,
      Constants.ItemName.RedstoneCardTier1,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.HoverUpgradeTier1,
      Constants.ItemName.HoverUpgradeTier2)
    blacklistHost(classOf[internal.Microcontroller],
      Constants.ItemName.APUTier1,
      Constants.ItemName.APUTier2,
      Constants.ItemName.GraphicsCardTier1,
      Constants.ItemName.GraphicsCardTier2,
      Constants.ItemName.GraphicsCardTier3,
      Constants.BlockName.Keyboard,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.ChunkloaderUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.DatabaseUpgradeTier1,
      Constants.ItemName.DatabaseUpgradeTier2,
      Constants.ItemName.DatabaseUpgradeTier3,
      Constants.ItemName.ExperienceUpgrade,
      Constants.ItemName.GeneratorUpgrade,
      Constants.ItemName.HoverUpgradeTier1,
      Constants.ItemName.HoverUpgradeTier2,
      Constants.ItemName.InventoryUpgrade,
      Constants.ItemName.InventoryControllerUpgrade,
      Constants.ItemName.NavigationUpgrade,
      Constants.ItemName.TankUpgrade,
      Constants.ItemName.TankControllerUpgrade,
      Constants.ItemName.TractorBeamUpgrade,
      Constants.ItemName.LeashUpgrade)
    blacklistHost(classOf[internal.Robot],
      Constants.ItemName.LeashUpgrade)
    blacklistHost(classOf[internal.Tablet],
      Constants.ItemName.NetworkCard,
      Constants.ItemName.RedstoneCardTier1,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.ChunkloaderUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.DatabaseUpgradeTier1,
      Constants.ItemName.DatabaseUpgradeTier2,
      Constants.ItemName.DatabaseUpgradeTier3,
      Constants.ItemName.ExperienceUpgrade,
      Constants.ItemName.GeneratorUpgrade,
      Constants.ItemName.HoverUpgradeTier1,
      Constants.ItemName.HoverUpgradeTier2,
      Constants.ItemName.InventoryUpgrade,
      Constants.ItemName.InventoryControllerUpgrade,
      Constants.ItemName.TankUpgrade,
      Constants.ItemName.TankControllerUpgrade,
      Constants.ItemName.LeashUpgrade)

    if (!WirelessRedstone.isAvailable) {
      blacklistHost(classOf[internal.Drone], Constants.ItemName.RedstoneCardTier2)
      blacklistHost(classOf[internal.Tablet], Constants.ItemName.RedstoneCardTier2)
    }

    // Note: kinda nasty, but we have to check for availability for extended
    // redstone mods after integration init, so we have to set tier two
    // redstone card availability here, after all other mods were inited.
    if (BundledRedstone.isAvailable || WirelessRedstone.isAvailable) {
      OpenComputers.log.info("Found extended redstone mods, enabling tier two redstone card.")
      Delegator.subItem(api.Items.get(Constants.ItemName.RedstoneCardTier2).createItemStack(1)) match {
        case Some(redstone: RedstoneCard) => redstone.showInItemList = true
        case _ =>
      }
    }

    if (Settings.get.enableLua53 && LuaStateFactory.Lua53.isAvailable) {
      api.Machine.add(classOf[NativeLua53Architecture])
    }

    api.Manual.addProvider(DefinitionPathProvider)
    api.Manual.addProvider(new ResourceContentProvider(Settings.resourceDomain, "doc/"))
    api.Manual.addProvider("", TextureImageProvider)
    api.Manual.addProvider("item", ItemImageProvider)
    api.Manual.addProvider("block", BlockImageProvider)
    api.Manual.addProvider("oredict", OreDictImageProvider)

    api.Manual.addTab(new TextureTabIconRenderer(Textures.guiManualHome), "oc:gui.Manual.Home", "%LANGUAGE%/index.md")
    api.Manual.addTab(new ItemStackTabIconRenderer(api.Items.get("case1").createItemStack(1)), "oc:gui.Manual.Blocks", "%LANGUAGE%/block/index.md")
    api.Manual.addTab(new ItemStackTabIconRenderer(api.Items.get("cpu1").createItemStack(1)), "oc:gui.Manual.Items", "%LANGUAGE%/item/index.md")
  }

  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getCurrentEquippedItem.getItem match {
      case wrench: Wrench => wrench.useWrenchOnBlock(player, player.getEntityWorld, x, y, z, !changeDurability)
      case _ => false
    }
  }

  def canCharge(stack: ItemStack): Boolean = stack.getItem match {
    case chargeable: Chargeable => chargeable.canCharge(stack)
    case _ => false
  }

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    stack.getItem match {
      case chargeable: Chargeable => chargeable.charge(stack, amount, simulate)
      case _ => 0.0
    }
  }

  def inkCartridgeInkProvider(stack: ItemStack): Int = {
    if (api.Items.get(stack) == api.Items.get(Constants.ItemName.InkCartridge))
      Settings.get.printInkValue
    else
      0
  }

  def dyeInkProvider(stack: ItemStack): Int = {
    if (Color.isDye(stack))
      Settings.get.printInkValue / 10
    else
      0
  }

  private def blacklistHost(host: Class[_], itemNames: String*) {
    for (itemName <- itemNames) {
      api.IMC.blacklistHost(itemName, host, api.Items.get(itemName).createItemStack(1))
    }
  }

  object DefinitionPathProvider extends PathProvider {
    private final val Blacklist = Set(
      "debugger"
    )

    override def pathFor(stack: ItemStack): String = Option(api.Items.get(stack)) match {
      case Some(definition) => checkBlacklisted(definition)
      case _ => null
    }

    override def pathFor(world: World, x: Int, y: Int, z: Int): String = world.getBlock(x, y, z) match {
      case block: SimpleBlock => checkBlacklisted(api.Items.get(new ItemStack(block)))
      case _ => null
    }

    private def checkBlacklisted(info: ItemInfo): String =
      if (info == null || Blacklist.contains(info.name)) null
      else if (info.block != null) "%LANGUAGE%/block/" + info.name + ".md"
      else "%LANGUAGE%/item/" + info.name + ".md"
  }

}
