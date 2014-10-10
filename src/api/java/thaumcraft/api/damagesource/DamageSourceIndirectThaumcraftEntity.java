package thaumcraft.api.damagesource;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

public class DamageSourceIndirectThaumcraftEntity extends EntityDamageSourceIndirect {

	private boolean fireDamage;
	private float hungerDamage;
	private boolean isUnblockable;


	public DamageSourceIndirectThaumcraftEntity(String par1Str,
			Entity par2Entity, Entity par3Entity) {
		super(par1Str, par2Entity, par3Entity);
	}

	
	public DamageSource setFireDamage()
    {
        this.fireDamage = true;
        return this;
    }
	
	public DamageSource setDamageBypassesArmor()
    {
        this.isUnblockable = true;
        this.hungerDamage = 0.0F;
        return this;
    }
}
