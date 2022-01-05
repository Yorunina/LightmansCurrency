package io.github.lightman314.lightmanscurrency.tileentity.handler;

import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.settings.ItemTraderSettings;
import io.github.lightman314.lightmanscurrency.trader.settings.ItemTraderSettings.ItemHandlerSettings;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class TraderItemHandler{

	private final IItemHandler inputOnly;
	private final IItemHandler outputOnly;
	private final IItemHandler inputAndOutput;
	
	public TraderItemHandler(IItemTrader trader)
	{
		this.inputOnly = new InputOnly(trader);
		this.outputOnly = new OutputOnly(trader);
		this.inputAndOutput = new InputAndOutput(trader);
	}
	
	public IItemHandler getHandler(ItemTraderSettings.ItemHandlerSettings settings) {
		if(settings == ItemHandlerSettings.INPUT_ONLY)
			return this.inputOnly;
		else if(settings == ItemHandlerSettings.OUTPUT_ONLY)
			return this.outputOnly;
		else if(settings == ItemHandlerSettings.INPUT_AND_OUTPUT)
			return this.inputAndOutput;
		return null;
	}
	
	private static abstract class TraderHandlerTemplate implements IItemHandler
	{
		private final IItemTrader trader;
		
		protected TraderHandlerTemplate(IItemTrader trader) { this.trader = trader; }
		
		protected final IInventory getStorage() { return this.trader.getStorage(); }
		protected final boolean limitInputs() { return this.trader.getItemSettings().limitInputsToSales(); }
		protected final boolean limitOutputs() { return this.trader.getItemSettings().limitOutputsToPurchases(); }
		
		protected final boolean traderSells(ItemStack stack)
		{
			for(ItemTradeData trade : this.trader.getAllTrades())
			{
				if(trade.isValid())
				{
					if(trade.isSale() || trade.isBarter())
					{
						if(InventoryUtil.ItemMatches(trade.getSellItem(), stack))
							return true;
					}
				}
			}
			return false;
		}
		
		protected final boolean traderPurchases(ItemStack stack)
		{
			for(ItemTradeData trade : this.trader.getAllTrades())
			{
				if(trade.isValid())
				{
					if(trade.isPurchase())
					{
						if(InventoryUtil.ItemMatches(trade.getSellItem(), stack))
							return true;
					}
					else if(trade.isBarter())
					{
						if(InventoryUtil.ItemMatches(trade.getBarterItem(), stack))
							return true;
					}
				}
			}
			return false;
		}
		
		//Constant functions that never change (unless disabled, upon which no item handler is given)
		@Override
		public int getSlots() {
			return this.getStorage().getSizeInventory();
		}
		
		@Override
		public ItemStack getStackInSlot(int slot) {
			return this.getStorage().getStackInSlot(slot);
		}

		@Override
		public int getSlotLimit(int slot) {
			return this.getStorage().getStackInSlot(slot).getMaxStackSize();
		}
		
	}
	
	private static class InputOnly extends TraderHandlerTemplate
	{

		private InputOnly(IItemTrader trader) { super(trader); }
		
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			ItemStack copyStack = stack.copy();
			ItemStack currentStack = this.getStorage().getStackInSlot(slot);
			if(currentStack.isEmpty() || InventoryUtil.ItemMatches(copyStack, currentStack))
			{
				//Get the possible fill amount
				int space = currentStack.isEmpty() ? copyStack.getMaxStackSize() : currentStack.getMaxStackSize() - currentStack.getCount();
				int fillAmount = MathUtil.clamp(copyStack.getCount(), 0, space);
				if(fillAmount > 0)
				{
					copyStack.shrink(fillAmount);
					if(!simulate)
					{
						//Place the item into the inventory
						if(currentStack.isEmpty())
						{
							ItemStack placeStack = stack.copy();
							placeStack.setCount(fillAmount);
							this.getStorage().setInventorySlotContents(slot, placeStack);
						}
					}
				}
			}
			return copyStack;
		}

		@Override
		//Disabled in input only
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if(this.limitInputs()) //Only allow inputs if the item is being sold
				return this.traderSells(stack);
			return true;
		}
		
	}
	
	private static class OutputOnly extends TraderHandlerTemplate
	{

		protected OutputOnly(IItemTrader trader) { super(trader); }

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack.copy();
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			ItemStack stack = this.getStorage().getStackInSlot(slot);
			if(stack.isEmpty())
				return ItemStack.EMPTY;
			//Check if the settings limit our ability to remove this item
			if(this.limitOutputs())
			{
				if(!this.traderPurchases(stack))
					return ItemStack.EMPTY;
			}
			int emptyAmount = MathUtil.clamp(amount, 0, Math.min(stack.getCount(), stack.getMaxStackSize()));
			if(emptyAmount > 0)
			{
				ItemStack emptyStack = stack.copy();
				emptyStack.setCount(emptyAmount);
				if(!simulate)
				{
					//Remove the item from storage
					stack.shrink(emptyAmount);
					if(stack.isEmpty())
						this.getStorage().setInventorySlotContents(slot, ItemStack.EMPTY);
				}
				return emptyStack;
			}
			return ItemStack.EMPTY;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}
	
	private static class InputAndOutput extends TraderHandlerTemplate
	{

		protected InputAndOutput(IItemTrader trader) { super(trader); }
		
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			ItemStack copyStack = stack.copy();
			ItemStack currentStack = this.getStorage().getStackInSlot(slot);
			if(currentStack.isEmpty() || InventoryUtil.ItemMatches(copyStack, currentStack))
			{
				//Get the possible fill amount
				int space = currentStack.isEmpty() ? copyStack.getMaxStackSize() : currentStack.getMaxStackSize() - currentStack.getCount();
				int fillAmount = MathUtil.clamp(copyStack.getCount(), 0, space);
				if(fillAmount > 0)
				{
					copyStack.shrink(fillAmount);
					if(!simulate)
					{
						//Place the item into the inventory
						if(currentStack.isEmpty())
						{
							ItemStack placeStack = stack.copy();
							placeStack.setCount(fillAmount);
							this.getStorage().setInventorySlotContents(slot, placeStack);
						}
					}
				}
			}
			return copyStack;
		}
		
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			ItemStack stack = this.getStorage().getStackInSlot(slot);
			if(stack.isEmpty())
				return ItemStack.EMPTY;
			//Check if the settings limit our ability to remove this item
			if(this.limitOutputs())
			{
				if(!this.traderPurchases(stack))
					return ItemStack.EMPTY;
			}
			int emptyAmount = MathUtil.clamp(amount, 0, Math.min(stack.getCount(), stack.getMaxStackSize()));
			if(emptyAmount > 0)
			{
				ItemStack emptyStack = stack.copy();
				emptyStack.setCount(emptyAmount);
				if(!simulate)
				{
					//Remove the item from storage
					stack.shrink(emptyAmount);
					if(stack.isEmpty())
						this.getStorage().setInventorySlotContents(slot, ItemStack.EMPTY);
				}
				return emptyStack;
			}
			return ItemStack.EMPTY;
		}
		
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if(this.limitInputs()) //Only allow inputs if the item is being sold
				return this.traderSells(stack);
			return true;
		}
		
	}
	
	
}
