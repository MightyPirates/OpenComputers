package li.cil.oc.api.detail

import li.cil.oc.api.driver.{Block, Item}

/** Avoids reflection structural types would induce. */
trait DriverAPI {
  def add(driver: Block)

  def add(driver: Item)
}
