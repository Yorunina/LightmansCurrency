package io.github.lightman314.lightmanscurrency.proxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.BlockItemSet;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.ClientEvents;
import io.github.lightman314.lightmanscurrency.client.ClientTradingOffice;
import io.github.lightman314.lightmanscurrency.client.colors.TicketColor;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TeamManagerScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TradeRuleScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TradingTerminalScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.*;
import io.github.lightman314.lightmanscurrency.client.model.ModelWallet;
import io.github.lightman314.lightmanscurrency.client.renderer.entity.layers.WalletLayer;
import io.github.lightman314.lightmanscurrency.client.renderer.tileentity.*;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.core.ModBlocks;
import io.github.lightman314.lightmanscurrency.core.ModContainers;
import io.github.lightman314.lightmanscurrency.core.ModItems;
import io.github.lightman314.lightmanscurrency.core.ModTileEntities;
import io.github.lightman314.lightmanscurrency.items.CoinBlockItem;
import io.github.lightman314.lightmanscurrency.items.CoinItem;
import io.github.lightman314.lightmanscurrency.money.CoinData;
import io.github.lightman314.lightmanscurrency.money.MoneyUtil;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

public class ClientProxy extends CommonProxy{
	
	private long timeOffset = 0;
	
	@Override
	public void setupClient() {
		
		//Set Render Layers
    	RenderTypeLookup.setRenderLayer(ModBlocks.DISPLAY_CASE.block, RenderType.getCutout());
    	
    	setRenderLayerForSet(ModBlocks.VENDING_MACHINE1, RenderType.getCutout());
    	setRenderLayerForSet(ModBlocks.VENDING_MACHINE2, RenderType.getCutout());
    	
    	RenderTypeLookup.setRenderLayer(ModBlocks.ARMOR_DISPLAY.block, RenderType.getCutout());
    	
    	//Register Screens
    	ScreenManager.registerFactory(ModContainers.ATM, ATMScreen::new);
    	ScreenManager.registerFactory(ModContainers.MINT, MintScreen::new);
    	
    	ScreenManager.registerFactory(ModContainers.ITEM_TRADER, ItemTraderScreen::new);
    	ScreenManager.registerFactory(ModContainers.ITEM_TRADER_CR, ItemTraderScreen::new);
    	ScreenManager.registerFactory(ModContainers.ITEM_TRADER_UNIVERSAL, ItemTraderScreen::new);
    	
    	ScreenManager.registerFactory(ModContainers.ITEM_TRADER_STORAGE, ItemTraderStorageScreen::new);
    	ScreenManager.registerFactory(ModContainers.ITEM_TRADER_STORAGE_UNIVERSAL, ItemTraderStorageScreen::new);
    	
    	ScreenManager.registerFactory(ModContainers.ITEM_EDIT, ItemEditScreen::new);
    	ScreenManager.registerFactory(ModContainers.UNIVERSAL_ITEM_EDIT, ItemEditScreen::new);
    	
    	ScreenManager.registerFactory(ModContainers.WALLET, WalletScreen::new);
    	ScreenManager.registerFactory(ModContainers.PAYGATE, PaygateScreen::new);
    	ScreenManager.registerFactory(ModContainers.TICKET_MACHINE, TicketMachineScreen::new);
    	
    	//Register Tile Entity Renderers
    	ClientRegistry.bindTileEntityRenderer(ModTileEntities.ITEM_TRADER, ItemTraderTileEntityRenderer::new);
    	ClientRegistry.bindTileEntityRenderer(ModTileEntities.FREEZER_TRADER, FreezerTraderTileEntityRenderer::new);
    	
    	//Register Addable Trade Rules
    	TradeRuleScreen.RegisterTradeRule(() -> new PlayerWhitelist());
    	TradeRuleScreen.RegisterTradeRule(() -> new PlayerBlacklist());
    	TradeRuleScreen.RegisterTradeRule(() -> new PlayerTradeLimit());
    	TradeRuleScreen.RegisterTradeRule(() -> new PlayerDiscounts());
    	TradeRuleScreen.RegisterTradeRule(() -> new TimedSale());
    	TradeRuleScreen.RegisterTradeRule(() -> new TradeLimit());
    	
    	//Register ClientEvents
    	//MinecraftForge.EVENT_BUS.register(new ClientEvents());
    	
    	//Register the key bind
    	ClientRegistry.registerKeyBinding(ClientEvents.KEY_WALLET);
    	ClientRegistry.registerKeyBinding(ClientEvents.KEY_TEAM);
    	
    	//Add wallet layer
    	Map<String, PlayerRenderer> skinMap = Minecraft.getInstance().getRenderManager().getSkinMap();
    	this.addWalletLayer(skinMap.get("default"));
    	this.addWalletLayer(skinMap.get("slim"));
    	
    	
	}
	
	private static void setRenderLayerForSet(BlockItemSet<?> blockItemSet, RenderType type)
	{
		blockItemSet.getAll().forEach(blockItemPair -> RenderTypeLookup.setRenderLayer(blockItemPair.block, type));
	}
	
	private void addWalletLayer(PlayerRenderer renderer)
	{
		List<LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>>> layers = ObfuscationReflectionHelper.getPrivateValue(LivingRenderer.class, renderer, "field_177097_h");
		if(layers != null)
		{
			layers.add(new WalletLayer<>(renderer, new ModelWallet<>()));
		}
	}
	
	@Override
	public void clearClientTraders()
	{
		ClientTradingOffice.clearData();
	}
	
	@Override
	public void updateTrader(CompoundNBT compound)
	{
		ClientTradingOffice.updateTrader(compound);
	}
	
	@Override
	public void removeTrader(UUID traderID)
	{
		ClientTradingOffice.removeTrader(traderID);
	}
	
	@Override
	public void initializeTeams(CompoundNBT compound)
	{
		if(compound.contains("Teams", Constants.NBT.TAG_LIST))
		{
			List<Team> teams = Lists.newArrayList();
			ListNBT teamList = compound.getList("Teams", Constants.NBT.TAG_COMPOUND);
			teamList.forEach(nbt -> teams.add(Team.load((CompoundNBT)nbt)));
			ClientTradingOffice.initTeams(teams);
		}
	}
	
	@Override
	public void updateTeam(CompoundNBT compound)
	{
		ClientTradingOffice.updateTeam(compound);
	}
	
	@Override
	public void removeTeam(UUID teamID)
	{
		ClientTradingOffice.removeTeam(teamID);
	}
	
	@Override
	public void initializeBankAccounts(CompoundNBT compound)
	{
		if(compound.contains("BankAccounts", Constants.NBT.TAG_LIST))
		{
			Map<UUID,BankAccount> bank = new HashMap<>();
			ListNBT bankList = compound.getList("BankAccounts", Constants.NBT.TAG_COMPOUND);
			for(int i = 0; i < bankList.size(); ++i)
			{
				CompoundNBT tag = bankList.getCompound(i);
				UUID id = tag.getUniqueId("Player");
				BankAccount bankAccount = new BankAccount(tag);
				bank.put(id, bankAccount);
			}
			ClientTradingOffice.initBankAccounts(bank);
		}
	}
	
	@Override
	public void updateBankAccount(CompoundNBT compound)
	{
		ClientTradingOffice.updateBankAccount(compound);
	}
	
	@Override
	public void openTerminalScreen()
	{
		Minecraft.getInstance().displayGuiScreen(new TradingTerminalScreen());
	}
	
	@Override
	public void openTeamManager()
	{
		Minecraft.getInstance().displayGuiScreen(new TeamManagerScreen());
	}
	
	@Override
	public void createTeamResponse(UUID teamID)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.currentScreen instanceof TeamManagerScreen)
		{
			TeamManagerScreen screen = (TeamManagerScreen)minecraft.currentScreen;
			screen.setActiveTeam(teamID);
		}
	}
	
	@Override
	public long getTimeDesync()
	{
		return timeOffset;
	}
	
	@Override
	public void setTimeDesync(long serverTime)
	{
		this.timeOffset = serverTime - System.currentTimeMillis();
		//Round the time offset to the nearest second
		this.timeOffset = (timeOffset / 1000) * 1000;
		if(this.timeOffset < 10000) //Ignore offset if less than 10s, as it's likely due to ping
			this.timeOffset = 0;
	}
	
	@Override
	public void loadAdminPlayers(List<UUID> serverAdminList)
	{
		TradingOffice.loadAdminPlayers(serverAdminList);
	}
	
	
	public void registerItemColors(ColorHandlerEvent.Item event)
	{
		LightmansCurrency.LogInfo("Registering Item Colors for Ticket Items");
		event.getItemColors().register(new TicketColor(), ModItems.TICKET, ModItems.TICKET_MASTER);
	}
	
	@SubscribeEvent
	//Add coin value tooltips to non CoinItem coins.
	public void onItemTooltip(ItemTooltipEvent event) {
		Item item = event.getItemStack().getItem();
		CoinData coinData = MoneyUtil.getData(item);
		if(coinData != null && !(item instanceof CoinItem || item instanceof CoinBlockItem))
		{
			CoinItem.addCoinTooltips(event.getItemStack(), event.getToolTip());
		}
	}
	
}
