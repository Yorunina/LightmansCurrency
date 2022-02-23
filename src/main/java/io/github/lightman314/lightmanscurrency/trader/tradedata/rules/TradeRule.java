package io.github.lightman314.lightmanscurrency.trader.tradedata.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TradeRuleScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.TradeCostEvent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class TradeRule {
	
	public static final String DEFAULT_TAG = "TradeRules";
	
	public final ResourceLocation type;
	public final Component getName() { return new TranslatableComponent("traderule." + type.getNamespace() + "." + type.getPath()); }
	
	public void beforeTrade(PreTradeEvent event) {}
	public void tradeCost(TradeCostEvent event) {}
	public void afterTrade(PostTradeEvent event) {}
	
	protected TradeRule(ResourceLocation type)
	{
		this.type = type;
	}
	
	public CompoundTag getNBT()
	{
		CompoundTag compound = new CompoundTag();
		compound.putString("type", this.type.toString());
		return write(compound);
	}
	
	protected abstract CompoundTag write(CompoundTag compound);
	
	public abstract void readNBT(CompoundTag compound);
	
	public abstract CompoundTag savePersistentData();
	public abstract void loadPersistentData(CompoundTag data);
	
	public abstract IconData getButtonIcon();
	
	public static CompoundTag writeRules(CompoundTag compound, List<TradeRule> rules)
	{
		return writeRules(compound, rules, DEFAULT_TAG);
	}
	
	public static CompoundTag writeRules(CompoundTag compound, List<TradeRule> rules, String tag)
	{
		ListTag ruleData = new ListTag();
		for(int i = 0; i < rules.size(); i++)
		{
			CompoundTag thisRuleData = rules.get(i).getNBT();
			if(thisRuleData != null)
				ruleData.add(thisRuleData);
		}
		compound.put(tag, ruleData);
		return compound;
	}
	
	public static boolean writePersistentData(CompoundTag compound, List<TradeRule> rules, String tag) {
		ListTag ruleData = new ListTag();
		for(int i = 0; i < rules.size(); ++i)
		{
			CompoundTag thisRuleData = rules.get(i).savePersistentData();
			if(thisRuleData != null)
			{
				thisRuleData.putString("type", rules.get(i).type.toString());
				ruleData.add(thisRuleData);
			}
		}
		if(ruleData.size() <= 0)
			return false;
		compound.put(tag, ruleData);
		return true;
	}
	
	public static List<TradeRule> readRules(CompoundTag compound)
	{
		return readRules(compound, DEFAULT_TAG);
	}
	
	public static List<TradeRule> readRules(CompoundTag compound, String tag)
	{
		List<TradeRule> rules = new ArrayList<>();
		if(compound.contains(tag, Tag.TAG_LIST))
		{
			ListTag ruleData = compound.getList(tag, Tag.TAG_COMPOUND);
			for(int i = 0; i < ruleData.size(); i++)
			{
				CompoundTag thisRuleData = ruleData.getCompound(i);
				TradeRule thisRule = Deserialize(thisRuleData);
				if(thisRule != null)
					rules.add(thisRule);
			}
		}
		return rules;
	}
	
	public static void readPersistentData(CompoundTag compound, List<TradeRule> tradeRules, String tag)
	{
		if(compound.contains(tag, Tag.TAG_LIST))
		{
			ListTag ruleData = compound.getList(tag, Tag.TAG_COMPOUND);
			for(int i = 0; i < ruleData.size(); ++i)
			{
				CompoundTag thisRuleData = ruleData.getCompound(i);
				boolean query = true;
				for(int r = 0; query && r < tradeRules.size(); ++r)
				{
					if(tradeRules.get(r).type.toString().contentEquals(thisRuleData.getString("type")))
					{
						tradeRules.get(r).loadPersistentData(thisRuleData);
					}
				}
			}
		}
	}
	
	public static List<TradeRule> Parse(JsonArray tradeRuleData)
	{
		Gson g = new Gson();
		List<TradeRule> rules = new ArrayList<>();
		for(int i = 0; i < tradeRuleData.size(); ++i)
		{
			try {
				CompoundTag thisRuleData = TagParser.parseTag(g.toJson(tradeRuleData.get(i)));
				TradeRule thisRule = Deserialize(thisRuleData);
				if(thisRule != null)
					rules.add(thisRule);
			}
			catch(Throwable t) { LightmansCurrency.LogError("Error loading Trade Rule at index " + i + ".", t); }
		}
		return rules;
	}
	
	@OnlyIn(Dist.CLIENT)
	public abstract GUIHandler createHandler(TradeRuleScreen screen, Supplier<TradeRule> rule);
	
	@OnlyIn(Dist.CLIENT)
	public static abstract class GUIHandler
	{
		
		protected final TradeRuleScreen screen;
		private final Supplier<TradeRule> rule;
		protected final TradeRule getRuleRaw() { return rule.get(); }
		
		protected GUIHandler(TradeRuleScreen screen, Supplier<TradeRule> rule)
		{
			this.screen = screen;
			this.rule = rule;
		}
		
		public abstract void initTab();
		
		public abstract void renderTab(PoseStack postStack, int mouseX, int mouseY, float partialTicks);
		
		public abstract void onTabClose();
		
		public void onScreenTick() { }
		
		public <T extends GuiEventListener & Widget & NarratableEntry> T addCustomRenderable(T widget)
		{
			return screen.addCustomRenderable(widget);
		}
		
		public <T extends GuiEventListener & NarratableEntry> T addCustomWidget(T widget)
		{
			return screen.addCustomWidget(widget);
		}
		
		public <T extends GuiEventListener> void removeCustomWidget(T widget)
		{
			screen.removeCustomWidget(widget);
		}
		
	}
	
	
	
	/**
	 * Trade Rule Deserialization
	 */
	static final Map<String,Supplier<TradeRule>> registeredDeserializers = new HashMap<>();
	
	public static void RegisterDeserializer(ResourceLocation type, Supplier<TradeRule> deserializer)
	{
		RegisterDeserializer(type.toString(), deserializer);
	}
	
	public static void RegisterDeserializer(String type, Supplier<TradeRule> deserializer)
	{
		if(registeredDeserializers.containsKey(type))
		{
			LightmansCurrency.LogWarning("A trade rule deserializer of type '" + type + "' has already been registered.");
			return;
		}
		registeredDeserializers.put(type, deserializer);
		LightmansCurrency.LogInfo("Registered trade rule deserializer of type " + type);
	}
	
	public static TradeRule Deserialize(CompoundTag compound)
	{
		String thisType = compound.getString("type");
		AtomicReference<TradeRule> data = new AtomicReference<TradeRule>();
		registeredDeserializers.forEach((type,deserializer) -> {
			if(thisType.equals(type))
			{
				TradeRule rule = deserializer.get();
				rule.readNBT(compound);
				data.set(rule);
			}	
		});
		if(data.get() != null)
			return data.get();
		LightmansCurrency.LogError("Could not find a deserializer of type '" + thisType + "'. Unable to load the Trade Rule.");
		return null;
	}
	
	
}
