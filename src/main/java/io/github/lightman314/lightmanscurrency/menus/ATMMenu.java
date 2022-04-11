package io.github.lightman314.lightmanscurrency.menus;

import io.github.lightman314.lightmanscurrency.core.ModMenus;

import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount.AccountReference;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount.AccountType;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount.IBankAccountTransferMenu;
import io.github.lightman314.lightmanscurrency.core.ModItems;
import io.github.lightman314.lightmanscurrency.menus.slots.CoinSlot;
import io.github.lightman314.lightmanscurrency.money.MoneyUtil;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ATMMenu extends AbstractContainerMenu implements IBankAccountTransferMenu{
	
	private Player player;
	public Player getPlayer() { return this.player; }
	
	private final Container coinInput = new SimpleContainer(9);
	public Container getCoinInput() { return this.coinInput; }
	
	private AccountReference accountSource = null;
	public BankAccount getAccount() { if(this.accountSource == null) return null; return this.accountSource.get(); }
	
	private Component transferMessage = new TextComponent("");
	
	public ATMMenu(int windowId, Inventory inventory)
	{
		super(ModMenus.ATM, windowId);
		
		this.player = inventory.player;
		//Auto-select the players bank account for now.
		this.accountSource = BankAccount.GenerateReference(this.player.level.isClientSide, AccountType.Player, this.player.getUUID());
		
		//Coinslots
		for(int x = 0; x < coinInput.getContainerSize(); x++)
		{
			this.addSlot(new CoinSlot(this.coinInput, x, 8 + x * 18, 98, false));
		}
		
		//Player inventory
		for(int y = 0; y < 3; y++)
		{
			for(int x = 0; x < 9; x++)
			{
				this.addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 130 + y * 18));
			}
		}
		//Player hotbar
		for(int x = 0; x < 9; x++)
		{
			this.addSlot(new Slot(inventory, x, 8 + x * 18, 188));
		}
	}
	
	@Override
	public boolean stillValid(Player playerIn) { return true; }
	
	@Override
	public void removed(Player playerIn)
	{
		super.removed(playerIn);
		this.clearContainer(playerIn,  this.coinInput);
	}
	
	@Override
	public ItemStack quickMoveStack(Player playerEntity, int index)
	{
		
		ItemStack clickedStack = ItemStack.EMPTY;
		
		Slot slot = this.slots.get(index);
		
		if(slot != null && slot.hasItem())
		{
			ItemStack slotStack = slot.getItem();
			clickedStack = slotStack.copy();
			if(index < this.coinInput.getContainerSize())
			{
				if(MoneyUtil.isCoin(slotStack.getItem()))
				{
					if(!this.moveItemStackTo(slotStack,  this.coinInput.getContainerSize(), this.slots.size(), true))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			else if(!this.moveItemStackTo(slotStack, 0, this.coinInput.getContainerSize(), false))
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
	
	//Button Input Codes:
	//100:Convert All Up
	//1:Copper -> Iron			-1:Iron -> Copper
	//2:Iron -> Gold			-2:Gold -> Iron
	//3:Gold -> Emerald			-3:Emerald -> Gold
	//4:Emerald -> Diamond		-4:Diamond -> Emerald
	//5:Diamond -> Netherite	-5: Netherite -> Diamond
	//-100: Convert all down
	public void ConvertCoins(int buttonInput)
	{
		///Converting Upwards
		//Converting All Upwards
		if(buttonInput == 100)
		{
			//Run two passes
			MoneyUtil.ConvertAllCoinsUp(this.coinInput);
		}
		//Copper to Iron
		else if(buttonInput == 1)
		{
			MoneyUtil.ConvertCoinsUp(this.coinInput, ModItems.COIN_COPPER);
		}
		//Iron to Gold
		else if(buttonInput == 2)
		{
			MoneyUtil.ConvertCoinsUp(this.coinInput, ModItems.COIN_IRON);
		}
		//Gold to Emerald
		else if(buttonInput == 3)
		{
			MoneyUtil.ConvertCoinsUp(this.coinInput, ModItems.COIN_GOLD);
		}
		//Emerald to Diamond
		else if(buttonInput == 4)
		{
			MoneyUtil.ConvertCoinsUp(this.coinInput, ModItems.COIN_EMERALD);
		}
		//Diamond to Netherite
		else if(buttonInput == 5)
		{
			MoneyUtil.ConvertCoinsUp(this.coinInput, ModItems.COIN_DIAMOND);
		}
		///Converting Downwards
		//Converting All Downwards
		else if(buttonInput == -100)
		{
			MoneyUtil.ConvertAllCoinsDown(this.coinInput);
		}
		//Netherite to Diamond
		else if(buttonInput == -5)
		{
			MoneyUtil.ConvertCoinsDown(this.coinInput, ModItems.COIN_NETHERITE);
		}
		//Netherite to Diamond
		if(buttonInput == -4)
		{
			MoneyUtil.ConvertCoinsDown(this.coinInput, ModItems.COIN_DIAMOND);
		}
		//Netherite to Diamond
		if(buttonInput == -3)
		{
			MoneyUtil.ConvertCoinsDown(this.coinInput, ModItems.COIN_EMERALD);
		}
		//Netherite to Diamond
		if(buttonInput == -2)
		{
			MoneyUtil.ConvertCoinsDown(this.coinInput, ModItems.COIN_GOLD);
		}
		//Netherite to Diamond
		if(buttonInput == -1)
		{
			MoneyUtil.ConvertCoinsDown(this.coinInput, ModItems.COIN_IRON);
		}
		
	}
	
	public void SetAccount(AccountReference account)
	{
		this.accountSource = account;
	}
	
	public Pair<AccountReference,Component> SetPlayerAccount(String playerName) {
		
		if(TradingOffice.isAdminPlayer(this.player))
		{
			PlayerReference accountPlayer = PlayerReference.of(playerName);
			if(accountPlayer != null)
			{
				this.accountSource = BankAccount.GenerateReference(false, accountPlayer);
				return Pair.of(this.accountSource, new TranslatableComponent("gui.bank.select.player.success", accountPlayer.lastKnownName()));
			}
			else
				return Pair.of(null, new TranslatableComponent("gui.bank.transfer.error.null.to"));
		}
		return Pair.of(null, new TextComponent("ERROR"));
	}

	@Override
	public AccountReference getAccountSource() {
		return this.accountSource;
	}
	
	@Override
	public Component getLastMessage() { return this.transferMessage; }
	
	@Override
	public void setMessage(Component message) { this.transferMessage = message; }
	
}
