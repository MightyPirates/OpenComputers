package li.cil.oc.integration.appeng

import java.util.Optional
import javax.annotation.Nonnull

import appeng.api._
import appeng.api.networking.IGrid
import appeng.api.networking.crafting.ICraftingGrid
import appeng.api.networking.energy.IEnergyGrid
import appeng.api.networking.storage.IStorageGrid
import appeng.api.storage.channels.{IFluidStorageChannel, IItemStorageChannel}
import appeng.api.storage.data.{IAEFluidStack, IAEItemStack}
import appeng.api.storage.IStorageHelper
import li.cil.oc.integration.Mods
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.ModList
import net.minecraftforge.forgespi.language.MavenVersionAdapter
import org.apache.maven.artifact.versioning.VersionRange

@AEAddon
class AEUtil extends IAEAddon {
  override def onAPIAvailable(aeApi: IAppEngApi) = AEUtil.onAPIAvailable(aeApi)
}

object AEUtil {
  var aeApi: Option[IAppEngApi] = None

  private def optionToScala[T](opt: Optional[T]): Option[T] = if (opt.isPresent) Some(opt.get) else None

  private def onAPIAvailable(aeApi: IAppEngApi) {
    AEUtil.aeApi = Some(aeApi)
    itemStorageChannel = aeApi.storage.getStorageChannel[IAEItemStack, IItemStorageChannel](classOf[IItemStorageChannel])
    fluidStorageChannel = aeApi.storage.getStorageChannel[IAEFluidStack, IFluidStorageChannel](classOf[IFluidStorageChannel])
  }

  val versionsWithNewItemDefinitionAPI: VersionRange = MavenVersionAdapter.createFromVersionSpec("[rv6-stable-5,)")

  var itemStorageChannel: IItemStorageChannel = null
  var fluidStorageChannel: IFluidStorageChannel = null

  def useNewItemDefinitionAPI: Boolean = optionToScala(ModList.get.getModContainerById(Mods.AppliedEnergistics2.id)).
    filter(container => versionsWithNewItemDefinitionAPI.containsVersion(container.getModInfo.getVersion)).isDefined

  // ----------------------------------------------------------------------- //

  def controllerClass: Class[_] = aeApi.flatMap(api => optionToScala(api.definitions.blocks.controller.maybeEntity)).orNull

  // ----------------------------------------------------------------------- //

  def interfaceClass: Class[_] = aeApi.map(api => api.definitions.blocks.iface.maybeEntity.get).orNull

  // ----------------------------------------------------------------------- //

  def isController(stack: ItemStack): Boolean = stack != null && aeApi.filter(_.definitions.blocks.controller.isSameAs(stack)).isDefined

  // ----------------------------------------------------------------------- //

  def isExportBus(stack: ItemStack): Boolean = stack != null && aeApi.filter(_.definitions.parts.exportBus.isSameAs(stack)).isDefined

  // ----------------------------------------------------------------------- //

  def isImportBus(stack: ItemStack): Boolean = stack != null && aeApi.filter(_.definitions.parts.importBus.isSameAs(stack)).isDefined

  // ----------------------------------------------------------------------- //

  def isBlockInterface(stack: ItemStack): Boolean = stack != null && aeApi.filter(_.definitions.blocks.iface.isSameAs(stack)).isDefined

  // ----------------------------------------------------------------------- //

  def isPartInterface(stack: ItemStack): Boolean = stack != null && aeApi.filter(_.definitions.parts.iface.isSameAs(stack)).isDefined

  // ----------------------------------------------------------------------- //

  def getGridStorage(@Nonnull grid: IGrid): IStorageGrid = grid.getCache( classOf[IStorageGrid] )

  // ----------------------------------------------------------------------- //

  def getGridCrafting(@Nonnull grid: IGrid): ICraftingGrid = grid.getCache( classOf[ICraftingGrid] )

  // ----------------------------------------------------------------------- //

  def getGridEnergy(@Nonnull grid: IGrid): IEnergyGrid = grid.getCache( classOf[IEnergyGrid] )
}
