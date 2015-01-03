package li.cil.oc.common.block

import java.util

import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Microcontroller extends RedstoneAware with traits.PowerAcceptor {
  setCreativeTab(null)
  NEI.hide(this)

  override protected def customTextures = Array(
    Some("MicrocontrollerTop"),
    Some("MicrocontrollerTop"),
    Some("MicrocontrollerSide"),
    Some("MicrocontrollerFront"),
    Some("MicrocontrollerSide"),
    Some("MicrocontrollerSide")
  )

  // ----------------------------------------------------------------------- //

  override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case mcu: tileentity.Microcontroller => mcu.info.copyItemStack()
      case _ => null
    }

  // Custom drop logic for NBT tagged item stack.
  override def getDrops(world: World, x: Int, y: Int, z: Int, metadata: Int, fortune: Int) = new java.util.ArrayList[ItemStack]()

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, metadata: Int) {}

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val info = new ItemUtils.MicrocontrollerData(stack)
      for (component <- info.components) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.caseRate(Tier.One)

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Microcontroller()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, x: Int, y: Int, z: Int, side: Int) =
    world.getTileEntity(x, y, z) match {
      case computer: tileentity.Microcontroller if computer.isRunning => 15
      case _ => 0
    }

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking && !Wrench.holdsApplicableWrench(player, BlockPosition(x, y, z))) {
      if (!world.isRemote) {
        world.getTileEntity(x, y, z) match {
          case mcu: tileentity.Microcontroller =>
            if (mcu.machine.isRunning) mcu.machine.stop()
            else mcu.machine.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, x, y, z, entity, stack)
    if (!world.isRemote) world.getTileEntity(x, y, z) match {
      case mcu: tileentity.Microcontroller =>
        mcu.info.load(stack)
        mcu.snooperNode.changeBuffer(mcu.info.storedEnergy - mcu.snooperNode.localBuffer)
      case _ =>
    }
  }

  override def removedByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean = {
    if (!world.isRemote) {
      world.getTileEntity(x, y, z) match {
        case mcu: tileentity.Microcontroller =>
          mcu.saveComponents()
          mcu.info.storedEnergy = mcu.snooperNode.localBuffer.toInt
          dropBlockAsItem(world, x, y, z, mcu.info.createItemStack())
        case _ =>
      }
    }
    super.removedByPlayer(world, player, x, y, z, willHarvest)
  }
}
