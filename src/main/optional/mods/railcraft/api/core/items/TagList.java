/*
 * Copyright (c) CovertJaguar, 2011 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.core.items;

import cpw.mods.fml.common.registry.GameRegistry;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This is a collection of ItemStack tags than can be used with
 * GameRegistry.findItemStack().
 *
 * @author CovertJaguar <http://www.railcraft.info>
 * @see GameRegistry#findItemStack(java.lang.String, java.lang.String, int)
 */
public class TagList {

    private static final SortedSet<String> tags = new TreeSet<String>();
    private static final SortedSet<String> tagsImmutable = Collections.unmodifiableSortedSet(tags);

    public static void addTag(String tag) {
        tags.add(tag);
    }

    public static SortedSet<String> getTags() {
        return tagsImmutable;
    }
}
