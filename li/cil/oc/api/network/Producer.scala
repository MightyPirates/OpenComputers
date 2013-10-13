package li.cil.oc.api.network

trait Producer extends Receiver {
  demand = 0

  def powerDemand:Double= {
    if (main != null) {
      main.getDemand
    }
    else {
      0.0
    }
  }

  def maxEnergy:Double= {
    if (main != null) {
      main.MAXENERGY
    }
    else {
      0.0
    }

  }

  def addEnergy(amount: Double) {
    if (main != null) {
      main.addEnergy(amount)
    }
  }
}
