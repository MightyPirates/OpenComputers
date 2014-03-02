package li.cil.oc.common.multipart

import codechicken.multipart.TMultiPart

/**
 * Created by lordjoda on 02.03.14.
 */
class CablePart extends TMultiPart{

  def getType: String = {
    "oc:cable"
  }
}
