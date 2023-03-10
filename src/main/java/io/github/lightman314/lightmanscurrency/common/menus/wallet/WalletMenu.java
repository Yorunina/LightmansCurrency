package io.github.lightman314.lightmanscurrency.common.menus.wallet;

import io.github.lightman314.lightmanscurrency.common.core.ModMenus;
import io.github.lightman314.lightmanscurrency.common.menus.slots.BlacklistSlot;
import io.github.lightman314.lightmanscurrency.common.menus.slots.DisplaySlot;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class WalletMenu extends WalletMenuBase {
	
	public WalletMenu(int windowId, PlayerInventory inventory, int walletStackIndex)
	{
		
		super(ModMenus.WALLET.get(), windowId, inventory, walletStackIndex);
		
		//Player Inventory before coin slots for desync safety.
		//Should make the Player Inventory slot indexes constant regardless of the wallet state.
		for(int y = 0; y < 3; y++)
		{
			for(int x = 0; x < 9; x++)
			{
				int index = x + (y * 9) + 9;
				if(index == this.walletStackIndex)
					this.addSlot(new DisplaySlot(this.inventory, index, 8 + x * 18, 32 + (y + getRowCount()) * 18));
				else
					this.addSlot(new BlacklistSlot(this.inventory, index, 8 + x * 18, 32 + (y + getRowCount()) * 18, this.inventory, this.walletStackIndex));
			}
		}
		
		//Player hotbar
		for(int x = 0; x < 9; x++)
		{
			if(x == this.walletStackIndex)
				this.addSlot(new DisplaySlot(this.inventory, x, 8 + x * 18, 90 + getRowCount() * 18));
			else
				this.addSlot(new BlacklistSlot(this.inventory, x, 8 + x * 18, 90 + getRowCount() * 18, this.inventory, this.walletStackIndex));
		}
		
		//Coin Slots last as they may vary between client and server at times.
		this.addCoinSlots(18);
		
		this.addDummySlots(37 + getMaxWalletSlots());
		
	}
	
	@Nonnull
	@Override
	public ItemStack quickMoveStack(@Nonnull PlayerEntity playerEntity, int index)
	{
		
		if(index + this.coinInput.getContainerSize() == this.walletStackIndex)
			return ItemStack.EMPTY;
		
		ItemStack clickedStack = ItemStack.EMPTY;
		
		Slot slot = this.slots.get(index);
		
		if(slot != null && slot.hasItem())
		{
			ItemStack slotStack = slot.getItem();
			clickedStack = slotStack.copy();
			if(index < 36)
			{
				if(!this.moveItemStackTo(slotStack, 36, this.slots.size(), false))
				{
					return ItemStack.EMPTY;
				}
			}
			else if(!this.moveItemStackTo(slotStack, 0, 36, true))
			{
				return ItemStack.EMPTY;
			}
			
			if(slotStack.isEmpty())
			{
				slot.set(ItemStack.EMPTY);
			}
			else
			{
				slot.setChanged();
			}
		}
		
		return clickedStack;
		
	}
	
	public void QuickCollectCoins()
	{
		IInventory inv = this.player.inventory;
		for(int i = 0; i < inv.getContainerSize(); ++i)
		{
			ItemStack item = inv.getItem(i);
			if(MoneyUtil.isCoin(item, false))
			{
				ItemStack result = this.PickupCoins(item);
				inv.setItem(i, result);
			}
		}
	}
	
}