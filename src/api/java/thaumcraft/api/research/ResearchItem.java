package thaumcraft.api.research;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

public class ResearchItem 
{
	/**
	 * A short string used as a key for this research. Must be unique
	 */
	public final String key;
	
	/**
	 * A short string used as a reference to the research category to which this must be added.
	 */
	public final String category;

	/**
	 * The aspect tags and their values required to complete this research
	 */
	public final AspectList tags;
	
    /**
     * This links to any research that needs to be completed before this research can be discovered or learnt.
     */
    public String[] parents = null;
    
    /**
     * Like parent above, but a line will not be displayed in the thaumonomicon linking them. Just used to prevent clutter.
     */
    public String[] parentsHidden = null;
    /**
     * any research linked to this that will be unlocked automatically when this research is complete
     */
    public String[] siblings = null;
	
    /**
     * the horizontal position of the research icon
     */
    public final int displayColumn;

    /**
     * the vertical position of the research icon
     */
    public final int displayRow;
    
    /**
     * the icon to be used for this research 
     */
    public final ItemStack icon_item;
    
    /**
     * the icon to be used for this research 
     */
    public final ResourceLocation icon_resource;
    
    /**
     * How large the research grid is. Valid values are 1 to 3.
     */
    private int complexity;

    /**
     * Special research has a spiky border. Used for important research milestones.
     */
    private boolean isSpecial;
    
    /**
     * Research that can be directly purchased with RP in normal research difficulty.
     */
    private boolean isSecondary;
    
	/**
     * This indicates if the research should use a circular icon border. Usually used for "passive" research 
     * that doesn't have recipes and grants passive effects, or that unlock automatically.
     */
    private boolean isRound;
    
    /**
     * Stub research cannot be discovered by normal means, but can be unlocked via the sibling system.
     */
    private boolean isStub;
    
    /**
     * This indicated that the research is completely hidden and cannot be discovered by any 
     * player-controlled means. The recipes will never show up in the thaumonomicon.
     * Usually used to unlock "hidden" recipes via sibling unlocking, like 
     * the various cap and rod combos for wands.
     */
    private boolean isVirtual;    
    
    @Deprecated
    private boolean isLost;
    
    /**
     * Concealed research does not display in the thaumonomicon until parent researches are discovered.
     */
    private boolean isConcealed;
    
    /**
     * Hidden research can only be discovered via scanning or knowledge fragments 
     */
    private boolean isHidden;
    
    /**
     * These research items will automatically unlock for all players on game start
     */
    private boolean isAutoUnlock;
    
    /**
     * Scanning these items will have a chance of revealing hidden knowledge in the thaumonomicon
     */
    private ItemStack[] itemTriggers;
    
    /**
     * Scanning these entities will have a chance of revealing hidden knowledge in the thaumonomicon
     */
    private String[] entityTriggers;
    
    /**
     * Scanning things with these aspects will have a chance of revealing hidden knowledge in the thaumonomicon
     */
    private Aspect[] aspectTriggers;

	private ResearchPage[] pages = null;
	
	public ResearchItem(String key, String category)
    {
    	this.key = key;
    	this.category = category;
    	this.tags = new AspectList();    	
        this.icon_resource = null;
        this.icon_item = null;
        this.displayColumn = 0;
        this.displayRow = 0;
        this.setVirtual();
        
    }
    
    public ResearchItem(String key, String category, AspectList tags, int col, int row, int complex, ResourceLocation icon)
    {
    	this.key = key;
    	this.category = category;
    	this.tags = tags;    	
        this.icon_resource = icon;
        this.icon_item = null;
        this.displayColumn = col;
        this.displayRow = row;
        this.complexity = complex;
        if (complexity < 1) this.complexity = 1;
        if (complexity > 3) this.complexity = 3;
    }
    
    public ResearchItem(String key, String category, AspectList tags, int col, int row, int complex, ItemStack icon)
    {
    	this.key = key;
    	this.category = category;
    	this.tags = tags;    	
        this.icon_item = icon;
        this.icon_resource = null;
        this.displayColumn = col;
        this.displayRow = row;
        this.complexity = complex;
        if (complexity < 1) this.complexity = 1;
        if (complexity > 3) this.complexity = 3;
    }

    public ResearchItem setSpecial()
    {
        this.isSpecial = true;
        return this;
    }
    
    public ResearchItem setStub()
    {
        this.isStub = true;
        return this;
    }
    
    @Deprecated
    public ResearchItem setLost()
    {
        this.isLost = true;
        return this;
    }
    
    public ResearchItem setConcealed()
    {
        this.isConcealed = true;
        return this;
    }
    
    public ResearchItem setHidden()
    {
        this.isHidden = true;
        return this;
    }
    
    public ResearchItem setVirtual()
    {
        this.isVirtual = true;
        return this;
    }
    
    public ResearchItem setParents(String... par)
    {
        this.parents = par;
        return this;
    }
    
    

	public ResearchItem setParentsHidden(String... par)
    {
        this.parentsHidden = par;
        return this;
    }
    
    public ResearchItem setSiblings(String... sib)
    {
        this.siblings = sib;
        return this;
    }
    
    public ResearchItem setPages(ResearchPage... par)
    {
        this.pages = par;
        return this;
    }
    
    public ResearchPage[] getPages() {
		return pages;
	}
    
    public ResearchItem setItemTriggers(ItemStack... par)
    {
        this.itemTriggers = par;
        return this;
    }
    
    public ResearchItem setEntityTriggers(String... par)
    {
        this.entityTriggers = par;
        return this;
    }
    
    public ResearchItem setAspectTriggers(Aspect... par)
    {
        this.aspectTriggers = par;
        return this;
    }

    public ItemStack[] getItemTriggers() {
		return itemTriggers;
	}

	public String[] getEntityTriggers() {
		return entityTriggers;
	}
	
	public Aspect[] getAspectTriggers() {
		return aspectTriggers;
	}

	public ResearchItem registerResearchItem()
    {
        ResearchCategories.addResearch(this);
        return this;
    }

    public String getName()
    {
    	return StatCollector.translateToLocal("tc.research_name."+key);
    }
    
    public String getText()
    {
    	return StatCollector.translateToLocal("tc.research_text."+key);
    }

    public boolean isSpecial()
    {
        return this.isSpecial;
    }
    
    public boolean isStub()
    {
        return this.isStub;
    }
        
    @Deprecated
    public boolean isLost()
    {
        return this.isLost;
    }
    
    public boolean isConcealed()
    {
        return this.isConcealed;
    }
    
    public boolean isHidden()
    {
        return this.isHidden;
    }
    
    public boolean isVirtual()
    {
        return this.isVirtual;
    }
    
    public boolean isAutoUnlock() {
		return isAutoUnlock;
	}
	
	public ResearchItem setAutoUnlock()
    {
        this.isAutoUnlock = true;
        return this;
    }
	
	public boolean isRound() {
		return isRound;
	}

	public ResearchItem setRound() {
		this.isRound = true;
		return this;
	}
	
	public boolean isSecondary() {
		return isSecondary;
	}

	public ResearchItem setSecondary() {
		this.isSecondary = true;
		return this;
	}

	public int getComplexity() {
		return complexity;
	}

	public ResearchItem setComplexity(int complexity) {
		this.complexity = complexity;
		return this;
	}

	/**
	 * @return the aspect aspects ordinal with the highest value. Used to determine scroll color and similar things
	 */
	public Aspect getResearchPrimaryTag() {
		Aspect aspect=null;
		int highest=0;
		if (tags!=null)
		for (Aspect tag:tags.getAspects()) {
			if (tags.getAmount(tag)>highest) {
				aspect=tag;
				highest=tags.getAmount(tag);
			};
		}
		return aspect;
	}
	
}
