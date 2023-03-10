package io.github.lightman314.lightmanscurrency.common.entity.merchant.villager.listings;

import com.google.gson.JsonObject;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.entity.merchant.villager.ItemListingSerializer;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class EnchantedItemForCoinsTrade implements VillagerTrades.ITrade
{

    public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID, "enchanted_item_for_coins");
    public static final Serializer SERIALIZER = new Serializer();

    protected final Item baseCoin;
    protected final int baseCoinCount;
    protected final Item sellItem;
    protected final int maxTrades;
    protected final int xp;
    protected final float priceMult;
    protected final double basePriceModifier;

    public EnchantedItemForCoinsTrade(IItemProvider baseCoin, int baseCoinCount, IItemProvider sellItem, int maxUses, int xpValue, float priceMultiplier, double basePriceModifier)
    {
        this.baseCoin = baseCoin.asItem();
        this.baseCoinCount = baseCoinCount;
        this.basePriceModifier = basePriceModifier;
        this.sellItem = sellItem.asItem();
        this.maxTrades = maxUses;
        this.xp = xpValue;
        this.priceMult = priceMultiplier;

    }

    @Override
    public MerchantOffer getOffer(@Nonnull Entity trader, Random rand) {
        int i = 5 + rand.nextInt(15);
        ItemStack itemstack = EnchantmentHelper.enchantItem(rand, new ItemStack(sellItem), i, false);

        long coinValue = MoneyUtil.getValue(this.baseCoin);
        long baseValue = coinValue * this.baseCoinCount;
        long priceValue = baseValue + (long)(coinValue * i * this.basePriceModifier);

        ItemStack price1 = ItemStack.EMPTY, price2 = ItemStack.EMPTY;
        List<ItemStack> priceStacks = MoneyUtil.getCoinsOfValue(priceValue);
        if(priceStacks.size() > 0)
            price1 = priceStacks.get(0);
        if(priceStacks.size() > 1)
            price2 = priceStacks.get(1);

        LightmansCurrency.LogInfo("EnchantedItemForCoinsTrade.getOffer() -> \n" +
                "i=" + i +
                "\ncoinValue=" + coinValue +
                "\nbaseValue=" + baseValue +
                "\npriceValue=" + priceValue +
                "\nprice1=" + price1.getCount() + "x" + ForgeRegistries.ITEMS.getKey(price1.getItem()) +
                "\nprice2=" + price2.getCount() + "x" + ForgeRegistries.ITEMS.getKey(price2.getItem())
        );

        return new MerchantOffer(price1, price2, itemstack, this.maxTrades, this.xp, this.priceMult);
    }

    private static class Serializer implements ItemListingSerializer.IItemListingSerializer, ItemListingSerializer.IItemListingDeserializer {
        @Override
        public ResourceLocation getType() { return TYPE; }
        @Override
        public JsonObject serializeInternal(JsonObject json, VillagerTrades.ITrade trade) {
            if(trade instanceof EnchantedItemForCoinsTrade)
            {
                EnchantedItemForCoinsTrade t = (EnchantedItemForCoinsTrade)trade;
                json.addProperty("Coin", ForgeRegistries.ITEMS.getKey(t.baseCoin).toString());
                json.addProperty("BaseCoinCount", t.baseCoinCount);
                json.addProperty("EnchantmentValueModifier", t.basePriceModifier);
                json.addProperty("Sell", ForgeRegistries.ITEMS.getKey(t.sellItem).toString());
                json.addProperty("MaxTrades", t.maxTrades);
                json.addProperty("XP", t.xp);
                json.addProperty("PriceMult", t.priceMult);
                return json;
            }
            return null;
        }

        @Override
        public VillagerTrades.ITrade deserialize(JsonObject json) throws Exception {
            Item coin = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get("Coin").getAsString()));
            int baseCoinCount = json.get("BaseCoinCount").getAsInt();
            double basePriceModifier = json.get("EnchantmentValueModifier").getAsDouble();
            Item sellItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get("Sell").getAsString()));
            int maxTrades = json.get("MaxTrades").getAsInt();
            int xp = json.get("XP").getAsInt();
            float priceMult = json.get("PriceMult").getAsFloat();
            return new EnchantedItemForCoinsTrade(coin, baseCoinCount, sellItem, maxTrades, xp, priceMult, basePriceModifier);
        }
    }

}