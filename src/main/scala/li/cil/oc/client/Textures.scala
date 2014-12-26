package li.cil.oc.client

import li.cil.oc.Settings
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Textures {
  val fontAntiAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars.png")
  val fontAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars_aliased.png")

  object GUI {
    private def L(name: String) = new ResourceLocation(Settings.resourceDomain, "textures/gui/" + name + ".png")

    val Background = L("background")
    val Bar = L("bar")
    val Borders = L("borders")
    val ButtonPower = L("button_power")
    val ButtonRange = L("button_range")
    val ButtonRun = L("button_run")
    val ButtonScroll = L("button_scroll")
    val ButtonSide = L("button_side")
    val ButtonSwitch = L("button_switch")
    val Computer = L("computer")
    val Database = L("database")
    val Database1 = L("database1")
    val Database2 = L("database2")
    val Disassembler = L("disassembler")
    val Drone = L("drone")
    val KeyboardMissing = L("keyboard_missing")
    val Raid = L("raid")
    val Range = L("range")
    val Robot = L("robot")
    val RobotAssembler = L("robot_assembler")
    val RobotNoScreen = L("robot_noscreen")
    val RobotSelection = L("robot_selection")
    val Server = L("server")
    val Slot = L("slot")

    def init(tm: TextureManager): Unit = {
      tm.bindTexture(Background)
      tm.bindTexture(Bar)
      tm.bindTexture(Borders)
      tm.bindTexture(ButtonPower)
      tm.bindTexture(ButtonRange)
      tm.bindTexture(ButtonRun)
      tm.bindTexture(ButtonScroll)
      tm.bindTexture(ButtonSide)
      tm.bindTexture(ButtonSwitch)
      tm.bindTexture(Computer)
      tm.bindTexture(Database)
      tm.bindTexture(Database1)
      tm.bindTexture(Database2)
      tm.bindTexture(Disassembler)
      tm.bindTexture(Drone)
      tm.bindTexture(KeyboardMissing)
      tm.bindTexture(Raid)
      tm.bindTexture(Range)
      tm.bindTexture(Robot)
      tm.bindTexture(RobotAssembler)
      tm.bindTexture(RobotNoScreen)
      tm.bindTexture(RobotSelection)
      tm.bindTexture(Server)
      tm.bindTexture(Slot)
    }
  }

  object Icons {
    private def L(name: String) = new ResourceLocation(Settings.resourceDomain, "textures/icons/" + name + ".png")

    private val ForSlotType = Slot.All.map(name => name -> L(name)).toMap
    private val ForTier = Map(Tier.None -> L("na")) ++ (Tier.One to Tier.Three).map(tier => tier -> L("tier" + tier)).toMap

    def init(tm: TextureManager): Unit = {
      for ((_, icon) <- ForSlotType) tm.bindTexture(icon)
      for ((_, icon) <- ForTier) tm.bindTexture(icon)
    }

    def get(slotType: String) = ForSlotType.get(slotType).orNull

    def get(tier: Int) = ForTier.get(tier).orNull
  }

  object Model {
    private def L(name: String) = new ResourceLocation(Settings.resourceDomain, "textures/model/" + name + ".png")

    val HologramEffect = L("HologramEffect")
    val Robot = L("robot")
    val UpgradeCrafting = L("UpgradeCrafting")
    val UpgradeGenerator = L("UpgradeGenerator")
    val UpgradeInventory = L("UpgradeInventory")

    def init(tm: TextureManager): Unit = {
      tm.bindTexture(HologramEffect)
      tm.bindTexture(Robot)
      tm.bindTexture(UpgradeCrafting)
      tm.bindTexture(UpgradeGenerator)
      tm.bindTexture(UpgradeInventory)
    }
  }

  // These are kept in the block texture atlas to support animations.
  object Block {
    private def L(name: String) = new ResourceLocation(Settings.resourceDomain, "blocks/" + name)

    val AssemblerSideAssembling = L("AssemblerSideAssembling")
    val AssemblerSideOn = L("AssemblerSideOn")
    val AssemblerTopOn = L("AssemblerTopOn")
    val CableCap = L("CableCap")
    val CaseFrontActivity = L("CaseFrontActivity")
    val CaseFrontOn = L("CaseFrontOn")
    val ChargerFrontOn = L("ChargerFrontOn")
    val ChargerSideOn = L("ChargerSideOn")
    val DisassemblerSideOn = L("DisassemblerSideOn")
    val DisassemblerTopOn = L("DisassemblerTopOn")
    val DiskDriveFrontActivity = L("DiskDriveFrontActivity")
    val GeolyzerTopOn = L("GeolyzerTopOn")
    val MicrocontrollerFrontLight = L("MicrocontrollerFrontLight")
    val MicrocontrollerFrontOn = L("MicrocontrollerFrontOn")
    val PowerDistributorSideOn = L("PowerDistributorSideOn")
    val PowerDistributorTopOn = L("PowerDistributorTopOn")
    val RackFrontActivity = L("ServerRackFrontActivity")
    val RackFrontOn = L("ServerRackFrontOn")
    val RaidFrontActivity = L("RaidFrontActivity")
    val RaidFrontError = L("RaidFrontError")
    val ScreenUpIndicator = L("screen/up_indicator")
    val SwitchSideOn = L("SwitchSideOn")

    def init(map: TextureMap): Unit = {
      map.registerSprite(AssemblerSideAssembling)
      map.registerSprite(AssemblerSideOn)
      map.registerSprite(AssemblerTopOn)
      map.registerSprite(CableCap)
      map.registerSprite(CaseFrontActivity)
      map.registerSprite(CaseFrontOn)
      map.registerSprite(ChargerFrontOn)
      map.registerSprite(ChargerSideOn)
      map.registerSprite(DisassemblerSideOn)
      map.registerSprite(DisassemblerTopOn)
      map.registerSprite(DiskDriveFrontActivity)
      map.registerSprite(GeolyzerTopOn)
      map.registerSprite(MicrocontrollerFrontLight)
      map.registerSprite(MicrocontrollerFrontOn)
      map.registerSprite(PowerDistributorSideOn)
      map.registerSprite(PowerDistributorTopOn)
      map.registerSprite(RackFrontActivity)
      map.registerSprite(RackFrontOn)
      map.registerSprite(RaidFrontActivity)
      map.registerSprite(RaidFrontError)
      map.registerSprite(ScreenUpIndicator)
      map.registerSprite(SwitchSideOn)
    }

    def bind(): Unit = Minecraft.getMinecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture)

    def unbind(): Unit = GlStateManager.bindTexture(0)

    def getSprite(location: ResourceLocation) = Minecraft.getMinecraft.getTextureMapBlocks.getAtlasSprite(location.toString)
  }

  @SubscribeEvent
  def onTextureLoad(e: TextureStitchEvent.Pre): Unit = {
    val tm = Minecraft.getMinecraft.getTextureManager
    tm.bindTexture(fontAntiAliased)
    tm.bindTexture(fontAliased)

    GUI.init(tm)
    Icons.init(tm)
    Model.init(tm)
    Block.init(e.map)
  }
}
