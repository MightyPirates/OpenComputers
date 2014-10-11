package thaumcraft.api.damagesource;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

public class DamageSourceThaumcraft extends DamageSource
{
    
	public static DamageSource taint = new DamageSourceThaumcraft("taint").setDamageBypassesArmor().setMagicDamage();
	public static DamageSource tentacle = new DamageSourceThaumcraft("tentacle");
	public static DamageSource swarm = new DamageSourceThaumcraft("swarm");
	public static DamageSource dissolve = new DamageSourceThaumcraft("dissolve").setDamageBypassesArmor();
	
    protected DamageSourceThaumcraft(String par1Str) {
		super(par1Str);
	}
    
	/** This kind of damage can be blocked or not. */
    private boolean isUnblockable = false;
    private boolean isDamageAllowedInCreativeMode = false;
    private float hungerDamage = 0.3F;

    /** This kind of damage is based on fire or not. */
    private boolean fireDamage;

    /** This kind of damage is based on a projectile or not. */
    private boolean projectile;

    /**
     * Whether this damage source will have its damage amount scaled based on the current difficulty.
     */
    private boolean difficultyScaled;
    private boolean magicDamage = false;
    private boolean explosion = false;
    
    public static DamageSource causeSwarmDamage(EntityLivingBase par0EntityLiving)
    {
        return new EntityDamageSource("swarm", par0EntityLiving);
    }

    public static DamageSource causeTentacleDamage(EntityLivingBase par0EntityLiving)
    {
        return new EntityDamageSource("tentacle", par0EntityLiving);
    }
    
}
