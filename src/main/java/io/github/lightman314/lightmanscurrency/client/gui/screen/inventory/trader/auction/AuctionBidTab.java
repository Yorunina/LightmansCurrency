package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.trader.auction;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.trader.TraderClientTab;
import io.github.lightman314.lightmanscurrency.client.gui.widget.CoinValueInput;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.TraderSaveData;
import io.github.lightman314.lightmanscurrency.common.traders.auction.AuctionHouseTrader;
import io.github.lightman314.lightmanscurrency.common.traders.auction.tradedata.AuctionTradeData;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.auction.MessageSubmitBid;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TextFormatting;

public class AuctionBidTab extends TraderClientTab {

	private final long auctionHouseID;
	private final int tradeIndex;
	
	private AuctionHouseTrader getAuctionHouse() {
		TraderData data = TraderSaveData.GetTrader(true, this.auctionHouseID);
		if(data instanceof AuctionHouseTrader)
			return (AuctionHouseTrader)data;
		return null;
	}
	
	private AuctionTradeData getTrade() {
		AuctionHouseTrader trader = this.getAuctionHouse();
		if(trader != null)
			return trader.getTrade(this.tradeIndex);
		return null;
	}
	
	public AuctionBidTab(TraderScreen screen, long auctionHouseID, int tradeIndex) { super(screen); this.auctionHouseID = auctionHouseID; this.tradeIndex = tradeIndex; }

	@Override
	public boolean blockInventoryClosing() { return false; }
	
	//Auction Bid Display
	TradeButton tradeDisplay;
	
	//Bid Amount Input
	CoinValueInput bidAmount;
	
	//Bid Button
	Button bidButton;
	
	Button closeButton;
	
	@Override
	public void onOpen() {
		
		if(this.getTrade() == null)
			return;
		
		this.tradeDisplay = this.screen.addRenderableTabWidget(new TradeButton(() -> this.menu.getContext(this.getAuctionHouse()), this::getTrade, b -> {}));
		this.tradeDisplay.move(this.screen.getGuiLeft() + this.screen.getXSize() / 2 - this.tradeDisplay.getWidth() / 2, this.screen.getGuiTop() + 5);
		
		this.bidAmount = this.screen.addRenderableTabWidget(new CoinValueInput(this.screen.getGuiLeft() + this.screen.getXSize() / 2 - CoinValueInput.DISPLAY_WIDTH / 2, this.screen.getGuiTop() + 10 + this.tradeDisplay.getHeight(), EasyText.translatable("gui.lightmanscurrency.auction.bidamount"), this.getTrade().getMinNextBid(), this.font, v -> {}, this.screen::addRenderableTabWidget));
		this.bidAmount.init();
		this.bidAmount.allowFreeToggle = false;
		this.bidAmount.drawBG = false;
		
		this.bidButton = this.screen.addRenderableTabWidget(new Button(this.screen.getGuiLeft() + 22, this.screen.getGuiTop() + 119, 68, 20, EasyText.translatable("gui.lightmanscurrency.auction.bid"), this::SubmitBid));
		
		this.closeButton = this.screen.addRenderableTabWidget(new Button(this.screen.getGuiLeft() + this.screen.getXSize() - 25, this.screen.getGuiTop() + 5, 20, 20, EasyText.literal("X").withStyle(TextFormatting.RED).withStyle(TextFormatting.BOLD), this::close));
		
		this.tick();
		
	}
	
	@Override
	public void renderBG(MatrixStack pose, int mouseX, int mouseY, float partialTicks) { }

	@Override
	public void renderTooltips(MatrixStack pose, int mouseX, int mouseY) {
		
		this.tradeDisplay.renderTooltips(pose, mouseX, mouseY);
		
	}
	
	@Override
	public void tick() {
		if(this.getTrade() == null)
		{
			this.screen.closeTab();
			return;
		}
		
		if(this.bidAmount != null)
		{
			long bidQuery = this.bidAmount.getCoinValue().getRawValue();
			CoinValue minBid = this.getTrade().getMinNextBid();
			if(bidQuery < minBid.getRawValue())
				this.bidAmount.setCoinValue(this.getTrade().getMinNextBid());
			this.bidButton.active = this.menu.getContext(this.getAuctionHouse()).getAvailableFunds() >= bidQuery;
			
			this.bidAmount.tick();
		}
		
	}
	
	private void SubmitBid(Button button) {
		LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSubmitBid(this.auctionHouseID, this.tradeIndex, this.bidAmount.getCoinValue()));
		this.screen.closeTab();
	}
	
	private void close(Button button) { this.screen.closeTab(); }
	
}