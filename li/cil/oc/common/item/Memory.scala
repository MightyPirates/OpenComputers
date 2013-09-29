package li.cil.oc.common.item

class Memory(val parent: Delegator, val kiloBytes: Int) extends Delegate {
  def unlocalizedName = "Memory" + kiloBytes + "k"
}
