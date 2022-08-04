package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.inventory.container.PlayerContainer
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import scala.collection.mutable

@Mod.EventBusSubscriber(value = Array(Dist.CLIENT), modid = OpenComputers.ID, bus = Bus.MOD)
object Textures {

  object Font extends TextureBundle {
    val Aliased = L("chars_aliased")
    val AntiAliased = L("chars")

    override protected def basePath = "font/%s"

    override protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation) =
      Minecraft.getInstance.textureManager.register(loc, new SimpleTexture(new ResourceLocation(loc.getNamespace, s"textures/${loc.getPath}.png")))
  }

  object GUI extends TextureBundle {
    val Background = L("background")
    val Bar = L("bar")
    val Borders = L("borders")
    val ButtonDriveMode = L("button_drive_mode")
    val ButtonPower = L("button_power")
    val ButtonRange = L("button_range")
    val ButtonRun = L("button_run")
    val ButtonScroll = L("button_scroll")
    val ButtonSide = L("button_side")
    val ButtonRelay = L("button_relay")
    val Computer = L("computer")
    val Database = L("database")
    val Database1 = L("database1")
    val Database2 = L("database2")
    val Disassembler = L("disassembler")
    val Drive = L("drive")
    val Drone = L("drone")
    val KeyboardMissing = L("keyboard_missing")
    val Manual = L("manual")
    val ManualHome = L("manual_home")
    val ManualMissingItem = L("manual_missing_item")
    val ManualTab = L("manual_tab")
    val Nanomachines = L("nanomachines_power")
    val NanomachinesBar = L("nanomachines_power_bar")
    val Printer = L("printer")
    val PrinterInk = L("printer_ink")
    val PrinterMaterial = L("printer_material")
    val PrinterProgress = L("printer_progress")
    val Rack = L("rack")
    val Raid = L("raid")
    val Range = L("range")
    val Robot = L("robot")
    val RobotAssembler = L("robot_assembler")
    val RobotNoScreen = L("robot_noscreen")
    val RobotSelection = L("robot_selection")
    val Server = L("server")
    val Slot = L("slot")
    val UpgradeTab = L("upgrade_tab")
    val Waypoint = L("waypoint")

    override protected def basePath = "gui/%s"

    override protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation) =
      Minecraft.getInstance.textureManager.register(loc, new SimpleTexture(new ResourceLocation(loc.getNamespace, s"textures/${loc.getPath}.png")))
  }

  object Icons extends TextureBundle {
    private val ForSlotType = Slot.All.map(name => name -> L(name)).toMap
    private val ForTier = Map(Tier.None -> L("na")) ++ (Tier.One to Tier.Three).map(tier => tier -> L("tier" + tier)).toMap

    def get(slotType: String) = ForSlotType.get(slotType).orNull

    def get(tier: Int) = ForTier.get(tier).orNull

    override protected def basePath = "icons/%s"

    override protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation) =
      Minecraft.getInstance.textureManager.register(loc, new SimpleTexture(new ResourceLocation(loc.getNamespace, s"textures/${loc.getPath}.png")))
  }

  object Model extends TextureBundle {
    val UpgradeCrafting = L("crafting_upgrade")
    val UpgradeGenerator = L("generator_upgrade")
    val UpgradeInventory = L("inventory_upgrade")
    val HologramEffect = L("hologram_effect")
    val Drone = L("drone")
    val Robot = L("robot")

    override protected def basePath = "model/%s"

    override protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation) =
      Minecraft.getInstance.textureManager.register(loc, new SimpleTexture(new ResourceLocation(loc.getNamespace, s"textures/${loc.getPath}.png")))
  }

  object Item extends TextureBundle {
    val DroneItem = L("drone")
    val Robot = L("robot")

    override protected def basePath = "items/%s"

    override protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation) = e.addSprite(loc)
  }

  // These are kept in the block texture atlas to support animations.
  object Block extends TextureBundle {
    val AdapterOn = L("overlay/adapter_on")
    val AssemblerSideAssembling = L("overlay/assembler_side_assembling")
    val AssemblerSideOn = L("overlay/assembler_side_on")
    val AssemblerTopOn = L("overlay/assembler_top_on")
    val CaseFrontActivity = L("overlay/case_front_activity")
    val CaseFrontError = L("overlay/case_front_error")
    val CaseFrontOn = L("overlay/case_front_on")
    val ChargerFrontOn = L("overlay/charger_front_on")
    val ChargerSideOn = L("overlay/charger_side_on")
    val DisassemblerSideOn = L("overlay/disassembler_side_on")
    val DisassemblerTopOn = L("overlay/disassembler_top_on")
    val DiskDriveFrontActivity = L("overlay/diskdrive_front_activity")
    val GeolyzerTopOn = L("overlay/geolyzer_top_on")
    val MicrocontrollerFrontLight = L("overlay/microcontroller_front_light")
    val MicrocontrollerFrontOn = L("overlay/microcontroller_front_on")
    val MicrocontrollerFrontError = L("overlay/microcontroller_front_error")
    val NetSplitterOn = L("overlay/netsplitter_on")
    val PowerDistributorSideOn = L("overlay/powerdistributor_side_on")
    val PowerDistributorTopOn = L("overlay/powerdistributor_top_on")
    val RackDiskDrive = L("rack_disk_drive")
    val RackDiskDriveActivity = L("overlay/rack_disk_drive_activity")
    val RackServer = L("rack_server")
    val RackServerActivity = L("overlay/rack_server_activity")
    val RackServerOn = L("overlay/rack_server_on")
    val RackServerError = L("overlay/rack_server_error")
    val RackServerNetworkActivity = L("overlay/rack_server_network_activity")
    val RackTerminalServer = L("rack_terminal_server")
    val RackTerminalServerOn = L("overlay/rack_terminal_server_on")
    val RackTerminalServerPresence = L("overlay/rack_terminal_server_presence")
    val RaidFrontActivity = L("overlay/raid_front_activity")
    val RaidFrontError = L("overlay/raid_front_error")
    val ScreenUpIndicator = L("overlay/screen_up_indicator")
    val SwitchSideOn = L("overlay/switch_side_on")
    val TransposerOn = L("overlay/transposer_on")

    val Cable = L("cable")
    val CableCap = L("cablecap")
    val GenericTop = L("generic_top", load = false)
    val NetSplitterSide = L("netsplitter_side")
    val NetSplitterTop = L("netsplitter_top")
    val RackFront = L("rack_front", load = false)
    val RackSide = L("rack_side", load = false)

    // Kill me now.
    object Screen {
      val Single = Array(
        L("screen/b"),
        L("screen/b"),
        L("screen/b2"),
        L("screen/b2"),
        L("screen/b2"),
        L("screen/b2")
      )

      val SingleFront = Array(
        L("screen/f"),
        L("screen/f2")
      )

      val Horizontal = Array(
        // Vertical.
        Array(
          Array(
            L("screen/bht"),
            L("screen/bhb"),
            L("screen/bht2"),
            L("screen/bht2"),
            L("screen/b2"),
            L("screen/b2")
          ),
          Array(
            L("screen/bhm"),
            L("screen/bhm"),
            L("screen/bhm2"),
            L("screen/bhm2"),
            L("screen/b"), // Not rendered.
            L("screen/b") // Not rendered.
          ),
          Array(
            L("screen/bhb"),
            L("screen/bht"),
            L("screen/bhb2"),
            L("screen/bhb2"),
            L("screen/b2"),
            L("screen/b2")
          )
        ),
        // Horizontal.
        Array(
          Array(
            L("screen/bhb2"),
            L("screen/bht2"),
            L("screen/bht"),
            L("screen/bhb"),
            L("screen/b2"),
            L("screen/b2")
          ),
          Array(
            L("screen/bhm2"),
            L("screen/bhm2"),
            L("screen/bhm"),
            L("screen/bhm"),
            L("screen/b"), // Not rendered.
            L("screen/b") // Not rendered.
          ),
          Array(
            L("screen/bht2"),
            L("screen/bhb2"),
            L("screen/bhb"),
            L("screen/bht"),
            L("screen/b2"),
            L("screen/b2")
          )
        )
      )

      val HorizontalFront = Array(
        // Vertical.
        Array(
          L("screen/fhb2"),
          L("screen/fhm2"),
          L("screen/fht2")
        ),
        // Horizontal.
        Array(
          L("screen/fhb"),
          L("screen/fhm"),
          L("screen/fht")
        )
      )

      val Vertical = Array(
        // Vertical.
        Array(
          Array(
            L("screen/b"),
            L("screen/b"),
            L("screen/bvt"),
            L("screen/bvt"),
            L("screen/bvt"),
            L("screen/bvt")
          ),
          Array(
            L("screen/b"), // Not rendered.
            L("screen/b"), // Not rendered.
            L("screen/bvm"),
            L("screen/bvm"),
            L("screen/bvm"),
            L("screen/bvm")
          ),
          Array(
            L("screen/b"),
            L("screen/b"),
            L("screen/bvb2"),
            L("screen/bvb2"),
            L("screen/bvb2"),
            L("screen/bvb2")
          )
        ),
        // Horizontal.
        Array(
          Array(
            L("screen/b2"),
            L("screen/b2"),
            L("screen/bvt"),
            L("screen/bvt"),
            L("screen/bht2"),
            L("screen/bhb2")
          ),
          Array(
            L("screen/b"), // Not rendered.
            L("screen/b"), // Not rendered.
            L("screen/bvm"),
            L("screen/bvm"),
            L("screen/bhm2"),
            L("screen/bhm2")
          ),
          Array(
            L("screen/b2"),
            L("screen/b2"),
            L("screen/bvb"),
            L("screen/bvb"),
            L("screen/bhb2"),
            L("screen/bht2")
          )
        )
      )

      val VerticalFront = Array(
        // Vertical.
        Array(
          L("screen/fvt"),
          L("screen/fvm"),
          L("screen/fvb2")
        ),
        // Horizontal.
        Array(
          L("screen/fvt"),
          L("screen/fvm"),
          L("screen/fvb")
        )
      )

      val Multi = Array(
        // Vertical.
        Array(
          // Top.
          Array(
            Array(
              L("screen/bht"),
              L("screen/bhb"),
              L("screen/btl"),
              L("screen/btr"),
              L("screen/bvb"),
              L("screen/bvt")
            ),
            Array(
              L("screen/bhm"),
              L("screen/bhm"),
              L("screen/btm"),
              L("screen/btm"),
              L("screen/b"), // Not rendered.
              L("screen/b") // Not rendered.
            ),
            Array(
              L("screen/bhb"),
              L("screen/bht"),
              L("screen/btr"),
              L("screen/btl"),
              L("screen/bvt"),
              L("screen/bvb")
            )
          ),
          // Middle.
          Array(
            Array(
              L("screen/b"), // Not rendered.
              L("screen/b"), // Not rendered.
              L("screen/bml"),
              L("screen/bmr"),
              L("screen/bvm"),
              L("screen/bvm")
            ),
            Array(
              L("screen/b"), // Not rendered.
              L("screen/b"), // Not rendered.
              L("screen/bmm"),
              L("screen/bmm"),
              L("screen/b"), // Not rendered.
              L("screen/b") // Not rendered.
            ),
            Array(
              L("screen/b"), // Not rendered.
              L("screen/b"), // Not rendered.
              L("screen/bmr"),
              L("screen/bml"),
              L("screen/bvm"),
              L("screen/bvt")
            )
          ),
          // Bottom.
          Array(
            Array(
              L("screen/bht"),
              L("screen/bhb"),
              L("screen/bbl2"),
              L("screen/bbr2"),
              L("screen/bvt"),
              L("screen/bvb2")
            ),
            Array(
              L("screen/bhm"),
              L("screen/bhm"),
              L("screen/bbm2"),
              L("screen/bbm2"),
              L("screen/b"), // Not rendered.
              L("screen/b") // Not rendered.
            ),
            Array(
              L("screen/bhb"),
              L("screen/bht"),
              L("screen/bbr2"),
              L("screen/bbl2"),
              L("screen/bvb2"),
              L("screen/bvt")
            )
          )
        ),
        // Horizontal.
        Array(
          // Top.
          Array(
            Array(
              L("screen/bhb2"),
              L("screen/bht2"),
              L("screen/btl"),
              L("screen/btr"),
              L("screen/bht2"),
              L("screen/bhb2")
            ),
            Array(
              L("screen/bhm2"),
              L("screen/bhm2"),
              L("screen/btm"),
              L("screen/btm"),
              L("screen/b"), // Not rendered.
              L("screen/b") // Not rendered.
            ),
            Array(
              L("screen/bht2"),
              L("screen/bhb2"),
              L("screen/btr"),
              L("screen/btl"),
              L("screen/bht2"),
              L("screen/bhb2")
            )
          ),
          // Middle.
          Array(
            Array(
              L("screen/b"), // Not rendered.
              L("screen/b"), // Not rendered.
              L("screen/bml"),
              L("screen/bml"),
              L("screen/bhm2"),
              L("screen/bhm2")
            ),
            Array(
              L("screen/b"), // Not rendered.
              L("screen/b"), // Not rendered.
              L("screen/bmm"),
              L("screen/bmm"),
              L("screen/b"), // Not rendered.
              L("screen/b") // Not rendered.
            ),
            Array(
              L("screen/b"), // Not rendered.
              L("screen/b"), // Not rendered.
              L("screen/bmr"),
              L("screen/bmr"),
              L("screen/bhm2"),
              L("screen/bhm2")
            )
          ),
          // Bottom.
          Array(
            Array(
              L("screen/bhb2"),
              L("screen/bht2"),
              L("screen/bbl"),
              L("screen/bbr"),
              L("screen/bhb2"),
              L("screen/bht2")
            ),
            Array(
              L("screen/bhm2"),
              L("screen/bhm2"),
              L("screen/bbm"),
              L("screen/bbm"),
              L("screen/b"), // Not rendered.
              L("screen/b") // Not rendered.
            ),
            Array(
              L("screen/bht2"),
              L("screen/bhb2"),
              L("screen/bbr"),
              L("screen/bbl"),
              L("screen/bhb2"),
              L("screen/bht2")
            )
          )
        )
      )

      val MultiFront = Array(
        // Vertical.
        Array(
          Array(
            L("screen/ftr"),
            L("screen/ftm"),
            L("screen/ftl")
          ),
          Array(
            L("screen/fmr"),
            L("screen/fmm"),
            L("screen/fml")
          ),
          Array(
            L("screen/fbr2"),
            L("screen/fbm2"),
            L("screen/fbl2")
          )
        ),
        // Horizontal.
        Array(
          Array(
            L("screen/ftr"),
            L("screen/ftm"),
            L("screen/ftl")
          ),
          Array(
            L("screen/fmr"),
            L("screen/fmm"),
            L("screen/fml")
          ),
          Array(
            L("screen/fbr"),
            L("screen/fbm"),
            L("screen/fbl")
          )
        )
      )

      // The hacks I do for namespacing...
      private[Block] def makeSureThisIsInitialized() {}
    }

    Screen.makeSureThisIsInitialized()

    def bind(): Unit = Textures.bind(PlayerContainer.BLOCK_ATLAS)

    override protected def basePath = "blocks/%s"

    override protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation) = e.addSprite(loc)
  }

  def bind(location: ResourceLocation): Unit = {
    val texture = if (location != null) Minecraft.getInstance.textureManager.getTexture(location) else null
    if (texture != null) texture.bind()
    else RenderState.bindTexture(0)
  }

  def getSprite(location: String): TextureAtlasSprite = getSprite(new ResourceLocation(location))

  def getSprite(location: ResourceLocation): TextureAtlasSprite =
    Minecraft.getInstance.getModelManager.getAtlas(PlayerContainer.BLOCK_ATLAS).getSprite(location)

  @SubscribeEvent
  def onTextureStitchPre(e: TextureStitchEvent.Pre): Unit = {
    if (e.getMap.location.equals(PlayerContainer.BLOCK_ATLAS)) {
      Font.init(e)
      GUI.init(e)
      Icons.init(e)
      Model.init(e)
      Item.init(e)
      Block.init(e)
    }
  }

  abstract class TextureBundle {
    private val locations = mutable.ArrayBuffer.empty[ResourceLocation]

    final def init(e: TextureStitchEvent.Pre): Unit = {
      locations.foreach(loader(e, _))
    }

    protected def L(name: String, load: Boolean = true) = {
      val location = new ResourceLocation(OpenComputers.ID, String.format(basePath, name))
      if (load) locations += location
      location
    }

    protected def basePath: String

    protected def loader(e: TextureStitchEvent.Pre, loc: ResourceLocation): Unit
  }

}
