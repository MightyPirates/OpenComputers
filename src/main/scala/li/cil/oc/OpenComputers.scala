package li.cil.oc

import li.cil.oc.common.IMC
import li.cil.oc.common.Proxy
import li.cil.oc.common.init.Blocks
import li.cil.oc.common.init.Items
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.block.Block
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.world.World
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.forgespi.Environment
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle._
import net.minecraftforge.fml.event.server._
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.network.simple.SimpleChannel
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import scala.collection.convert.ImplicitConversionsToScala._

@Mod.EventBusSubscriber(modid = OpenComputers.ID, bus = Bus.FORGE)
object OpenComputers {
  final val ID = "opencomputers"

  final val Name = "OpenComputers"

  final val log: Logger = LogManager.getLogger(Name)

  lazy val proxy: Proxy = {
    val cls = Environment.get.getDist match {
      case Dist.CLIENT => Class.forName("li.cil.oc.client.Proxy")
      case _ => Class.forName("li.cil.oc.common.Proxy")
    }
    cls.getConstructor().newInstance().asInstanceOf[Proxy]
  }

  var channel: SimpleChannel = null

  private var instance: Option[OpenComputers] = None

  def get = instance match {
    case Some(oc) => oc
    case _ => throw new IllegalStateException("not initialized")
  }

  @Deprecated
  def openGui(player: PlayerEntity, guiId: Int, world: World, x: Int, y: Int, z: Int): Unit = proxy.openGui(player, guiId, world, x, y, z)

  @SubscribeEvent
  def serverStart(e: FMLServerStartingEvent): Unit = {
    ThreadPoolFactory.safePools.foreach(_.newThreadPool())
  }

  @SubscribeEvent
  def serverStop(e: FMLServerStoppedEvent): Unit = {
    ThreadPoolFactory.safePools.foreach(_.waitForCompletion())
  }
}

@Mod(OpenComputers.ID)
class OpenComputers {
  val modContainer: ModContainer = ModLoadingContext.get.getActiveContainer

  val Version = modContainer.getModInfo.getVersion

  FMLJavaModLoadingContext.get.getModEventBus.register(this)
  OpenComputers.instance = Some(this)

  @SubscribeEvent
  def registerBlocks(e: RegistryEvent.Register[Block]) {
    Blocks.init()
  }

  @SubscribeEvent
  def registerItems(e: RegistryEvent.Register[Item]) {
    Items.init()
    OpenComputers.proxy.initExtraTags()
  }

  @SubscribeEvent
  def commonInit(e: FMLCommonSetupEvent): Unit = {
    OpenComputers.proxy.preInit(e)
    OpenComputers.proxy.init(e)
  }

  @SubscribeEvent
  def imc(e: InterModProcessEvent): Unit = InterModComms.getMessages(OpenComputers.ID).sequential.iterator.foreach(IMC.handleMessage)

  @SubscribeEvent
  def loadComplete(e: FMLLoadCompleteEvent): Unit = OpenComputers.proxy.postInit(e)
}
