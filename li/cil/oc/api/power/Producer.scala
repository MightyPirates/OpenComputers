package li.cil.oc.api.power

trait Producer extends Receiver {
  demand = 0

  def powerDemand: Double = provider match {
    case Some(p) => p.getDemand
    case _ => 0
  }

  def maxEnergy: Double = provider match {
    case Some(p) => p.MAXENERGY
    case _ => 0
  }

  def addEnergy(amount: Double) = provider match {
    case Some(p) => p.addEnergy(amount)
    case _ =>
  }
}
