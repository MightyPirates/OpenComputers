package thaumcraft.api.aspects;

import java.io.Serializable;
import java.util.LinkedHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import thaumcraft.api.ThaumcraftApiHelper;

public class AspectList implements Serializable {
	
	public LinkedHashMap<Aspect,Integer> aspects = new LinkedHashMap<Aspect,Integer>();//aspects associated with this object

	
	/**
	 * this creates a new aspect list with preloaded values based off the aspects of the given item.
	 * @param the itemstack of the given item
	 */
	public AspectList(ItemStack stack) {
		try {
			AspectList temp = ThaumcraftApiHelper.getObjectAspects(stack);
			if (temp!=null)
			for (Aspect tag:temp.getAspects()) {
				add(tag,temp.getAmount(tag));
			}
		} catch (Exception e) {}
	}
	
	public AspectList() {
	}
	
	public AspectList copy() {
		AspectList out = new AspectList();
		for (Aspect a:this.getAspects())
			out.add(a, this.getAmount(a));
		return out;
	}
	
	/**
	 * @return the amount of different aspects in this collection
	 */
	public int size() {
		return aspects.size();
	}
	
	/**
	 * @return the amount of total vis in this collection
	 */
	public int visSize() {
		int q = 0;
		
		for (Aspect as:aspects.keySet()) {
			q+=this.getAmount(as);
		}
		
		return q;
	}
	
	/**
	 * @return an array of all the aspects in this collection
	 */
	public Aspect[] getAspects() {
		Aspect[] q = new Aspect[1];
		return aspects.keySet().toArray(q);
	}
	
	/**
	 * @return an array of all the aspects in this collection
	 */
	public Aspect[] getPrimalAspects() {
		AspectList t = new AspectList();
		for (Aspect as:aspects.keySet()) {
			if (as.isPrimal()) {
				t.add(as,1);
			}
		}
		Aspect[] q = new Aspect[1];
		return t.aspects.keySet().toArray(q);
	}
	
	/**
	 * @return an array of all the aspects in this collection sorted by name
	 */
	public Aspect[] getAspectsSorted() {
		try {
			Aspect[] out = aspects.keySet().toArray(new Aspect[1]);
			boolean change=false;
			do {
				change=false;
				for(int a=0;a<out.length-1;a++) {
					Aspect e1 = out[a];
					Aspect e2 = out[a+1];
					if (e1!=null && e2!=null && e1.getTag().compareTo(e2.getTag())>0) {
						out[a] = e2;
						out[a+1] = e1;
						change = true;
						break;
					}
				}
			} while (change==true);
			return out;
		} catch (Exception e) {
			return this.getAspects(); 
		}
	}
	
	/**
	 * @return an array of all the aspects in this collection sorted by amount
	 */
	public Aspect[] getAspectsSortedAmount() {
		try {
			Aspect[] out = aspects.keySet().toArray(new Aspect[1]);
			boolean change=false;
			do {
				change=false;
				for(int a=0;a<out.length-1;a++) {
					int e1 = getAmount(out[a]); 
					int e2 = getAmount(out[a+1]);
					if (e1>0 && e2>0 && e2>e1) {
						Aspect ea = out[a];
						Aspect eb = out[a+1];
						out[a] = eb;
						out[a+1] = ea;
						change = true;
						break;
					}
				}
			} while (change==true);
			return out;
		} catch (Exception e) {
			return this.getAspects();
		}
	}
	
	/**
	 * @param key
	 * @return the amount associated with the given aspect in this collection
	 */
	public int getAmount(Aspect key) {
		return  aspects.get(key)==null?0:aspects.get(key);
	}
	
	/**
	 * Reduces the amount of an aspect in this collection by the given amount. 
	 * @param key
	 * @param amount
	 * @return 
	 */
	public boolean reduce(Aspect key, int amount) {
		if (getAmount(key)>=amount) {
			int am = getAmount(key)-amount;
			aspects.put(key, am);
			return true;
		}
		return false;
	}
	
	/**
	 * Reduces the amount of an aspect in this collection by the given amount. 
	 * If reduced to 0 or less the aspect will be removed completely. 
	 * @param key
	 * @param amount
	 * @return
	 */
	public AspectList remove(Aspect key, int amount) {
		int am = getAmount(key)-amount;
		if (am<=0) aspects.remove(key); else
		this.aspects.put(key, am);
		return this;
	}
	
	/**
	 * Simply removes the aspect from the list
	 * @param key
	 * @param amount
	 * @return
	 */
	public AspectList remove(Aspect key) {
		aspects.remove(key); 
		return this;
	}
	
	/**
	 * Adds this aspect and amount to the collection. 
	 * If the aspect exists then its value will be increased by the given amount.
	 * @param aspect
	 * @param amount
	 * @return
	 */
	public AspectList add(Aspect aspect, int amount) {
		if (this.aspects.containsKey(aspect)) {
			int oldamount = this.aspects.get(aspect);
			amount+=oldamount;
		}
		this.aspects.put( aspect, amount );
		return this;
	}

	
	/**
	 * Adds this aspect and amount to the collection. 
	 * If the aspect exists then only the highest of the old or new amount will be used.
	 * @param aspect
	 * @param amount
	 * @return
	 */
	public AspectList merge(Aspect aspect, int amount) {
		if (this.aspects.containsKey(aspect)) {
			int oldamount = this.aspects.get(aspect);
			if (amount<oldamount) amount=oldamount;
			
		}
		this.aspects.put( aspect, amount );
		return this;
	}
	
	/**
	 * Reads the list of aspects from nbt
	 * @param nbttagcompound
	 * @return 
	 */
	public void readFromNBT(NBTTagCompound nbttagcompound)
    {
        aspects.clear();
        NBTTagList tlist = nbttagcompound.getTagList("Aspects",(byte)10);
		for (int j = 0; j < tlist.tagCount(); j++) {
			NBTTagCompound rs = (NBTTagCompound) tlist.getCompoundTagAt(j);
			if (rs.hasKey("key")) {
				add(	Aspect.getAspect(rs.getString("key")),
						rs.getInteger("amount"));
			}
		}
    }
	
	/**
	 * Writes the list of aspects to nbt
	 * @param nbttagcompound
	 * @return 
	 */
	public void writeToNBT(NBTTagCompound nbttagcompound)
    {
        NBTTagList tlist = new NBTTagList();
		nbttagcompound.setTag("Aspects", tlist);
		for (Aspect aspect : getAspects())
			if (aspect != null) {
				NBTTagCompound f = new NBTTagCompound();
				f.setString("key", aspect.getTag());
				f.setInteger("amount", getAmount(aspect));
				tlist.appendTag(f);
			}
    }
	
	
	
}
