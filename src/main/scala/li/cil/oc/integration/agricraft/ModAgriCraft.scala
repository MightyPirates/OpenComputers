package li.cil.oc.integration.agricraft

import com.InfinityRaider.AgriCraft.blocks.BlockCrop
import li.cil.oc.integration.util.Crop
import li.cil.oc.integration.util.Crop.CropProvider
import li.cil.oc.integration.{Mods, Mod, ModProxy}
import li.cil.oc.server.component._
import li.cil.oc.util.BlockPosition
import net.minecraft.block.Block
import net.minecraft.item.Item

object ModAgriCraft extends ModProxy with CropProvider {
  override def getMod: Mod = Mods.AgriCraft

  override def initialize(): Unit = {
    Crop.addProvider(this)
  }

  override def getInformation(pos: BlockPosition): Array[AnyRef] = {
    val world = pos.world.get
    val target = world.getBlock(pos.x,pos.y,pos.z)
    target match {
      case crop:BlockCrop=>{
        val meta = world.getBlockMetadata(pos.x, pos.y, pos.z)
        val value = meta * 100 / 2
        result(Item.itemRegistry.getNameForObject(crop.getSeed))
      }
      case _=>result(Unit,"not a thing")
    }
  }

  override def isValidFor(block: Block): Boolean = {
    block match {
      case _: BlockCrop => true
      case _ => false
    }
  }
}
