package li.cil.oc.integration.appeng

import appeng.api.AEApi
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.VersionRange
import li.cil.oc.integration.Mods
import net.minecraft.item.ItemStack

object AEUtil {
  val versionsWithNewItemDefinitionAPI = VersionRange.createFromVersionSpec("[rv2-beta-20,)")

  def useNewItemDefinitionAPI = versionsWithNewItemDefinitionAPI.containsVersion(
    Loader.instance.getIndexedModList.get(Mods.AppliedEnergistics2.id).getProcessedVersion)

  // ----------------------------------------------------------------------- //

  def areChannelsEnabled: Boolean = AEApi.instance != null && {
    if (useNewItemDefinitionAPI) areChannelsEnabledNew
    else areChannelsEnabledOld
  }

  private def areChannelsEnabledNew: Boolean = AEApi.instance.definitions.blocks.controller.maybeStack(1).isPresent

  private def areChannelsEnabledOld: Boolean = AEApi.instance.blocks != null && AEApi.instance.blocks.blockController != null && AEApi.instance.blocks.blockController.item != null

  // ----------------------------------------------------------------------- //

  def controllerClass: Class[_] =
    if (AEApi.instance != null) {
      if (AEUtil.useNewItemDefinitionAPI) controllerClassNew
      else controllerClassOld
    }
    else null

  private def controllerClassNew: Class[_] =
    if (areChannelsEnabled) AEApi.instance.definitions.blocks.controller.maybeEntity.orNull
    else null: Class[_] // ... why -.-

  private def controllerClassOld: Class[_] = {
    // Not classOf[TileController] because that derps the compiler when it tries to resolve the class (says can't find API classes from RotaryCraft).
    if (areChannelsEnabled) Class.forName("appeng.tile.networking.TileController")
    else null
  }

  // ----------------------------------------------------------------------- //

  def isController(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && {
    if (useNewItemDefinitionAPI) isControllerNew(stack)
    else isControllerOld(stack)
  }

  private def isControllerNew(stack: ItemStack): Boolean =
    areChannelsEnabled &&
      AEApi.instance.definitions.blocks.controller.isSameAs(stack)

  private def isControllerOld(stack: ItemStack): Boolean =
    areChannelsEnabled &&
      AEApi.instance.blocks != null &&
      AEApi.instance.blocks.blockController != null &&
      AEApi.instance.blocks.blockController.sameAsStack(stack)

  // ----------------------------------------------------------------------- //

  def isExportBus(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && {
    if (useNewItemDefinitionAPI) isExportBusNew(stack)
    else isExportBusOld(stack)
  }

  private def isExportBusNew(stack: ItemStack): Boolean =
    AEApi.instance.definitions.parts.exportBus.isSameAs(stack)

  private def isExportBusOld(stack: ItemStack): Boolean =
    AEApi.instance.parts != null &&
      AEApi.instance.parts.partExportBus != null &&
      AEApi.instance.parts.partExportBus.sameAsStack(stack)

  // ----------------------------------------------------------------------- //

  def isImportBus(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && {
    if (useNewItemDefinitionAPI) isImportBusNew(stack)
    else isImportBusOld(stack)
  }

  private def isImportBusNew(stack: ItemStack): Boolean =
    AEApi.instance.definitions.parts.importBus.isSameAs(stack)

  private def isImportBusOld(stack: ItemStack): Boolean =
    AEApi.instance.parts != null &&
      AEApi.instance.parts.partImportBus != null &&
      AEApi.instance.parts.partImportBus.sameAsStack(stack)

  // ----------------------------------------------------------------------- //

  def isBlockInterface(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && {
    if (useNewItemDefinitionAPI) isBlockInterfaceNew(stack)
    else isBlockInterfaceOld(stack)
  }

  private def isBlockInterfaceNew(stack: ItemStack): Boolean =
    AEApi.instance.definitions.blocks.iface.isSameAs(stack)

  private def isBlockInterfaceOld(stack: ItemStack): Boolean =
    AEApi.instance.blocks != null &&
      AEApi.instance.blocks.blockInterface != null &&
      AEApi.instance.blocks.blockInterface.sameAsStack(stack)

  // ----------------------------------------------------------------------- //

  def isPartInterface(stack: ItemStack): Boolean = stack != null && AEApi.instance != null && {
    if (useNewItemDefinitionAPI) isPartInterfaceNew(stack)
    else isPartInterfaceOld(stack)
  }

  private def isPartInterfaceNew(stack: ItemStack): Boolean =
    AEApi.instance.definitions.parts.iface.isSameAs(stack)

  private def isPartInterfaceOld(stack: ItemStack): Boolean =
    AEApi.instance.parts != null &&
      AEApi.instance.parts.partInterface != null &&
      AEApi.instance.parts.partInterface.sameAsStack(stack)
}
