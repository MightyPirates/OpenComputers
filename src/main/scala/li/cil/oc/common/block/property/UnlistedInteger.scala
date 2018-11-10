package li.cil.oc.common.block.property

import net.minecraftforge.common.property.IUnlistedProperty

class UnlistedInteger(val name:String) extends IUnlistedProperty[Integer]{
  override def getName: String = name

  override def isValid(value: Integer): Boolean = value != null

  override def getType: Class[Integer] = classOf[Integer]

  override def valueToString(value: Integer): String = value.toString
}
