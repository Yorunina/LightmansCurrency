package io.github.lightman314.lightmanscurrency.network;

import io.github.lightman314.lightmanscurrency.network.message.*;
import io.github.lightman314.lightmanscurrency.network.message.armor_display.*;
import io.github.lightman314.lightmanscurrency.network.message.auction.*;
import io.github.lightman314.lightmanscurrency.network.message.bank.*;
import io.github.lightman314.lightmanscurrency.network.message.coinmint.*;
import io.github.lightman314.lightmanscurrency.network.message.command.*;
import io.github.lightman314.lightmanscurrency.network.message.data.*;
import io.github.lightman314.lightmanscurrency.network.message.emergencyejection.*;
import io.github.lightman314.lightmanscurrency.network.message.enchantments.*;
import io.github.lightman314.lightmanscurrency.network.message.interfacebe.*;
import io.github.lightman314.lightmanscurrency.network.message.notifications.*;
import io.github.lightman314.lightmanscurrency.network.message.persistentdata.*;
import io.github.lightman314.lightmanscurrency.network.message.playertrading.*;
import io.github.lightman314.lightmanscurrency.network.message.teams.*;
import io.github.lightman314.lightmanscurrency.network.message.trader.*;
import io.github.lightman314.lightmanscurrency.network.message.wallet.*;
import io.github.lightman314.lightmanscurrency.network.message.walletslot.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.atm.ATMData;
import io.github.lightman314.lightmanscurrency.common.money.MoneyData;
import io.github.lightman314.lightmanscurrency.network.message.ticket_machine.*;
import io.github.lightman314.lightmanscurrency.network.message.time.MessageSyncClientTime;

public class LightmansCurrencyPacketHandler {
	
	public static final String PROTOCOL_VERSION = "1";
	
	public static SimpleChannel instance;
	private static int nextId = 0;
	
	public static void init()
	{
		
		instance = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(LightmansCurrency.MODID,"network"))
				.networkProtocolVersion(() -> PROTOCOL_VERSION)
				.clientAcceptedVersions(PROTOCOL_VERSION::equals)
				.serverAcceptedVersions(PROTOCOL_VERSION::equals)
				.simpleChannel();
		
		//ATM & Bank
		register(MessageATMConversion.class, MessageATMConversion::encode, MessageATMConversion::decode, MessageATMConversion::handle);
		register(MessageSelectBankAccount.class, MessageSelectBankAccount::encode, MessageSelectBankAccount::decode, MessageSelectBankAccount::handle);
		register(MessageBankInteraction.class, MessageBankInteraction::encode, MessageBankInteraction::decode, MessageBankInteraction::handle);
		register(MessageInitializeClientBank.class, MessageInitializeClientBank::encode, MessageInitializeClientBank::decode, MessageInitializeClientBank::handle);
		register(MessageUpdateClientBank.class, MessageUpdateClientBank::encode, MessageUpdateClientBank::decode, MessageUpdateClientBank::handle);
		register(MessageBankTransferTeam.class, MessageBankTransferTeam::encode, MessageBankTransferTeam::decode, MessageBankTransferTeam::handle);
		register(MessageBankTransferPlayer.class, MessageBankTransferPlayer::encode, MessageBankTransferPlayer::decode, MessageBankTransferPlayer::handle);
		register(MessageBankTransferResponse.class, MessageBankTransferResponse::encode, MessageBankTransferResponse::decode, MessageBankTransferResponse::handle);
		register(MessageATMSetPlayerAccount.class, MessageATMSetPlayerAccount::encode, MessageATMSetPlayerAccount::decode, MessageATMSetPlayerAccount::handle);
		register(MessageATMPlayerAccountResponse.class, MessageATMPlayerAccountResponse::encode, MessageATMPlayerAccountResponse::decode, MessageATMPlayerAccountResponse::handle);
		register(MessageSetBankNotificationLevel.class, MessageSetBankNotificationLevel::encode, MessageSetBankNotificationLevel::decode, MessageSetBankNotificationLevel::handle);
		register(SPacketSyncSelectedBankAccount.class,SPacketSyncSelectedBankAccount::encode, SPacketSyncSelectedBankAccount::decode, SPacketSyncSelectedBankAccount::handle);
		
		//Coinmint
		register(MessageMintCoin.class, MessageMintCoin::encode, MessageMintCoin::decode, MessageMintCoin::handle);
		
		//Trader
		register(MessageExecuteTrade.class, MessageExecuteTrade::encode, MessageExecuteTrade::decode, MessageExecuteTrade::handle);
		register(MessageCollectCoins.class, MessageCollectCoins::encode, MessageCollectCoins::decode, MessageCollectCoins::handle);
		register(MessageStoreCoins.class, MessageStoreCoins::encode, MessageStoreCoins::decode, MessageStoreCoins::handle);
		register(MessageOpenStorage.class, MessageOpenStorage::encode, MessageOpenStorage::decode, MessageOpenStorage::handle);
		register(MessageOpenTrades.class, MessageOpenTrades::encode, MessageOpenTrades::decode, MessageOpenTrades::handle);
		register(MessageSyncUsers.class, MessageSyncUsers::encode, MessageSyncUsers::decode, MessageSyncUsers::handle);
		register(MessageAddOrRemoveTrade.class, MessageAddOrRemoveTrade::encode, MessageAddOrRemoveTrade::decode, MessageAddOrRemoveTrade::handle);
		register(MessageTraderMessage.class, MessageTraderMessage::encode, MessageTraderMessage::decode, MessageTraderMessage::handle);
		register(MessageStorageInteraction.class, MessageStorageInteraction::encode, MessageStorageInteraction::decode, MessageStorageInteraction::handle);
		register(MessageStorageInteractionC.class, MessageStorageInteractionC::encode, MessageStorageInteractionC::decode, MessageStorageInteractionC::handle);
		
		//Armor Display Trader
		register(MessageRequestArmorStandID.class, MessageRequestArmorStandID::encode, MessageRequestArmorStandID::decode, MessageRequestArmorStandID::handle);
		register(MessageSendArmorStandID.class, MessageSendArmorStandID::encode, MessageSendArmorStandID::decode, MessageSendArmorStandID::handle);
		
		//Wallet
		register(MessagePlayPickupSound.class, MessagePlayPickupSound::encode, MessagePlayPickupSound::decode, MessagePlayPickupSound::handle);
		register(MessageWalletConvertCoins.class, MessageWalletConvertCoins::encode, MessageWalletConvertCoins::decode, MessageWalletConvertCoins::handle);
		register(MessageWalletToggleAutoConvert.class, MessageWalletToggleAutoConvert::encode, MessageWalletToggleAutoConvert::decode, MessageWalletToggleAutoConvert::handle);
		register(MessageOpenWallet.class, MessageOpenWallet::encode, MessageOpenWallet::decode, MessageOpenWallet::handle);
		register(MessageOpenWalletBank.class, MessageOpenWalletBank::encode, MessageOpenWalletBank::decode, MessageOpenWalletBank::handle);
		register(MessageWalletQuickCollect.class, LazyEncoders::emptyEncode, LazyEncoders.emptyDecode(MessageWalletQuickCollect::new), MessageWalletQuickCollect::handle);
		
		//Wallet Inventory Slot
		register(SPacketSyncWallet.class, SPacketSyncWallet::encode, SPacketSyncWallet::decode, SPacketSyncWallet::handle);
		register(CPacketSetVisible.class, CPacketSetVisible::encode, CPacketSetVisible::decode, CPacketSetVisible::handle);
		register(CPacketWalletInteraction.class, CPacketWalletInteraction::encode, CPacketWalletInteraction::decode, CPacketWalletInteraction::handle);
		
		//Ticket Machine
		register(MessageCraftTicket.class, MessageCraftTicket::encode, MessageCraftTicket::decode, MessageCraftTicket::handle);
		
		//Universal Traders
		register(MessageClearClientTraders.class, LazyEncoders::emptyEncode, LazyEncoders.emptyDecode(MessageClearClientTraders::new), MessageClearClientTraders::handle);
		register(MessageUpdateClientTrader.class, MessageUpdateClientTrader::encode, MessageUpdateClientTrader::decode, MessageUpdateClientTrader::handle);
		register(MessageRemoveClientTrader.class, MessageRemoveClientTrader::encode, MessageRemoveClientTrader::decode, MessageRemoveClientTrader::handle);
		
		//Auction House
		register(MessageStartBid.class, MessageStartBid::encode, MessageStartBid::decode, MessageStartBid::handle);
		register(MessageSubmitBid.class, MessageSubmitBid::encode, MessageSubmitBid::decode, MessageSubmitBid::handle);
		register(SMessageSyncAuctionStandDisplay.class, SMessageSyncAuctionStandDisplay::encode, SMessageSyncAuctionStandDisplay::decode, SMessageSyncAuctionStandDisplay::handle);
		
		//Trader Interfaces
		register(MessageHandlerMessage.class, MessageHandlerMessage::encode, MessageHandlerMessage::decode, MessageHandlerMessage::handle);
		register(MessageInterfaceInteraction.class, MessageInterfaceInteraction::encode, MessageInterfaceInteraction::decode, MessageInterfaceInteraction::handle);
		
		//Teams
		register(MessageInitializeClientTeams.class, MessageInitializeClientTeams::encode, MessageInitializeClientTeams::decode, MessageInitializeClientTeams::handle);
		register(MessageRemoveClientTeam.class, MessageRemoveClientTeam::encode, MessageRemoveClientTeam::decode, MessageRemoveClientTeam::handle);
		register(MessageUpdateClientTeam.class, MessageUpdateClientTeam::encode, MessageUpdateClientTeam::decode, MessageUpdateClientTeam::handle);
		register(MessageEditTeam.class, MessageEditTeam::encode, MessageEditTeam::decode, MessageEditTeam::handle);
		register(MessageRenameTeam.class, MessageRenameTeam::encode, MessageRenameTeam::decode, MessageRenameTeam::handle);
		register(MessageDisbandTeam.class, MessageDisbandTeam::encode, MessageDisbandTeam::decode, MessageDisbandTeam::handle);
		register(MessageOpenTeamManager.class, LazyEncoders::emptyEncode, LazyEncoders.emptyDecode(MessageOpenTeamManager::new), MessageOpenTeamManager::handle);
		register(MessageCreateTeam.class, MessageCreateTeam::encode, MessageCreateTeam::decode, MessageCreateTeam::handle);
		register(MessageCreateTeamResponse.class, MessageCreateTeamResponse::encode, MessageCreateTeamResponse::decode, MessageCreateTeamResponse::handle);
		register(MessageCreateTeamBankAccount.class, MessageCreateTeamBankAccount::encode, MessageCreateTeamBankAccount::decode, MessageCreateTeamBankAccount::handle);
		register(MessageSetTeamBankLimit.class, MessageSetTeamBankLimit::encode, MessageSetTeamBankLimit::decode, MessageSetTeamBankLimit::handle);
		
		//Notifications
		register(MessageUpdateClientNotifications.class, MessageUpdateClientNotifications::encode, MessageUpdateClientNotifications::decode, MessageUpdateClientNotifications::handle);
		register(MessageFlagNotificationsSeen.class, MessageFlagNotificationsSeen::encode, MessageFlagNotificationsSeen::decode, MessageFlagNotificationsSeen::handle);
		register(MessageClientNotification.class, MessageClientNotification::encode, MessageClientNotification::decode, MessageClientNotification::handle);
		
		//Core
		register(MessageRequestNBT.class, MessageRequestNBT::encode, MessageRequestNBT::decode, MessageRequestNBT::handle);
		register(MessageSyncClientTime.class, MessageSyncClientTime::encode, MessageSyncClientTime::decode, MessageSyncClientTime::handle);
		
		//Command/Admin
		register(MessageSyncAdminList.class, MessageSyncAdminList::encode, MessageSyncAdminList::decode, MessageSyncAdminList::handle);
		register(MessageDebugTrader.class, MessageDebugTrader::encode, MessageDebugTrader::decode, MessageDebugTrader::handle);
		
		//Money Data
		register(MoneyData.class, MoneyData::encode, MoneyData::decode, MoneyData::handle);
		
		//ATM Data
		register(ATMData.class, ATMData::encode, ATMData::decode, ATMData::handle);
		
		//Enchantments
		register(SPacketMoneyMendingClink.class, LazyEncoders::emptyEncode, LazyEncoders.emptyDecode(SPacketMoneyMendingClink::new), SPacketMoneyMendingClink::handle);
		
		//Persistent Data
		register(MessageAddPersistentTrader.class, MessageAddPersistentTrader::encode, MessageAddPersistentTrader::decode, MessageAddPersistentTrader::handle);
		register(MessageAddPersistentAuction.class, MessageAddPersistentAuction::encode, MessageAddPersistentAuction::decode, MessageAddPersistentAuction::handle);
		
		//Ejection data
		register(SPacketSyncEjectionData.class, SPacketSyncEjectionData::encode, SPacketSyncEjectionData::decode, SPacketSyncEjectionData::handle);
		register(CPacketChangeSelectedData.class, CPacketChangeSelectedData::encode, CPacketChangeSelectedData::decode, CPacketChangeSelectedData::handle);
		register(SPacketChangeSelectedData.class, SPacketChangeSelectedData::encode, SPacketChangeSelectedData::decode, SPacketChangeSelectedData::handle);
		register(CPacketOpenTraderRecovery.class, LazyEncoders::emptyEncode, LazyEncoders.emptyDecode(CPacketOpenTraderRecovery::new), CPacketOpenTraderRecovery::handle);

		//Player Trading
		register(SMessageUpdatePlayerTrade.class, SMessageUpdatePlayerTrade::encode, SMessageUpdatePlayerTrade::decode, SMessageUpdatePlayerTrade::handle);
		register(CMessagePlayerTradeInteraction.class, CMessagePlayerTradeInteraction::encode, CMessagePlayerTradeInteraction::decode, CMessagePlayerTradeInteraction::handle);

	}
	
	private static <T> void register(Class<T> clazz, BiConsumer<T, PacketBuffer> encoder, Function<PacketBuffer,T> decoder, BiConsumer<T,Supplier<Context>> handler)
	{
		instance.registerMessage(nextId++, clazz, encoder, decoder, handler);
	}
	
	public static PacketTarget getTarget(PlayerEntity player)
	{
		if(player instanceof ServerPlayerEntity)
			return getTarget((ServerPlayerEntity) player);
		return null;
	}
	
	public static PacketTarget getTarget(ServerPlayerEntity player)
	{
		return PacketDistributor.PLAYER.with(() -> player);
	}
	
	private static class LazyEncoders
	{

		public static <T> void emptyEncode(T ignored1, PacketBuffer ignored2) {}
		public static <T> Function<PacketBuffer,T> emptyDecode(NonNullSupplier<T> get) { return (buffer) -> get.get(); }

	}
	
}