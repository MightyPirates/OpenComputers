package li.cil.oc.client

import li.cil.oc.Settings
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits.Computer
import li.cil.oc.common.{CompressedPacketBuilder, PacketType, SimplePacketBuilder}
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.ForgeDirection

object PacketSender {
  // Timestamp after which the next clipboard message may be sent. Used to
  // avoid spamming large packets on key repeat.
  protected var clipboardCooldown = 0L

  def sendComputerPower(t: Computer, power: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.ComputerPower)

    pb.writeTileEntity(t)
    pb.writeBoolean(power)

    pb.sendToServer()
  }

  def sendKeyDown(address: String, char: Char, code: Int) {
    val pb = new SimplePacketBuilder(PacketType.KeyDown)

    pb.writeUTF(address)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendKeyUp(address: String, char: Char, code: Int) {
    val pb = new SimplePacketBuilder(PacketType.KeyUp)

    pb.writeUTF(address)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendClipboard(address: String, value: String) {
    if (value != null && !value.isEmpty) {
      if (value.length > 64 * 1024 || System.currentTimeMillis() < clipboardCooldown) {
        val player = Minecraft.getMinecraft.thePlayer
        val handler = Minecraft.getMinecraft.getSoundHandler
        handler.playSound(new PositionedSoundRecord(new ResourceLocation("note.harp"), 1, 1, player.posX.toFloat, player.posY.toFloat, player.posZ.toFloat))
      }
      else {
        clipboardCooldown = System.currentTimeMillis() + value.length / 10
        for (part <- value.grouped(16 * 1024)) {
          val pb = new CompressedPacketBuilder(PacketType.Clipboard)

          pb.writeUTF(address)
          pb.writeUTF(part)

          pb.sendToServer()
        }
      }
    }
  }

  def sendMouseClick(address: String, x: Int, y: Int, drag: Boolean, button: Int) {
    val pb = new SimplePacketBuilder(PacketType.MouseClickOrDrag)

    pb.writeUTF(address)
    pb.writeShort(x)
    pb.writeShort(y)
    pb.writeBoolean(drag)
    pb.writeByte(button.toByte)

    pb.sendToServer()
  }

  def sendMouseScroll(address: String, x: Int, y: Int, scroll: Int) {
    val pb = new SimplePacketBuilder(PacketType.MouseScroll)

    pb.writeUTF(address)
    pb.writeShort(x)
    pb.writeShort(y)
    pb.writeByte(scroll)

    pb.sendToServer()
  }

  def sendMouseUp(address: String, x: Int, y: Int, button: Int) {
    val pb = new SimplePacketBuilder(PacketType.MouseUp)

    pb.writeUTF(address)
    pb.writeShort(x)
    pb.writeShort(y)
    pb.writeByte(button.toByte)

    pb.sendToServer()
  }

  def sendMultiPlace() {
    val pb = new SimplePacketBuilder(PacketType.MultiPartPlace)
    pb.sendToServer()
  }

  def sendPetVisibility() {
    val pb = new SimplePacketBuilder(PacketType.PetVisibility)

    pb.writeBoolean(!Settings.get.hideOwnPet)

    pb.sendToServer()
  }

  def sendRobotAssemblerStart(t: RobotAssembler) {
    val pb = new SimplePacketBuilder(PacketType.RobotAssemblerStart)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendRobotStateRequest(dimension: Int, x: Int, y: Int, z: Int) {
    val pb = new SimplePacketBuilder(PacketType.RobotStateRequest)

    pb.writeInt(dimension)
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeInt(z)

    pb.sendToServer()
  }

  def sendServerPower(t: ServerRack, number: Int, power: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.ComputerPower)

    pb.writeTileEntity(t)
    pb.writeInt(number)
    pb.writeBoolean(power)

    pb.sendToServer()
  }

  def sendServerRange(t: ServerRack, range: Int) {
    val pb = new SimplePacketBuilder(PacketType.ServerRange)

    pb.writeTileEntity(t)
    pb.writeInt(range)

    pb.sendToServer()
  }

  def sendServerSide(t: ServerRack, number: Int, side: ForgeDirection) {
    val pb = new SimplePacketBuilder(PacketType.ServerSide)

    pb.writeTileEntity(t)
    pb.writeInt(number)
    pb.writeDirection(side)

    pb.sendToServer()
  }

  def sendServerSwitchMode(t: ServerRack, internal: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.ServerSwitchMode)

    pb.writeTileEntity(t)
    pb.writeBoolean(internal)

    pb.sendToServer()
  }

  def sendTextBufferInit(address: String) {
    val pb = new SimplePacketBuilder(PacketType.TextBufferInit)

    pb.writeUTF(address)

    pb.sendToServer()
  }
}