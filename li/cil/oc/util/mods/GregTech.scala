package li.cil.oc.util.mods

import net.minecraft.item.ItemStack

object GregTech {
  private val (sRecipeAdder, addAssemblerRecipe)=try{
    val api = Class.forName("gregtechmod.api.GregTech_API")
    for(meth<-api.getMethods){
      println(meth)
    }
    val iRecipe = Class.forName("gregtechmod.api.interfaces.IGT_RecipeAdder")

    val adder = api.getField("sRecipeAdder").get(null)
    val addAssemb = iRecipe.getMethods.find(_.getName == "addAssemblerRecipe")

    (Option(adder),addAssemb)
  } catch {
    case e: Throwable =>{
       e.printStackTrace()
      (None, null)
    }
  }

  def available = sRecipeAdder.isDefined


  def addAssemblerRecipe (input1: ItemStack, input2: ItemStack, output: ItemStack, duration: Int, eut: Int){
    (sRecipeAdder,addAssemblerRecipe) match{
      case (Some(adder),Some(recipe))=>recipe.invoke(adder,input1,input2,output,duration:java.lang.Integer,eut:java.lang.Integer)
      case _=>
    }

  }
}
