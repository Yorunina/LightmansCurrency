package io.github.lightman314.lightmanscurrency.common.traders.tradedata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.lightman314.lightmanscurrency.common.taxes.TaxEntry;
import io.github.lightman314.lightmanscurrency.common.traders.TradeContext;
import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.rules.ITradeRuleHost;
import io.github.lightman314.lightmanscurrency.common.traders.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.client.TradeRenderManager;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.comparison.TradeComparisonResult;
import io.github.lightman314.lightmanscurrency.common.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.common.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.common.events.TradeEvent.TradeCostEvent;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.trades_basic.BasicTradeEditTab;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public abstract class TradeData implements ITradeRuleHost {

	public static final String DEFAULT_KEY = "Trades";

	public enum TradeDirection { SALE(0,1), PURCHASE(1,0), NONE(-1,0);
		public final int index;
		private final int nextIndex;
		public final TradeDirection next() { return fromIndex(this.nextIndex); }
		TradeDirection(int index, int nextIndex) { this.index = index; this.nextIndex = nextIndex; }
		public static TradeDirection fromIndex(int index) {
			for(TradeDirection d : TradeDirection.values())
			{
				if(d.index == index)
					return d;
			}
			return TradeDirection.SALE;
		}
	}

	protected CoinValue cost = CoinValue.EMPTY;

	List<TradeRule> rules = new ArrayList<>();

	public abstract TradeDirection getTradeDirection();

	public boolean validCost()
	{
		return this.getCost().isFree() || this.getCost().getValueNumber() > 0;
	}

	public boolean isValid() { return validCost(); }

	public CoinValue getCost() { return this.cost; }

	public CoinValue getCost(TradeContext context)
	{
		if(context.hasTrader() && context.hasPlayerReference())
			return context.getTrader().runTradeCostEvent(context.getPlayerReference(), this).getCostResult();
		return this.getCost();
	}

	/**
	 * Gets the cost after all taxes are applied.
	 * Assumes that this is a purchase trade.
	 */
	public CoinValue getCostWithTaxes(TraderData trader)
	{
		CoinValue cost = this.cost;
		long taxAmount = 0;
		for(TaxEntry entry : trader.getApplicableTaxes())
			taxAmount += cost.percentageOfValue(entry.getTaxPercentage()).getValueNumber();
		return cost.plusValue(CoinValue.fromNumber(taxAmount));
	}
	public CoinValue getCostWithTaxes(TradeContext context)
	{
		CoinValue cost = this.getCost(context);
		if(context.hasTrader())
		{
			TraderData trader = context.getTrader();
			long taxAmount = 0;
			for(TaxEntry entry : trader.getApplicableTaxes())
				taxAmount += cost.percentageOfValue(entry.getTaxPercentage()).getValueNumber();
			return cost.plusValue(CoinValue.fromNumber(taxAmount));
		}
		return cost;
	}

	public void setCost(CoinValue value) { this.cost = value; }

	public final int stockCountOfCost(TraderData trader)
	{
		if(this.cost.isFree())
			return 1;
		if(this.cost.getValueNumber() == 0)
			return 0;
		long coinValue = trader.getStoredMoney().getValueNumber();
		long price = this.getCostWithTaxes(trader).getValueNumber();
		return (int)(coinValue / price);
	}

	public final int stockCountOfCost(TradeContext context)
	{
		if(!context.hasTrader())
			return 0;

		TraderData trader = context.getTrader();

		if(this.cost.isFree())
			return 1;
		if(this.cost.getValueNumber() == 0)
			return 0;
		long coinValue = trader.getStoredMoney().getValueNumber();
		long price = this.getCostWithTaxes(context).getValueNumber();
		return (int) MathUtil.SafeDivide(coinValue, price, 1);
	}

	private final boolean validateRules;

	protected TradeData(boolean validateRules) {
		this.validateRules = validateRules;
		if(this.validateRules)
			TradeRule.ValidateTradeRuleList(this.rules, this);
	}

	public CompoundTag getAsNBT()
	{
		CompoundTag tradeNBT = new CompoundTag();
		tradeNBT.put("Price", this.cost.save());
		TradeRule.saveRules(tradeNBT, this.rules, "RuleData");

		return tradeNBT;
	}

	protected void loadFromNBT(CompoundTag nbt)
	{
		this.cost = CoinValue.safeLoad(nbt, "Price");
		//Set whether it's free or not
		if(nbt.contains("IsFree") && nbt.getBoolean("IsFree"))
			this.cost = CoinValue.FREE;

		this.rules.clear();
		if(nbt.contains("TradeRules"))
		{
			this.rules = TradeRule.loadRules(nbt, "TradeRules", this);
			for(TradeRule r : this.rules) r.setActive(true);
		}
		else
			this.rules = TradeRule.loadRules(nbt, "RuleData", this);

		if(this.validateRules)
			TradeRule.ValidateTradeRuleList(this.rules, this);

	}

	@Override
	public final boolean isTrader() { return false; }

	@Override
	public final boolean isTrade() { return true; }

	@Override
	public boolean allowTradeRule(@Nonnull TradeRule rule) { return true; }

	public void beforeTrade(PreTradeEvent event) {
		for(TradeRule rule : this.rules)
		{
			if(rule.isActive())
				rule.beforeTrade(event);
		}
	}

	public void tradeCost(TradeCostEvent event)
	{
		for(TradeRule rule : this.rules)
		{
			if(rule.isActive())
				rule.tradeCost(event);
		}
	}

	public void afterTrade(PostTradeEvent event) {
		for(TradeRule rule : this.rules)
		{
			if(rule.isActive())
				rule.afterTrade(event);
		}
	}

	@Nonnull
	@Override
	public List<TradeRule> getRules() { return new ArrayList<>(this.rules); }

	@Override
	public void markTradeRulesDirty() { }

	/**
	 * Only to be used for persistent trader loading
	 */
	public void setRules(List<TradeRule> rules) { this.rules = rules; }

	public abstract TradeComparisonResult compare(TradeData otherTrade);

	public abstract boolean AcceptableDifferences(TradeComparisonResult result);

	public abstract List<Component> GetDifferenceWarnings(TradeComparisonResult differences);

	@OnlyIn(Dist.CLIENT)
	public abstract TradeRenderManager<?> getButtonRenderer();

	/**
	 * Called when an input display is clicked on in display mode.
	 * Runs on the client, but can (and should) be called on the server by running tab.sendInputInteractionMessage for consistent execution
	 *
	 * @param tab The Trade Edit tab that is being used to display this tab.
	 * @param clientHandler The client handler that can be used to send custom client messages to the currently opened tab. Will be null on the server.
	 * @param index The index of the input display that was clicked.
	 * @param button The mouse button that was clicked.
	 * @param heldItem The item being held by the player.
	 */
	public abstract void onInputDisplayInteraction(@Nonnull BasicTradeEditTab tab, @Nullable Consumer<CompoundTag> clientHandler, int index, int button, @Nonnull ItemStack heldItem);

	/**
	 * Called when an output display is clicked on in display mode.
	 * Runs on the client, but can (and should) be called on the server by running tab.sendOutputInteractionMessage for consistent execution
	 *
	 * @param tab The Trade Edit tab that is being used to display this tab.
	 * @param clientHandler The client handler that can be used to send custom client messages to the currently opened tab. Will be null on the server.
	 * @param index The index of the input display that was clicked.
	 * @param button The mouse button that was clicked.
	 * @param heldItem The item being held by the player.
	 */
	public abstract void onOutputDisplayInteraction(@Nonnull BasicTradeEditTab tab, @Nullable Consumer<CompoundTag> clientHandler, int index, int button, @Nonnull ItemStack heldItem);

	/**
	 * Called when the trade is clicked on in display mode, but the mouse wasn't over any of the input or output slots.
	 * Runs on the client, but can (and should) be called on the server by running tab.sendOtherInteractionMessage for consistent code execution.
	 *
	 * @param tab The Trade Edit tab that is being used to display this tab.
	 * @param clientHandler The client handler that can be used to send custom client messages to the currently opened tab. Will be null on the server.
	 * @param mouseX The local X position of the mouse button when it was clicked. [0,tradeButtonWidth)
	 * @param mouseY The local Y position of the mouse button when it was clicked. [0,tradeButtonHeight)
	 * @param button The mouse button that was clicked.
	 * @param heldItem The item currently being held by the player.
	 */
	public abstract void onInteraction(@Nonnull BasicTradeEditTab tab, @Nullable Consumer<CompoundTag> clientHandler, int mouseX, int mouseY, int button, @Nonnull ItemStack heldItem);

	@NotNull
	public final List<Integer> getRelevantInventorySlots(TradeContext context, NonNullList<Slot> slots) {
		List<Integer> results = new ArrayList<>();
		this.collectRelevantInventorySlots(context, slots, results);
		return results;
	}

	protected void collectRelevantInventorySlots(TradeContext context, NonNullList<Slot> slots, List<Integer> results) { }

}