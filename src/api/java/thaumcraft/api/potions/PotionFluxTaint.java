package thaumcraft.api.potions;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.api.entities.ITaintedMob;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PotionFluxTaint extends Potion
{
    public static PotionFluxTaint instance = null; // will be instantiated at runtime
    private int statusIconIndex = -1;
    
    public PotionFluxTaint(int par1, boolean par2, int par3)
    {
    	super(par1,par2,par3);
    	setIconIndex(0, 0);
    }
    
    public static void init()
    {
    	instance.setPotionName("potion.fluxtaint");
    	instance.setIconIndex(3, 1);
    	instance.setEffectiveness(0.25D);
    }
    
	@Override
	public boolean isBadEffect() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getStatusIconIndex() {
		Minecraft.getMinecraft().renderEngine.bindTexture(rl);
		return super.getStatusIconIndex();
	}
	
	static final ResourceLocation rl = new ResourceLocation("thaumcraft","textures/misc/potions.png");
	
	@Override
	public void performEffect(EntityLivingBase target, int par2) {
		if (target instanceof ITaintedMob) {
			target.heal(1);
		} else
		if (!target.isEntityUndead() && !(target instanceof EntityPlayer))
        {
			target.attackEntityFrom(DamageSourceThaumcraft.taint, 1);
        } 
		else
		if (!target.isEntityUndead() && (target.getMaxHealth() > 1 || (target instanceof EntityPlayer)))
        {
			target.attackEntityFrom(DamageSourceThaumcraft.taint, 1);
        } 
	}
    
	public boolean isReady(int par1, int par2)
    {
		int k = 40 >> par2;
        return k > 0 ? par1 % k == 0 : true;
    }
    
}
