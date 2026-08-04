package li.cil.oc.common.component

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class UpgradeSkinComponent(pName:String) {
  val name = pName
  var skin = None : Option[ItemStack]
  var rotX = 0f
  var rotY = 0f
  var rotZ = 0f
  var oldRotX = 0f
  var oldRotY = 0f
  var oldRotZ = 0f
  var animationTicksLeft = 0
  var animationTicksTotal = 0

  def prepareRotationAnimation(pRotX: Float, pRotY: Float, pRotZ: Float, duration: Int): Unit = {
    /*oldRotX = setOldRot(pRotX, rotX)
    oldRotY = setOldRot(pRotY, rotY)
    oldRotZ = setOldRot(pRotZ, rotZ)
    rotX = floorMod(pRotX, 360)
    rotY = floorMod(pRotY, 360)
    rotZ = floorMod(pRotZ, 360)*/
    oldRotX = rotX
    oldRotY = rotY
    oldRotZ = rotZ
    rotX = pRotX
    rotY = pRotY
    rotZ = pRotZ
    animationTicksTotal = duration
    animationTicksLeft = duration
  }

  def setOldRot(newRot: Float, oldRot: Float): Float = {
    if(newRot > 360) {
      oldRot - 360
    } else if(newRot < 0) {
      oldRot + 360
    } else {
      oldRot
    }
  }

  def floorMod(a: Float, b: Int): Float = {
    var am = a % b
    if (am < 0) {
      am += b
    }
    am
  }

  def updateClient(): Unit = {
    if(animationTicksLeft > 0) {
      animationTicksLeft -= 1
      if(animationTicksLeft == 0) {
        animationTicksTotal = 0
      }
    }
  }

  def save(nbt: NBTTagCompound): Unit = {
    val tag = new NBTTagCompound()

    if(skin.isDefined) {
      val skinNbt = new NBTTagCompound()
      skin.get.writeToNBT(skinNbt)
      tag.setTag("skin", skinNbt)
    }
    tag.setFloat("rotX", rotX)
    tag.setFloat("rotY", rotY)
    tag.setFloat("rotZ", rotZ)
    tag.setFloat("oldRotX", oldRotX)
    tag.setFloat("oldRotY", oldRotY)
    tag.setFloat("oldRotZ", oldRotZ)
    tag.setInteger("animationTicksLeft", animationTicksLeft)
    tag.setInteger("animationTicksTotal", animationTicksTotal)

    nbt.setTag("skin" + name, tag)
  }

  def load(nbt: NBTTagCompound): Unit = {
    if(nbt.hasKey("skin" + name)) {
      val tag = nbt.getCompoundTag("skin" + name)

      if(tag.hasKey("skin")) {
        skin = Some(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("skin")))
      } else {
        skin = None
      }
      rotX = tag.getFloat("rotX")
      rotY = tag.getFloat("rotY")
      rotZ = tag.getFloat("rotZ")

      if(tag.hasKey("animationTicksLeft")) {
        oldRotX = tag.getFloat("oldRotX")
        oldRotY = tag.getFloat("oldRotY")
        oldRotZ = tag.getFloat("oldRotZ")
        animationTicksLeft = tag.getInteger("animationTicksLeft")
        animationTicksTotal = tag.getInteger("animationTicksTotal")
      }
    }
  }
}
