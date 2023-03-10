package io.github.lightman314.lightmanscurrency.common.enchantments;

import java.util.List;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.common.capability.IWalletHandler;
import io.github.lightman314.lightmanscurrency.common.capability.WalletCapability;
import io.github.lightman314.lightmanscurrency.common.core.ModEnchantments;
import io.github.lightman314.lightmanscurrency.common.core.ModSounds;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import io.github.lightman314.lightmanscurrency.common.menus.wallet.WalletMenuBase;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class CoinMagnetEnchantment extends WalletEnchantment {

	//Max enchantment level
	public static final int MAX_LEVEL = 3;
	//Max level to calculate range for
	public static final int MAX_CALCULATION_LEVEL = MAX_LEVEL + 2;
	
	public CoinMagnetEnchantment(Rarity rarity, EquipmentSlotType... slots) {
		super(rarity, LCEnchantmentCategories.WALLET_PICKUP_CATEGORY, slots);
	}
	
	public int getMinCost(int level) { return 5 + (level - 1) * 8; }

	public int getMaxCost(int level) { return super.getMinCost(level) + 50; }

	public int getMaxLevel() { return MAX_LEVEL; }
	
	public static void runEntityTick(LivingEntity entity) {
		if(entity.isSpectator())
			return;
		IWalletHandler walletHandler = WalletCapability.lazyGetWalletHandler(entity);
		if(walletHandler != null)
		{
			ItemStack wallet = walletHandler.getWallet();
			//Don't do anything if the stack is not a waller
			//Or if the wallet cannot pick up coins
			if(!WalletItem.isWallet(wallet) || !(wallet.getItem() instanceof WalletItem) || !WalletItem.CanPickup((WalletItem)wallet.getItem()))
				return;
			//Get the level (-1 to properly calculate range)
			int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.COIN_MAGNET.get(), wallet);
			//Don't do anything if the Coin Magnet enchantment is not present.
			if(enchantLevel <= 0)
				return;
			//Calculate the search radius
			float range = getCollectionRange(enchantLevel);
			World level = entity.level;
			AxisAlignedBB searchBox = new AxisAlignedBB(entity.xo - range, entity.yo - range, entity.zo - range, entity.xo + range, entity.yo + range, entity.zo + range);
			boolean updateWallet = false;
			for(Entity e : level.getEntities(entity, searchBox, CoinMagnetEnchantment::coinMagnetEntityFilter))
			{
				ItemEntity ie = (ItemEntity)e;
				ItemStack coinStack = ie.getItem();
				ItemStack leftovers = WalletItem.PickupCoin(wallet, coinStack);
				if(leftovers.getCount() != coinStack.getCount())
				{
					updateWallet = true;
					if(leftovers.isEmpty())
						ie.remove();
					else
						ie.setItem(leftovers);
					level.playSound(null, entity, ModSounds.COINS_CLINKING, SoundCategory.PLAYERS, 0.4f, 1f);
				}
			}
			if(updateWallet)
			{
				walletHandler.setWallet(wallet);
				WalletMenuBase.OnWalletUpdated(entity);
			}
		}
	}

	public static boolean coinMagnetEntityFilter(Entity entity) {
		if(entity instanceof ItemEntity)
		{
			ItemEntity item = (ItemEntity)entity;
			return !item.hasPickUpDelay() && MoneyUtil.isCoin(item.getItem(), false);
		}
		return false;
	}
	
	public static float getCollectionRange(int enchantLevel) {
		enchantLevel -= 1;
		if(enchantLevel < 0)
			return 0f;
		return Config.SERVER.coinMagnetRangeBase.get() + (Config.SERVER.coinMagnetRangeLevel.get() * Math.min(enchantLevel, MAX_CALCULATION_LEVEL - 1));
	}
	
	public static ITextComponent getCollectionRangeDisplay(int enchantLevel) {
		float range = getCollectionRange(enchantLevel);
		String display = range %1f > 0f ? String.valueOf(range) : String.valueOf(Math.round(range));
		return EasyText.literal(display).withStyle(TextFormatting.GREEN);
	}
	
	@Override
	public void addWalletTooltips(List<ITextComponent> tooltips, int enchantLevel, ItemStack wallet) {
		if(wallet.getItem() instanceof WalletItem)
		{
			if(enchantLevel > 0 && WalletItem.CanPickup((WalletItem)wallet.getItem()))
			{
				tooltips.add(EasyText.translatable("tooltip.lightmanscurrency.wallet.pickup.magnet", getCollectionRangeDisplay(enchantLevel)).withStyle(TextFormatting.YELLOW));
			}
		}
	}
	
}
