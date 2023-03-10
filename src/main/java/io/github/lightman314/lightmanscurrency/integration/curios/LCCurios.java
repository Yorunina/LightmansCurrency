package io.github.lightman314.lightmanscurrency.integration.curios;

import javax.annotation.Nonnull;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.gamerule.ModGameRules;
import io.github.lightman314.lightmanscurrency.common.items.PortableTerminalItem;
import io.github.lightman314.lightmanscurrency.common.menus.wallet.WalletMenuBase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.GameRules;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.capability.CurioItemCapability;

import java.util.Map;

public class LCCurios {
	
	public static final String WALLET_SLOT = "wallet";

	private static ICuriosItemHandler lazyGetCuriosHelper(LivingEntity entity) {
		LazyOptional<ICuriosItemHandler> optional = CuriosApi.getCuriosHelper().getCuriosHandler(entity);
		return optional.isPresent() ? optional.orElseGet(() -> { throw new RuntimeException("Unexpected error occurred!"); }) : null;
	}


	public static boolean hasWalletSlot(LivingEntity entity) {
		if(entity == null)
			return false;
		try {
			ICuriosItemHandler curiosHelper = lazyGetCuriosHelper(entity);
			if(curiosHelper != null)
			{
				ICurioStacksHandler stacksHandler = curiosHelper.getStacksHandler(WALLET_SLOT).orElse(null);
				return stacksHandler != null && stacksHandler.getSlots() > 0;
			}
		} catch(Throwable t) { LightmansCurrency.LogError("Error checking curios wallet slot validity.", t); }
		return false;
	}
	
	public static ItemStack getCuriosWalletContents(LivingEntity entity) {
		try {
			ICuriosItemHandler curiosHelper = lazyGetCuriosHelper(entity);
			if(curiosHelper != null)
			{
				ICurioStacksHandler stacksHandler = curiosHelper.getStacksHandler(WALLET_SLOT).orElse(null);
				if(stacksHandler != null && stacksHandler.getSlots() > 0)
					return stacksHandler.getStacks().getStackInSlot(0);
			}
		} catch(Throwable t) { LightmansCurrency.LogError("Error getting wallet from curios wallet slot.", t); }
		return ItemStack.EMPTY;
	}
	
	public static void setCuriosWalletContents(LivingEntity entity, ItemStack wallet) {
		try {
			ICuriosItemHandler curiosHelper = lazyGetCuriosHelper(entity);
			if(curiosHelper != null)
			{
				ICurioStacksHandler stacksHandler = curiosHelper.getStacksHandler(WALLET_SLOT).orElse(null);
				if(stacksHandler != null && stacksHandler.getSlots() > 0)
					stacksHandler.getStacks().setStackInSlot(0, wallet);
			}
		} catch(Throwable t) { LightmansCurrency.LogError("Error placing wallet into the curios wallet slot.", t); }
	}
	
	public static boolean getCuriosWalletVisibility(LivingEntity entity) {
		try {
			ICuriosItemHandler curiosHelper = lazyGetCuriosHelper(entity);
			if(curiosHelper != null)
			{
				ICurioStacksHandler stacksHandler = curiosHelper.getStacksHandler(WALLET_SLOT).orElse(null);
				if(stacksHandler != null && stacksHandler.getSlots() > 0)
					return stacksHandler.getRenders().get(0);
			}
		} catch(Throwable t) { LightmansCurrency.LogError("Error getting wallet slot visibility from curios.", t); }
		return false;
	}

	public static boolean hasPortableTerminal(LivingEntity entity) {
		try{
			ICuriosItemHandler curiosHelper = lazyGetCuriosHelper(entity);
			if(curiosHelper != null)
			{
				for(Map.Entry<String,ICurioStacksHandler> entry : curiosHelper.getCurios().entrySet())
				{
					ICurioStacksHandler stacksHandler = entry.getValue();
					if(stacksHandler != null)
					{
						IDynamicStackHandler sh = stacksHandler.getStacks();
						for(int i = 0; i < sh.getSlots(); ++i)
						{
							if(sh.getStackInSlot(i).getItem() instanceof PortableTerminalItem)
								return true;
						}
					}
				}
			}
		} catch(Throwable t) { LightmansCurrency.LogError("Error checking for Portable Terminal from curios.", t); }
		return false;
	}

	public static ICapabilityProvider createWalletProvider(ItemStack ignored)
	{
		return CurioItemCapability.createProvider(new ICurio()
		{
			
			@Nonnull
			@Override
			public SoundInfo getEquipSound(SlotContext context) { return new SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 1f); }
			
			@Override
			public boolean canEquipFromUse(SlotContext context) { return false; }
			
			@Override
			public boolean canSync(String identifier, int index, LivingEntity livingEntity) { return true; }
			
			@Override
			public boolean canEquip(String identifier, LivingEntity livingEntity) { return livingEntity instanceof PlayerEntity; }
			
			@Override
			public boolean canUnequip(String identifier, LivingEntity entity) {
				if(entity instanceof PlayerEntity)
				{
					PlayerEntity player = (PlayerEntity) entity;
					if(player.containerMenu instanceof WalletMenuBase)
					{
						WalletMenuBase menu = (WalletMenuBase)player.containerMenu;
						//Prevent unequipping if the wallet is open in the menu.
						return !menu.isEquippedWallet();
					}
				}
				return true;
			}
			
			@Nonnull
			@Override
			public DropRule getDropRule(LivingEntity livingEntity)
			{
				GameRules.BooleanValue keepWallet = ModGameRules.getCustomValue(livingEntity.level, ModGameRules.KEEP_WALLET);
				if((keepWallet != null && keepWallet.get()))
					return DropRule.ALWAYS_KEEP;
				else
					return DropRule.DEFAULT;
			}
			
		});
	}
	
}
