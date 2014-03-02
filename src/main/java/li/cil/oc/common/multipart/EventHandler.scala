package li.cil.oc.common.multipart

import net.minecraftforge.event.entity.player.{PlayerDestroyItemEvent, PlayerInteractEvent}
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.vec.{Vector3, BlockCoord}
import net.minecraft.block.Block
import li.cil.oc.Blocks
import codechicken.multipart.{TileMultipart, TMultiPart}
import codechicken.lib.packet.PacketCustom
import net.minecraft.network.packet.Packet15Place
import net.minecraftforge.common.MinecraftForge
import li.cil.oc.common.block.Delegator
import li.cil.oc.client.PacketSender

object EventHandler {

  @ForgeSubscribe
  def playerInteract(event: PlayerInteractEvent) {
    if (event.action == Action.RIGHT_CLICK_BLOCK && event.entityPlayer.worldObj.isRemote) {
      if (place(event.entityPlayer))
        event.setCanceled(true)

    }
  }

  def place(player: EntityPlayer): Boolean = {
    val world = player.getEntityWorld
    val hit = RayTracer.reTrace(world, player)
    if (hit == null)
      return false

    val pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ)
    val held = player.getHeldItem
    var part: TMultiPart = null
    if (held == null)
      return false

    Delegator.subBlock(held) match {
      case Some(subBlock) if subBlock == Blocks.cable =>
        part = new CablePart()
      case _ =>
    }

    if (part == null)
      return false

    //attempt to use block activated like normal and tell the server the right stuff
    if (world.isRemote && !player.isSneaking) {
      val f = new Vector3(hit.hitVec).add(-hit.blockX, -hit.blockY, -hit.blockZ)
      val block = Block.blocksList(world.getBlockId(hit.blockX, hit.blockY, hit.blockZ))
      if (block != null &&  block.onBlockActivated(world, hit.blockX, hit.blockY, hit.blockZ, player, hit.sideHit, f.x.toFloat, f.y.toFloat, f.z.toFloat)) {
        player.swingItem()
        PacketCustom.sendToServer(new Packet15Place(
          hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
          player.inventory.getCurrentItem,
          f.x.toFloat, f.y.toFloat, f.z.toFloat))
        return false
      }
    }

    val tile = TileMultipart.getOrConvertTile(world, pos)
    if (tile == null || !tile.canAddPart(part))
      return false

    if (!world.isRemote) {
      TileMultipart.addPart(world, pos, part)
      world.playSoundEffect(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
        Blocks.cable.parent.stepSound.getPlaceSound,
        (Blocks.cable.parent.stepSound.getVolume + 1.0F) / 2.0F,
        Blocks.cable.parent.stepSound.getPitch * 0.8F)
      if (!player.capabilities.isCreativeMode) {
        held.stackSize -= 1
        if (held.stackSize == 0) {
          player.inventory.mainInventory(player.inventory.currentItem) = null
          MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, held))
        }
      }
    }
    else {
      player.swingItem()
      //TODO
      PacketSender.sendMultiPlace()
    }
    true
  }


}
