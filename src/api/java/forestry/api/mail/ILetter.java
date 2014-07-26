/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.mail;

import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import forestry.api.core.INBTTagable;

public interface ILetter extends IInventory, INBTTagable {

	ItemStack[] getPostage();

	void setProcessed(boolean flag);

	boolean isProcessed();

	boolean isMailable();

	void setSender(MailAddress address);

	MailAddress getSender();

	boolean hasRecipient();

	void setRecipient(MailAddress address);

	MailAddress[] getRecipients();

	String getRecipientString();

	void setText(String text);

	String getText();

	@SuppressWarnings("rawtypes")
	void addTooltip(List list);

	boolean isPostPaid();

	int requiredPostage();

	void invalidatePostage();

	ItemStack[] getAttachments();

	void addAttachment(ItemStack itemstack);

	void addAttachments(ItemStack[] itemstacks);

	int countAttachments();

	void addStamps(ItemStack stamps);

}
