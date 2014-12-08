package li.cil.oc.common.block

import li.cil.oc.api
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

// TODO Remove in 1.5
class DelegatorConverter extends Block(Material.rock) {
  NEI.hide(this)

  override def hasTileEntity(metadata: Int) = true

  // We don't have to register this tile entity because it'll vanish immediately anyway, so
  // it'll not be saved, therefore not be loaded, therefor no need for the mapping.
  override def createTileEntity(world: World, metadata: Int) = new tileentity.traits.TileEntity() {}
}

object DelegatorConverter {

  class Item(block: Block) extends ItemBlock(block) {
    override def getItemStackDisplayName(stack: ItemStack) = "Pick me up to fix me!"

    override def onUpdate(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
      entity match {
        case player: EntityPlayer => DelegatorConverter.convert(stack, this) match {
          case Some(replacement) => player.inventory.setInventorySlotContents(slot, replacement)
          case _ => // Nothing to convert.
        }
        case _ => // Can't convert.
      }
    }
  }

  def convert(world: World, x: Int, y: Int, z: Int, data: Option[NBTTagCompound]) {
    val block = world.getBlock(x, y, z)
    val meta = world.getBlockMetadata(x, y, z)
    getDescriptor(block, meta) match {
      case Some(descriptor) =>
        world.setBlock(x, y, z, descriptor.block())
        data.foreach(nbt => world.getTileEntity(x, y, z) match {
          case t: TileEntity => t.readFromNBT(nbt)
          case _ => // Failed.
        })
      case _ => // Nothing to convert.
    }
  }

  def convert(stack: ItemStack): ItemStack = {
    if (stack != null) {
      stack.getItem match {
        case item: ItemBlock =>
          convert(stack, item) match {
            case Some(newStack) => return newStack
            case _ =>
          }
        case _ =>
      }
    }
    stack
  }

  def convert(stack: ItemStack, item: ItemBlock): Option[ItemStack] = {
    val block = item.field_150939_a
    val meta = stack.getItemDamage
    getDescriptor(block, meta) match {
      case Some(descriptor) =>
        val newStack = descriptor.createItemStack(stack.stackSize)
        if (stack.hasTagCompound) {
          newStack.setTagCompound(stack.getTagCompound.copy().asInstanceOf[NBTTagCompound])
        }
        Option(newStack)
      case _ => None
    }
  }

  def getDescriptor(block: Block, meta: Int) = {
    val oldName = Block.blockRegistry.getNameForObject(block)
    val newName = if (oldName == "OpenComputers:simple") convertSimple(meta)
    else if (oldName == "OpenComputers:simple_redstone") convertSimpleRedstone(meta)
    else if (oldName == "OpenComputers:special") convertSpecial(meta)
    else if (oldName == "OpenComputers:special_redstone") convertSpecialRedstone(meta)
    else None
    newName.map(api.Items.get)
  }

  private def convertSimple(id: Int): Option[String] = id match {
    case 0 => Option("adapter")
    case 1 => Option("capacitor")
    case 2 => Option("diskDrive")
    case 3 => Option("powerDistributor")
    case 4 => Option("powerConverter")
    case 5 => Option("switch")
    case 6 => Option("screen1")
    case 7 => Option("screen2")
    case 8 => Option("screen3")
    case 9 => Option("accessPoint")
    case 10 => Option("geolyzer")
    case 11 => Option("disassembler")
    case 12 => Option("motionSensor")
    case _ => None
  }

  private def convertSimpleRedstone(id: Int): Option[String] = id match {
    case 0 => Option("case1")
    case 1 => Option("case2")
    case 2 => Option("case3")
    case 3 => Option("charger")
    case 4 => Option("redstone")
    case 5 => Option("screen1")
    case 6 => Option("screen2")
    case 7 => Option("screen3")
    case 8 => Option("caseCreative")
    case _ => None
  }

  private def convertSpecial(id: Int): Option[String] = id match {
    case 0 => Option("cable")
    case 1 => Option("keyboard")
    case 2 => Option("robotAfterimage")
    case 3 => Option("hologram1")
    case 4 => Option("hologram2")
    case 5 => Option("assembler")
    case _ => None
  }

  private def convertSpecialRedstone(id: Int): Option[String] = id match {
    case 0 => Option("robot")
    case 1 => Option("serverRack")
    case _ => None
  }
}