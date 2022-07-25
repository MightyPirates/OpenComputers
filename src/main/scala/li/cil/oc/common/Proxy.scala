package li.cil.oc.common

import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

import com.google.common.base.Strings
import li.cil.oc._
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.init.Blocks
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.integration.Mods
import li.cil.oc.server
import li.cil.oc.server._
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.server.machine.luac.NativeLua52Architecture
import li.cil.oc.server.machine.luac.NativeLua53Architecture
import li.cil.oc.server.machine.luaj.LuaJLuaArchitecture
import net.minecraft.block.Block
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketBuffer
import net.minecraft.tags.ItemTags
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.RegistryEvent.MissingMappings
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle._
import net.minecraftforge.fml.network.NetworkEvent
import net.minecraftforge.fml.network.NetworkRegistry
import net.minecraftforge.registries.ForgeRegistries

import scala.collection.convert.ImplicitConversionsToScala._
import scala.reflect.ClassTag

@Deprecated
class Proxy {
  @Deprecated
  def initExtraTags() {
    OpenComputers.log.debug("Initializing additional OreDict entries.")

    tryRegisterNugget[item.DiamondChip](Constants.ItemName.DiamondChip, OpenComputers.ID + ":chip_diamond", net.minecraft.item.Items.DIAMOND, "forge:gems/diamond")
  }

  def preInit(e: FMLCommonSetupEvent) {
    OpenComputers.log.info("Initializing OpenComputers API.")

    api.CreativeTab.instance = CreativeTab
    api.API.driver = driver.Registry
    api.API.fileSystem = fs.FileSystem
    api.API.items = Items
    api.API.machine = machine.Machine
    api.API.nanomachines = nanomachines.Nanomachines
    api.API.network = network.Network

    api.API.config = Settings.get.config

    // Weird JNLua bug identified
    // When loading JNLua (for either 5.2 or 5.3 lua state) there is a static section that the library loads
    // being static, it loads once regardless of which lua state is loaded first
    // static { REGISTRYINDEX = lua_registryindex(); }
    // The problem is that lua_registryindex was removed in 5.3
    // Thus, if we load JNLua from a lua5.3 state first, this static section fails
    // We must load 5.2 first, AND we know 5.3 will likely fail to load if 5.2 failed
    val include52: Boolean = LuaStateFactory.include52
    // now that JNLua has been initialized from a lua52 state, we are safe to check 5.3
    if (LuaStateFactory.include53) {
      api.Machine.add(classOf[NativeLua53Architecture])
    }
    if (include52) {
      api.Machine.add(classOf[NativeLua52Architecture])
    }
    if (LuaStateFactory.includeLuaJ) {
      api.Machine.add(classOf[LuaJLuaArchitecture])
    }
    
    api.Machine.LuaArchitecture =
      if (Settings.get.forceLuaJ) classOf[LuaJLuaArchitecture]
      else api.Machine.architectures.head
  }

  def init(e: FMLCommonSetupEvent) {
    OpenComputers.channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(OpenComputers.ID, "net_main"), new Supplier[String] {
      override def get = ""
    }, new Predicate[String] {
      override def test(ver: String) = "".equals(ver)
    }, new Predicate[String] {
      override def test(ver: String) = "".equals(ver)
    })
    OpenComputers.channel.registerMessage(0, classOf[Array[Byte]], new BiConsumer[Array[Byte], PacketBuffer] {
      override def accept(msg: Array[Byte], buff: PacketBuffer) = buff.writeByteArray(msg)
    }, new Function[PacketBuffer, Array[Byte]] {
      override def apply(buff: PacketBuffer) = buff.readByteArray()
    }, new BiConsumer[Array[Byte], Supplier[NetworkEvent.Context]] {
      override def accept(msg: Array[Byte], ctx: Supplier[NetworkEvent.Context]) = {
        val context = ctx.get
        context.enqueueWork(new Runnable {
          override def run = PacketHandler.handlePacket(context.getDirection, msg, context.getSender)
        })
        context.setPacketHandled(true)
      }
    })
    PacketHandler.serverHandler = server.PacketHandler

    Loot.init()
    Achievement.init()

    OpenComputers.log.debug("Initializing mod integration.")
    Mods.init()

    OpenComputers.log.info("Initializing capabilities.")
    Capabilities.init()

    api.API.isPowerEnabled = !Settings.get.ignorePower
  }

  def postInit(e: FMLLoadCompleteEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }

  def tryRegisterNugget[TItem <: Delegate : ClassTag](nuggetItemName: String, nuggetOredictName: String, ingotItem: Item, ingotOredictName: String): Unit = {
    val nugget = Items.get(nuggetItemName).createItemStack(1)

    Delegator.subItem(nugget) match {
      case Some(subItem: TItem) =>
        if (ItemTags.getAllTags.getTagOrEmpty(new ResourceLocation(nuggetOredictName)).contains(nugget.getItem)) {
          Items.registerItem(subItem, nuggetItemName)
          Items.registerItem(ingotItem, ingotOredictName)
        }
        else {
          subItem.showInItemList = false
        }
      case _ =>
    }
  }

  def getGuiHandler(): common.GuiHandler = server.GuiHandler

  @Deprecated
  def openGui(player: PlayerEntity, guiId: Int, world: World, x: Int, y: Int, z: Int): Unit = {
    player match {
      case _: FakePlayer => {} // Ignore fake players.
      case _: ServerPlayerEntity => {
        player.openMenu(new INamedContainerProvider {
          override def createMenu(id: Int, plrInv: PlayerInventory, plr: PlayerEntity): Container =
            getGuiHandler.getServerGuiElement(guiId, id, plr, plr.level, x, y, z).asInstanceOf[Container]

          override def getDisplayName(): ITextComponent = StringTextComponent.EMPTY
        })
      }
      case _ => OpenComputers.log.error(s"Unsupported entity for openGui: ${player.getClass.getName}")
    }
  }

  def registerModel(instance: Delegate, id: String): Unit = {}

  def registerModel(instance: Item, id: String): Unit = {}

  def registerModel(instance: Block, id: String): Unit = {}

  // Yes, this could be boiled down even further, but I like to keep it
  // explicit like this, because it makes it a) clearer, b) easier to
  // extend, in case that should ever be needed.

  // Example usage: OpenComputers.ID + ":rack" -> "serverRack"
  private val blockRenames = Map[String, String](
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack // Yay, full circle >_>
  )

  // Example usage: OpenComputers.ID + ":tabletCase" -> "tabletCase1"
  private val itemRenames = Map[String, String](
    OpenComputers.ID + ":dataCard" -> Constants.ItemName.DataCardTier1,
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack,
    OpenComputers.ID + ":wlanCard" -> Constants.ItemName.WirelessNetworkCardTier2
  )

  @SubscribeEvent
  def missingBlockMappings(e: MissingMappings[Block]) {
    for (missing <- e.getMappings(OpenComputers.ID)) {
        blockRenames.get(missing.key.getPath) match {
          case Some(name) =>
            if (Strings.isNullOrEmpty(name)) missing.ignore()
            else missing.remap(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(OpenComputers.ID, name)))
          case _ => missing.warn()
        }
    }
  }

  @SubscribeEvent
  def missingItemMappings(e: MissingMappings[Item]) {
    for (missing <- e.getMappings(OpenComputers.ID)) {
        itemRenames.get(missing.key.getPath) match {
          case Some(name) =>
            if (Strings.isNullOrEmpty(name)) missing.ignore()
            else missing.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(OpenComputers.ID, name)))
          case _ => missing.warn()
        }
      }
  }
}
