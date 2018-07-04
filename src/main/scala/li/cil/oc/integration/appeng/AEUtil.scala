package li.cil.oc.integration.appeng

import javax.annotation.Nonnull

import appeng.api.AEApi
import appeng.api.networking.IGrid
import appeng.api.networking.crafting.ICraftingGrid
import appeng.api.networking.energy.IEnergyGrid
import appeng.api.networking.storage.IStorageGrid
import appeng.api.storage.channels.{IFluidStorageChannel, IItemStorageChannel}
import appeng.api.storage.data.{IAEFluidStack, IAEItemStack}
import li.cil.oc.integration.Mods
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.versioning.VersionRange
import net.minecraftforge.fml.common.Loader

object AEUtil {
  val versionsWithNewItemDefinitionAPI = VersionRange.createFromVersionSpec("[rv4-alpha-1,)")

  val itemStorageChannel = AEApi.instance.storage.getStorageChannel[IAEItemStack, IItemStorageChannel](classOf[IItemStorageChannel])
  val fluidStorageChannel = AEApi.instance.storage.getStorageChannel[IAEFluidStack, IFluidStorageChannel](classOf[IFluidStorageChannel])

  def useNewItemDefinitionAPI = versionsWithNewItemDefinitionAPI.containsVersion(
    Loader.instance.getIndexedModList.get(Mods.AppliedEnergistics2.id).getProcessedVersion)

  // ----------------------------------------------------------------------- //

  def areChannelsEnabled: Boolean = AEApi.instance != null && AEApi.instance.definitions.blocks.controller.maybeStack(1).isPresent

  // ----------------------------------------------------------------------- //

  def controllerClass: Class[_] =
    if (AEApi.instance != null)
      if (areChannelsEnabled) AEApi.instance.definitions.blocks.controller.maybeEntity.get()
      else null: Class[_]
    else null

  // ----------------------------------------------------------------------- //

  def interfaceClass: Class[_] =
    if (AEApi.instance != null)
      if (areChannelsEnabled) AEApi.instance.definitions.blocks.iface.maybeEntity.get()
      else null: Class[_]
    else null

  // ----------------------------------------------------------------------- //

  def isController(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && areChannelsEnabled && AEApi.instance.definitions.blocks.controller.isSameAs(stack)

  // ----------------------------------------------------------------------- //

  def isExportBus(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && AEApi.instance.definitions.parts.exportBus.isSameAs(stack)

  // ----------------------------------------------------------------------- //

  def isImportBus(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && AEApi.instance.definitions.parts.importBus.isSameAs(stack)

  // ----------------------------------------------------------------------- //

  def isBlockInterface(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && AEApi.instance.definitions.blocks.iface.isSameAs(stack)

  // ----------------------------------------------------------------------- //

  def isPartInterface(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && AEApi.instance.definitions.parts.iface.isSameAs(stack)

  // ----------------------------------------------------------------------- //

  def getGridStorage(@Nonnull grid: IGrid): IStorageGrid = grid.getCache( classOf[IStorageGrid] )

  // ----------------------------------------------------------------------- //

  def getGridCrafting(@Nonnull grid: IGrid): ICraftingGrid = grid.getCache( classOf[ICraftingGrid] )

  // ----------------------------------------------------------------------- //

  def getGridEnergy(@Nonnull grid: IGrid): IEnergyGrid = grid.getCache( classOf[IEnergyGrid] )
}
