package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import io.github.lightman314.lightmanscurrency.client.gui.screen.TradeItemPriceScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TradeRuleScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TraderSettingsScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TextLogWindow;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.IconButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.ItemTradeButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.common.ItemTraderStorageUtil;
import io.github.lightman314.lightmanscurrency.containers.ItemTraderStorageContainer;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.item_trader.MessageSetTradeItem;
import io.github.lightman314.lightmanscurrency.network.message.logger.MessageClearLogger;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageCollectCoins;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageOpenStorage;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageOpenTrades;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageStoreCoins;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.settings.Settings;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ItemTraderStorageScreen extends ContainerScreen<ItemTraderStorageContainer>{

	public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(LightmansCurrency.MODID, "textures/gui/container/traderstorage.png");
	public static final ResourceLocation ALLY_GUI_TEXTURE = TradeRuleScreen.GUI_TEXTURE;
	
	public static final int SCREEN_EXTENSION = ItemTraderStorageUtil.SCREEN_EXTENSION;
	
	Button buttonShowTrades;
	Button buttonCollectMoney;
	Button buttonOpenSettings;
	
	Button buttonStoreMoney;
	
	Button buttonShowLog;
	Button buttonClearLog;
	
	TextLogWindow logWindow;
	
	Button buttonTradeRules;
	
	List<Button> tradePriceButtons = new ArrayList<>();
	
	public ItemTraderStorageScreen(ItemTraderStorageContainer container, PlayerInventory inventory, ITextComponent title)
	{
		super(container, inventory, title);
		int tradeCount = this.container.tileEntity.getTradeCount();
		this.ySize = 18 * ItemTraderStorageUtil.getRowCount(tradeCount) + 125;
		this.xSize = ItemTraderStorageUtil.getWidth(tradeCount);
		
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY)
	{
		
		drawTraderStorageBackground(matrix, this, this.font, this.container, this.container.tileEntity, this.minecraft, this.xSize, this.ySize);
		
	}
	
	@SuppressWarnings("deprecation")
	public static void drawTraderStorageBackground(MatrixStack matrix, Screen screen, FontRenderer font, Container container, IItemTrader trader, Minecraft minecraft, int xSize, int ySize)
	{
		
		List<ItemTradeData> trades = trader.getAllTrades();
		int tradeCount = trades.size();
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
		int startX = (screen.width - xSize) / 2;
		int startY = (screen.height - ySize) / 2;
		
		int rowCount = ItemTraderStorageUtil.getRowCount(tradeCount);
		int columnCount = ItemTraderStorageUtil.getColumnCount(tradeCount);
		
		//Top-left corner
		screen.blit(matrix, startX + SCREEN_EXTENSION, startY, 0, 0, 7, 17);
		for(int x = 0; x < columnCount; x++)
		{
			//Draw the top
			screen.blit(matrix, startX + SCREEN_EXTENSION + 7 + (x * 162), startY, 7, 0, 162, 17);
		}
		//Top-right corner
		screen.blit(matrix, startX + xSize - SCREEN_EXTENSION - 7, startY, 169, 0, 7, 17);
		
		//Draw the storage rows
		int index = 0;
		for(int y = 0; y < rowCount; y++)
		{
			//Left edge
			screen.blit(matrix, startX + SCREEN_EXTENSION, startY + 17 + 18 * y, 0, 17, 7, 18);
			if(y < rowCount - 1 || tradeCount % ItemTraderStorageUtil.getColumnCount(tradeCount) == 0 || y == 0)
			{
				for(int x = 0; x < columnCount; x++)
				{
					if(index < tradeCount)
						screen.blit(matrix, startX + SCREEN_EXTENSION + 7 + (x * 162), startY + 17 + 18 * y, 7, 17, 162, 18);
					else
						screen.blit(matrix, startX + SCREEN_EXTENSION + 7 + (x * 162), startY + 17 + 18 * y, 0, 143, 162, 18);
					index++;
				}
			}
			else //Trade Count is NOT even, and is on the last row, AND is not the first row
			{
				for(int x = 0; x < columnCount; x++)
				{
					//Draw a blank background
					screen.blit(matrix, startX + SCREEN_EXTENSION + 7 + (x * 162), startY + 17 + 18 * y, 0, 143, 162, 18);
				}
				screen.blit(matrix, startX + SCREEN_EXTENSION + 7 + ItemTraderStorageUtil.getStorageSlotOffset(tradeCount, y), startY + 17 + 18 * y, 7, 17, 162, 18);
			}
			//Right edge
			screen.blit(matrix, startX + xSize - SCREEN_EXTENSION - 7, startY + 17 + 18 * y, 169, 17, 7, 18);
		}
		
		//Bottom-left corner
		screen.blit(matrix, startX + SCREEN_EXTENSION, startY + 17 + 18 * rowCount, 0, 35, 7, 8);
		for(int x = 0; x < columnCount; x++)
		{
			//Draw the bottom
			screen.blit(matrix, startX + SCREEN_EXTENSION + 7 + (x * 162), startY + 17 + 18 * rowCount, 7, 35, 162, 8);
		}
		//Bottom-right corner
		screen.blit(matrix, startX + xSize - SCREEN_EXTENSION - 7, startY + 17 + 18 * rowCount, 169, 35, 7, 8);
		
		//Draw the bottom
		screen.blit(matrix, startX + SCREEN_EXTENSION + ItemTraderStorageUtil.getInventoryOffset(tradeCount), startY + 25 + rowCount * 18, 0, 43, 176 + 32, 100);
		
		//Render the fake trade buttons
		minecraft.getTextureManager().bindTexture(ItemTradeButton.TRADE_TEXTURES);
		for(int i = 0; i < tradeCount; i++)
		{
			boolean inverted = ItemTraderStorageUtil.isFakeTradeButtonInverted(tradeCount, i);
			ItemTradeButton.renderItemTradeButton(matrix, screen, font, startX + ItemTraderStorageUtil.getFakeTradeButtonPosX(tradeCount, i), startY + ItemTraderStorageUtil.getFakeTradeButtonPosY(tradeCount, i), i, trader, null, false, true, inverted);
			//if(inverted)
			//	screen.blit(matrix, startX + ItemTraderStorageUtil.getFakeTradeButtonPosX(tradeCount, i), startY + ItemTraderStorageUtil.getFakeTradeButtonPosY(tradeCount, i), ItemTradeButton.WIDTH, yOffset, ItemTradeButton.WIDTH, ItemTradeButton.HEIGHT);
			//else
			//	screen.blit(matrix, startX + ItemTraderStorageUtil.getFakeTradeButtonPosX(tradeCount, i), startY + ItemTraderStorageUtil.getFakeTradeButtonPosY(tradeCount, i), 0, yOffset, ItemTradeButton.WIDTH, ItemTradeButton.HEIGHT);
		}
		
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY)
	{
		
		drawTraderStorageForeground(matrix, this.font, this.container.tileEntity.getTradeCount(), this.ySize, this.container.tileEntity.getName(), this.playerInventory.getDisplayName());
		
	}
	
	//Trade count is now only required to get the proper positioning for the inventory label
	public static void drawTraderStorageForeground(MatrixStack matrix, FontRenderer font, int tradeCount, int ySize, ITextComponent title, ITextComponent inventoryTitle)
	{
		
		font.drawString(matrix, title.getString(), 8.0f + SCREEN_EXTENSION, 6.0f, 0x404040);

		font.drawString(matrix, inventoryTitle.getString(), ItemTraderStorageUtil.getInventoryOffset(tradeCount) + 8.0f + SCREEN_EXTENSION, (ySize - 94), 0x404040);
		
	}
	
	@Override
	protected void init()
	{
		super.init();
		
		this.buttonShowTrades = this.addButton(new IconButton(this.guiLeft + SCREEN_EXTENSION, this.guiTop - 20, this::PressTradesButton, this.font, IconData.of(GUI_TEXTURE, 176, 0)));
		
		this.buttonCollectMoney = this.addButton(new IconButton(this.guiLeft + SCREEN_EXTENSION + 20, this.guiTop - 20, this::PressCollectionButton, this.font, IconData.of(GUI_TEXTURE, 176 + 16, 0)));
		this.buttonCollectMoney.active = false;
		this.buttonCollectMoney.visible = this.container.hasPermissions(Permissions.COLLECT_COINS);
		this.buttonShowLog = this.addButton(new Button(this.guiLeft + SCREEN_EXTENSION + 40, this.guiTop - 20, 20, 20, new TranslationTextComponent("gui.button.lightmanscurrency.showlog"), this::PressLogButton));
		this.buttonClearLog = this.addButton(new Button(this.guiLeft + SCREEN_EXTENSION + 60, this.guiTop - 20, 20, 20, new TranslationTextComponent("gui.button.lightmanscurrency.clearlog"), this::PressClearLogButton));
		
		
		
		int tradeCount = this.container.tileEntity.getTradeCount();
		this.buttonStoreMoney = this.addButton(new IconButton(this.guiLeft + SCREEN_EXTENSION + ItemTraderStorageUtil.getInventoryOffset(tradeCount) + 176 + 32, this.guiTop + 25 + ItemTraderStorageUtil.getRowCount(tradeCount) * 18, this::PressStoreCoinsButton, this.font, IconData.of(GUI_TEXTURE, 176, 16)));
		this.buttonStoreMoney.visible = false;
		
		
		this.buttonOpenSettings = this.addButton(new IconButton(this.guiLeft + this.xSize - SCREEN_EXTENSION - 20, this.guiTop - 20, this::PressSettingsButton, this.font, IconData.of(GUI_TEXTURE, 176 + 32, 0)));
		this.buttonOpenSettings.visible = this.container.hasPermissions(Permissions.EDIT_SETTINGS);
		
		this.buttonTradeRules = this.addButton(new IconButton(this.guiLeft + this.xSize - SCREEN_EXTENSION - 40, this.guiTop - 20, this::PressTradeRulesButton, this.font, IconData.of(GUI_TEXTURE, 176 + 16, 16)));
		this.buttonTradeRules.visible = this.container.hasPermissions(Permissions.EDIT_TRADE_RULES);
		
		this.logWindow = new TextLogWindow(this.guiLeft + (this.xSize / 2) - (TextLogWindow.WIDTH / 2), this.guiTop, () -> this.container.tileEntity.getLogger(), this.font);
		this.addListener(this.logWindow);
		this.logWindow.visible = false;
		
		for(int i = 0; i < tradeCount; i++)
		{
			tradePriceButtons.add(this.addButton(new Button(this.guiLeft + ItemTraderStorageUtil.getTradePriceButtonPosX(tradeCount, i), this.guiTop + ItemTraderStorageUtil.getTradePriceButtonPosY(tradeCount, i), 20, 20, new TranslationTextComponent("gui.button.lightmanscurrency.dollarsign"), this::PressTradePriceButton)));
		}
		
		tick();
		
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(matrixStack);
		if(this.logWindow.visible)
		{
			this.logWindow.render(matrixStack, mouseX, mouseY, partialTicks);
			this.buttonShowLog.render(matrixStack, mouseX, mouseY, partialTicks);
			if(this.buttonClearLog.visible)
				this.buttonClearLog.render(matrixStack, mouseX, mouseY, partialTicks);
			if(this.buttonShowLog.isMouseOver(mouseX, mouseY))
				this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.log.hide"), mouseX, mouseY);
			else if(this.buttonClearLog.isMouseOver(mouseX, mouseY))
				this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.log.clear"), mouseX, mouseY);
			return;
		}
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		this.renderHoveredTooltip(matrixStack, mouseX,  mouseY);
		
		if(this.buttonShowTrades.isMouseOver(mouseX,mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.opentrades"), mouseX, mouseY);
		}
		else if(this.buttonCollectMoney.active && this.buttonCollectMoney.isMouseOver(mouseX, mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.collectcoins", this.container.tileEntity.getStoredMoney().getString()), mouseX, mouseY);
		}
		else if(this.buttonStoreMoney.isMouseOver(mouseX, mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.storecoins"), mouseX, mouseY);
		}
		else if(this.buttonShowLog.isMouseOver(mouseX, mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.log.show"), mouseX, mouseY);
		}
		else if(this.buttonClearLog.isMouseOver(mouseX, mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.log.clear"), mouseX, mouseY);
		}
		else if(this.buttonTradeRules.isMouseOver(mouseX, mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.traderules"), mouseX, mouseY);
		}
		else if(this.buttonOpenSettings.isMouseOver(mouseX, mouseY))
		{
			this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.settings"), mouseX, mouseY);
		}
		else if(this.container.player.inventory.getItemStack().isEmpty())
		{
			int tradeCount = this.container.tileEntity.getTradeCount();
			for(int i = 0; i < this.container.tileEntity.getTradeCount(); i++)
			{
				boolean inverted = ItemTraderStorageUtil.isFakeTradeButtonInverted(tradeCount, i);
				int result = ItemTradeButton.tryRenderTooltip(matrixStack, this, i, this.container.tileEntity, this.guiLeft + ItemTraderStorageUtil.getFakeTradeButtonPosX(tradeCount, i), this.guiTop + ItemTraderStorageUtil.getFakeTradeButtonPosY(tradeCount, i), inverted, mouseX, mouseY, null);
				if(result < 0) //Result is negative if the mouse is over a slot, but the slot is empty.
					this.renderTooltip(matrixStack, new TranslationTextComponent("tooltip.lightmanscurrency.trader.item_edit"), mouseX, mouseY);
			}
		}
	}
	
	@Override
	public void tick()
	{
		if(!this.container.hasPermissions(Permissions.OPEN_STORAGE))
		{
			this.container.player.closeScreen();
			return;
		}
		super.tick();
		
		this.container.tick();
		
		this.buttonCollectMoney.visible = (!this.container.tileEntity.getCoreSettings().isCreative() || this.container.tileEntity.getStoredMoney().getRawValue() > 0) && this.container.hasPermissions(Permissions.COLLECT_COINS);
		this.buttonCollectMoney.active = this.container.tileEntity.getStoredMoney().getRawValue() > 0;
		
		this.buttonOpenSettings.visible = this.container.hasPermissions(Permissions.EDIT_SETTINGS);
		
		this.buttonStoreMoney.visible = this.container.HasCoinsToAdd();
		this.buttonClearLog.visible = this.container.tileEntity.getLogger().logText.size() > 0 && this.container.hasPermissions(Permissions.CLEAR_LOGS);
		
	}
	
	//0 for left-click. 1 for right-click
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		ItemStack heldItem = this.container.player.inventory.getItemStack();
		int tradeCount = this.container.tileEntity.getTradeCount();
		for(int i = 0; i < tradeCount; ++i)
		{
			ItemTradeData trade = this.container.tileEntity.getTrade(i);
			if(ItemTradeButton.isMouseOverSlot(0, this.guiLeft + ItemTraderStorageUtil.getFakeTradeButtonPosX(tradeCount, i), this.guiTop + ItemTraderStorageUtil.getFakeTradeButtonPosY(tradeCount, i), (int)mouseX, (int)mouseY, ItemTraderStorageUtil.isFakeTradeButtonInverted(tradeCount, i)))
			{
				ItemStack currentSellItem = trade.getSellItem();
				//LightmansCurrency.LogInfo("Clicked on sell item. Click Type: " + button + "; Sell Item: " + currentSellItem.getCount() + "x" + currentSellItem.getItem().getRegistryName() + "; Held Item: " + heldItem.getCount() + "x" + heldItem.getItem().getRegistryName());
				if(heldItem.isEmpty() && currentSellItem.isEmpty())
				{
					//Open the item edit screen if both the sell item and held items are empty
					this.container.openItemEditScreenForTrade(i);
					return true;
				}
				else if(heldItem.isEmpty())
				{
					//If held item is empty, right-click to decrease by 1, left-click to empty
					currentSellItem.shrink(button == 0 ? currentSellItem.getCount() : 1);
					if(currentSellItem.getCount() <= 0)
						currentSellItem = ItemStack.EMPTY;
					trade.setSellItem(currentSellItem);
					LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetTradeItem(this.container.tileEntity.getPos(), i, currentSellItem, 0));
					return true;
				}
				else
				{
					//If the held item is empty, right-click to increase by 1, left click to set to current held count
					if(button == 1)
					{
						if(InventoryUtil.ItemMatches(currentSellItem, heldItem))
						{
							if(currentSellItem.getCount() < currentSellItem.getMaxStackSize()) //Limit to the max stack size.
								currentSellItem.grow(1);
						}
						else
						{
							currentSellItem = heldItem.copy();
							currentSellItem.setCount(1);
						}
					}
					else
						currentSellItem = heldItem.copy();
					trade.setSellItem(currentSellItem);
					LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetTradeItem(this.container.tileEntity.getPos(), i, currentSellItem, 0));
					return true;
				}
			}
			else if(this.container.tileEntity.getTrade(i).isBarter() && ItemTradeButton.isMouseOverSlot(1, this.guiLeft + ItemTraderStorageUtil.getFakeTradeButtonPosX(tradeCount, i), this.guiTop + ItemTraderStorageUtil.getFakeTradeButtonPosY(tradeCount, i), (int)mouseX, (int)mouseY, ItemTraderStorageUtil.isFakeTradeButtonInverted(tradeCount, i)))
			{
				ItemStack currentBarterItem = trade.getBarterItem();
				if(heldItem.isEmpty() && currentBarterItem.isEmpty())
				{
					//Open the item edit screen if both the sell item and held items are empty
					this.container.openItemEditScreenForTrade(i);
					return true;
				}
				else if(heldItem.isEmpty())
				{
					//If held item is empty, right-click to decrease by 1, left-click to empty
					currentBarterItem.shrink(button == 0 ? currentBarterItem.getCount() : 1);
					if(currentBarterItem.getCount() <= 0)
						currentBarterItem = ItemStack.EMPTY;
					trade.setBarterItem(currentBarterItem);
					LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetTradeItem(this.container.tileEntity.getPos(), i, currentBarterItem, 1));
					return true;
				}
				else
				{
					//If the held item is not empty, right-click to increase by 1, left click to set to current held count
					if(button == 1)
					{
						if(InventoryUtil.ItemMatches(currentBarterItem, heldItem))
						{
							if(currentBarterItem.getCount() < currentBarterItem.getMaxStackSize())
								currentBarterItem.grow(1);
						}
						else
						{
							currentBarterItem = heldItem.copy();
							currentBarterItem.setCount(1);
						}
					}
					else
						currentBarterItem = heldItem.copy();
					trade.setBarterItem(currentBarterItem);
					LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetTradeItem(this.container.tileEntity.getPos(), i, currentBarterItem, 1));
					return true;
				}
			}
		}
		//LightmansCurrency.LogInfo("Did not click on any trade definition slots.");
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	private void PressTradesButton(Button button)
	{
		LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenTrades(this.container.tileEntity.getPos()));
	}
	
	private void PressCollectionButton(Button button)
	{
		//Open the container screen
		if(container.hasPermissions(Permissions.COLLECT_COINS))
		{
			//CurrencyMod.LOGGER.info("Owner attempted to collect the stored money.");
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageCollectCoins());
		}
		else
			Settings.PermissionWarning(this.container.player, "collect stored coins", Permissions.COLLECT_COINS);
	}
	
	private void PressStoreCoinsButton(Button button)
	{
		if(container.hasPermissions(Permissions.STORE_COINS))
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageStoreCoins());
		}
		else
			Settings.PermissionWarning(this.container.player, "store coins", Permissions.STORE_COINS);
	}
	
	private void PressTradePriceButton(Button button)
	{
		if(!this.container.hasPermissions(Permissions.EDIT_TRADES))
			return;
		
		int tradeIndex = 0;
		if(tradePriceButtons.contains(button))
			tradeIndex = tradePriceButtons.indexOf(button);
		
		this.minecraft.displayGuiScreen(new TradeItemPriceScreen(this.container.tileEntity, tradeIndex, this.playerInventory.player));
		
	}
	
	private void PressLogButton(Button button)
	{
		this.logWindow.visible = !this.logWindow.visible;
	}
	
	private void PressClearLogButton(Button button)
	{
		LightmansCurrencyPacketHandler.instance.sendToServer(new MessageClearLogger(this.container.tileEntity.getPos()));
	}
	
	private void PressTradeRulesButton(Button button)
	{
		Minecraft.getInstance().displayGuiScreen(new TradeRuleScreen(this.container.tileEntity.GetRuleScreenBackHandler()));
	}
	
	private void PressSettingsButton(Button button)
	{
		this.container.player.closeScreen();
		Minecraft.getInstance().displayGuiScreen(new TraderSettingsScreen(() -> this.container.tileEntity, (player) ->LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenStorage(this.container.tileEntity.getPos()))));
	}
	
}
