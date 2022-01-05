package io.github.lightman314.lightmanscurrency.trader.settings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.SettingsLogger;
import io.github.lightman314.lightmanscurrency.client.gui.settings.core.*;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.permissions.PermissionsList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

public class CoreTraderSettings extends Settings{

	public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID,"trader_core");
	
	private static final String UPDATE_CUSTOM_NAME = "customName";
	private static final String UPDATE_ADD_ALLY = "addAlly";
	private static final String UPDATE_REMOVE_ALLY = "removeAlly";
	private static final String UPDATE_ALLY_PERMISSIONS = "allyPermissions";
	private static final String UPDATE_CREATIVE = "creative";
	private static final String UPDATE_OWNERSHIP = "transferOwnership";
	
	public static PermissionsList getAllyDefaultPermissions() { return new PermissionsList(ImmutableMap.of(Permissions.OPEN_STORAGE, 1, Permissions.EDIT_TRADES, 1, Permissions.EDIT_TRADE_RULES, 1, Permissions.EDIT_SETTINGS, 1, Permissions.CHANGE_NAME, 1)); }
	
	//Owner
	PlayerReference owner = null;
	//Ally Permissions
	List<PlayerReference> allies = Lists.newArrayList();
	PermissionsList allyPermissions = getAllyDefaultPermissions();
	//Custom Permissions
	Map<PlayerReference,PermissionsList> customPermissions = Maps.newHashMap();

	//Custom Name
	String customName = "";
	public boolean hasCustomName() { return !this.customName.isEmpty(); }
	public String getCustomName() { return this.customName; }
	public CompoundNBT setCustomName(PlayerEntity requestor, String newName)
	{
		if(!this.hasPermission(requestor, Permissions.CHANGE_NAME))
		{
			PermissionWarning(requestor, "change the traders name", Permissions.CHANGE_NAME);
			return null;
		}
		if(!this.customName.contentEquals(newName))
		{
			String oldName = this.customName;
			this.customName = newName;
			this.logger.LogNameChange(requestor, oldName, this.customName);
			this.markDirty();
			CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_CUSTOM_NAME);
			updateInfo.putString("NewName", this.customName);
			return updateInfo;
		}
		return null;
	}
	
	//Creative Mode
	boolean isCreative = false;
	
	SettingsLogger logger = new SettingsLogger();
	
	public CoreTraderSettings(IMarkDirty marker, BiConsumer<ResourceLocation,CompoundNBT> sendToServer) { super(marker, sendToServer, TYPE); }
	
	public PlayerReference getOwner() { return this.owner; }
	/**
	 * Initialized the traders owner.
	 * Does nothing if the owner is already defined.
	 * Does not flag it as dirty, as it is assumed to be run during a traders initialization.
	 */
	public void initializeOwner(PlayerReference owner)
	{
		if(this.owner == null)
			this.owner = owner;
		else
			LightmansCurrency.LogWarning("Attempted to initialize the owner for a trader that is already owned.");
	}
	public CompoundNBT setOwner(PlayerEntity requestor, String newOwnerName){
		if(!this.hasPermission(requestor, Permissions.TRANSFER_OWNERSHIP))
		{
			PermissionWarning(requestor, "transfer ownership", Permissions.TRANSFER_OWNERSHIP);
			return null;
		}
		else if(this.owner == null)
		{
			this.owner = PlayerReference.of(newOwnerName);
			if(this.owner != null)
			{
				CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_OWNERSHIP);
				updateInfo.putString("newOwner", newOwnerName);
				return updateInfo;
			}
		}
		else
		{
			PlayerReference newOwner = PlayerReference.of(newOwnerName);
			if(newOwner != null && !this.owner.is(newOwner))
			{
				PlayerReference oldOwner = this.owner;
				this.owner = newOwner;
				this.logger.LogOwnerChange(requestor, oldOwner, this.owner);
				this.markDirty();
				
				CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_OWNERSHIP);
				updateInfo.putString("newOwner", newOwnerName);
				return updateInfo;
			}
		}
		return null;
	}
	
	public List<PlayerReference> getAllies() { return this.allies; }
	public CompoundNBT addAlly(PlayerEntity requestor, String newAllyName)
	{
		if(!this.hasPermission(requestor, Permissions.ADD_REMOVE_ALLIES))
		{
			PermissionWarning(requestor, "add an ally", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		if(!PlayerReference.listContains(this.allies, newAllyName))
		{
			PlayerReference newAlly = PlayerReference.of(newAllyName);
			if(newAlly != null)
			{
				this.allies.add(newAlly);
				this.logger.LogAllyChange(requestor, newAlly, true);
				this.markDirty();
			}
			if(newAlly != null || requestor.world.isRemote)
			{
				CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_ADD_ALLY);
				updateInfo.putString("AllyName", newAllyName);
				return updateInfo;
			}
		}
		return null;
	}
	public CompoundNBT removeAlly(PlayerEntity requestor, String removedAllyName)
	{
		if(!this.hasPermission(requestor, Permissions.ADD_REMOVE_ALLIES))
		{
			PermissionWarning(requestor, "remove an ally", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		if(PlayerReference.listContains(this.allies, removedAllyName))
		{
			PlayerReference removedAlly = null;
			for(int i = 0; i < this.allies.size() && removedAlly == null; ++i)
			{
				if(this.allies.get(i).is(removedAllyName))
				{
					removedAlly = this.allies.get(i);
					this.allies.remove(i);
				}
			}
			if(removedAlly == null)
				return null;
			this.logger.LogAllyChange(requestor, removedAlly, false);
			this.markDirty();
			CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_REMOVE_ALLY);
			updateInfo.putString("AllyName", removedAllyName);
			return updateInfo;
		}
		return null;
	}
	public CompoundNBT setAllyPermissionLevel(PlayerEntity requestor, String permission, int level)
	{
		if(!this.hasPermission(requestor, Permissions.EDIT_PERMISSIONS))
		{
			PermissionWarning(requestor, "edit ally permissions", Permissions.EDIT_PERMISSIONS);
			return null;
		}
		if(this.allyPermissions.getLevel(permission) != level)
		{
			this.allyPermissions.setLevel(permission, level);
			this.markDirty();
		}
		CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_ALLY_PERMISSIONS);
		updateInfo.putString("Permission", permission);
		updateInfo.putInt("Level", level);
		return updateInfo;
	}
	
	/**
	 * Updates the last known name of all matching player references in the owner/ally/custom permissions list.
	 * @param player
	 */
	public void updateNames(PlayerEntity player)
	{
		if(this.owner != null)
			this.owner.tryUpdateName(player);
		for(PlayerReference ally : this.allies)
			ally.tryUpdateName(player);
		this.customPermissions.forEach((p,permission) ->{
			p.tryUpdateName(player);
		});
	}
	
	public PermissionsList addCustomPermissionEntry(PlayerEntity requestor, PlayerReference player)
	{
		if(!this.hasPermission(requestor, Permissions.EDIT_PERMISSIONS))
		{
			PermissionWarning(requestor, "add custom permission", Permissions.EDIT_PERMISSIONS);
			return null;
		}
		if(this.customPermissions.containsKey(player))
			return this.customPermissions.get(player);
		return this.customPermissions.put(player, new PermissionsList());
	}
	
	public void removeCustomPermissionEntry(PlayerReference player)
	{
		this.customPermissions.remove(player);
	}
	
	public boolean hasPermission(PlayerEntity player, String permission)
	{
		if(player == null)
			return true;
		return getPermissionLevel(player, permission) > 0;
	}
	
	public int getPermissionLevel(PlayerEntity player, String permission)
	{
		if(player == null || (this.owner != null && this.owner.is(player)) || TradingOffice.isAdminPlayer(player))
			return Integer.MAX_VALUE;
		AtomicInteger level = new AtomicInteger(0);
		if(PlayerReference.listContains(this.allies, player.getUniqueID()))
			level.set(this.allyPermissions.getLevel(permission));
		this.customPermissions.forEach((playerRef,permissions) ->{
			if(playerRef.is(player))
			{
				int l = permissions.getLevel(permission);
				if(l > level.get())
					level.set(l);
			}
		});
		return level.get();
	}
	
	public boolean isCreative() { return this.isCreative; }
	public CompoundNBT toggleCreative(PlayerEntity requestor)
	{
		if(!TradingOffice.isAdminPlayer(requestor))
		{
			PermissionWarning(requestor, "toggle creative mode", Permissions.ADMIN_MODE);
			return null;
		}
		this.isCreative = !this.isCreative;
		this.logger.LogCreativeToggle(requestor, this.isCreative);
		this.markDirty();
		CompoundNBT updateInfo = this.initUpdateInfo(UPDATE_CREATIVE);
		updateInfo.putBoolean("isCreative", this.isCreative);
		return updateInfo;
	}
	
	public SettingsLogger getLogger() { return this.logger; }
	
	@Override
	public void changeSetting(PlayerEntity requestor, CompoundNBT updateInfo) {
		if(this.isUpdateType(updateInfo, UPDATE_CUSTOM_NAME))
		{
			String newName = updateInfo.getString("NewName");
			CompoundNBT result = this.setCustomName(requestor, newName);
			if(result != null)
				this.markDirty();
			LightmansCurrency.LogInfo("Custom Name changed to '" + newName + "' on the server.");
		}
		else if(this.isUpdateType(updateInfo, UPDATE_ADD_ALLY))
		{
			String allyName = updateInfo.getString("AllyName");
			CompoundNBT result = this.addAlly(requestor, allyName);
			if(result != null)
				this.markDirty();
			LightmansCurrency.LogInfo("Attempted to add '" + allyName + "' as an ally.");
		}
		else if(this.isUpdateType(updateInfo, UPDATE_REMOVE_ALLY))
		{
			String allyName = updateInfo.getString("AllyName");
			CompoundNBT result = this.removeAlly(requestor, allyName);
			if(result != null)
				this.markDirty();
			LightmansCurrency.LogInfo("Attempted to remove '" + allyName + "' from the ally list.");
		}
		else if(this.isUpdateType(updateInfo, UPDATE_CREATIVE))
		{
			boolean nowCreative = updateInfo.getBoolean("isCreative");
			if(nowCreative != this.isCreative)
			{
				CompoundNBT result = this.toggleCreative(requestor);
				if(result != null)
					this.markDirty();
			}
		}
	}
	
	public CompoundNBT save(CompoundNBT compound)
	{
		
		this.saveOwner(compound);
		this.saveAllyPermissions(compound);
		this.saveAllyList(compound);
		this.saveCustomPermissions(compound);
		this.saveCustomName(compound);
		this.saveCreative(compound);
		this.saveLogger(compound);
		
		return compound;
	}
	
	public CompoundNBT saveOwner(CompoundNBT compound)
	{
		if(this.owner != null)
			compound.put("Owner", this.owner.save());
		return compound;
	}
	
	public CompoundNBT saveAllyPermissions(CompoundNBT compound)
	{
		this.allyPermissions.save(compound, "AllyPermissions");
		return compound;
	}
	
	public CompoundNBT saveAllyList(CompoundNBT compound)
	{
		PlayerReference.saveList(compound, this.allies, "Allies");
		return compound;
	}
	
	public CompoundNBT saveCustomPermissions(CompoundNBT compound)
	{
		//Custom Permissions
		ListNBT customPermissionsList = new ListNBT();
		this.customPermissions.forEach((player,permissions)->{
			CompoundNBT thisCompound = new CompoundNBT();
			thisCompound.put("Player", player.save());
			permissions.save(compound, "Permissions");
			customPermissionsList.add(thisCompound);
		});
		compound.put("CustomPermissions", customPermissionsList);
		return compound;
	}
	
	public CompoundNBT saveCustomName(CompoundNBT compound)
	{
		compound.putString("CustomName", this.customName);
		return compound;
	}
	
	public CompoundNBT saveCreative(CompoundNBT compound)
	{
		compound.putBoolean("Creative", this.isCreative);
		return compound;
	}
	
	public CompoundNBT saveLogger(CompoundNBT compound)
	{
		this.logger.write(compound);
		return compound;
	}
	
	public void loadFromOldUniversalData(CompoundNBT compound)
	{
		LightmansCurrency.LogInfo("Loading Core Trader Settings from old UniversalData compound.");
		//Owner
		UUID ownerID = null;
		String ownerName = "";
		if(compound.contains("OwnerID"))
			ownerID = compound.getUniqueId("OwnerID");
		if(compound.contains("OwnerName", Constants.NBT.TAG_STRING))
			ownerName = compound.getString("OwnerName");
		if(ownerID != null)
			this.owner = PlayerReference.of(ownerID, ownerName);
		
		//Creative
		if(compound.contains("Creative"))
			this.isCreative = compound.getBoolean("Creative");
		
		//Read allies
		if(compound.contains("Allies",Constants.NBT.TAG_LIST))
		{
			this.allies.clear();
			ListNBT allyList = compound.getList("Allies", Constants.NBT.TAG_COMPOUND);
			for(int i = 0; i < allyList.size(); i++)
			{
				CompoundNBT thisAlly = allyList.getCompound(i);
				if(thisAlly.contains("name", Constants.NBT.TAG_STRING))
				{
					this.addAlly(null, thisAlly.getString("name"));
				}
			}
		}
		
		//TraderName
		if(compound.contains("TraderName", Constants.NBT.TAG_STRING))
			this.customName = compound.getString("TraderName");
	}
	
	public void loadFromOldTraderData(CompoundNBT compound)
	{
		LightmansCurrency.LogInfo("Loading Core Trader Settings from old TileEntity compound.");
		//Owner
		UUID ownerID = null;
		String ownerName = "";
		if(compound.contains("OwnerID"))
			ownerID = compound.getUniqueId("OwnerID");
		if(compound.contains("OwnerName", Constants.NBT.TAG_STRING))
			ownerName = compound.getString("OwnerName");
		if(ownerID != null)
			this.owner = PlayerReference.of(ownerID, ownerName);
		
		//Creative
		if(compound.contains("Creative"))
			this.isCreative = compound.getBoolean("Creative");
		
		//Custom Name
		if(compound.contains("CustomName", Constants.NBT.TAG_STRING))
			this.customName = compound.getString("CustomName");
		
		//Read Allies
		if(compound.contains("Allies",Constants.NBT.TAG_LIST))
		{
			this.allies.clear();
			ListNBT allyList = compound.getList("Allies", Constants.NBT.TAG_COMPOUND);
			for(int i = 0; i < allyList.size(); i++)
			{
				CompoundNBT thisAlly = allyList.getCompound(i);
				if(thisAlly.contains("name", Constants.NBT.TAG_STRING))
				{
					this.addAlly(null, thisAlly.getString("name"));
				}
			}
		}
		
	}
	
	public void load(CompoundNBT compound)
	{
		//Owner
		if(compound.contains("Owner", Constants.NBT.TAG_COMPOUND))
			this.owner = PlayerReference.load(compound.getCompound("Owner"));
		//Ally Permissions
		if(compound.contains("AllyPermissions", Constants.NBT.TAG_COMPOUND))
			this.allyPermissions = PermissionsList.load(compound, "AllyPermissions");
		if(compound.contains("Allies", Constants.NBT.TAG_LIST))
			this.allies = PlayerReference.loadList(compound, "Allies");
		//Custom Permissions
		if(compound.contains("CustomPermissions", Constants.NBT.TAG_LIST))
		{
			this.customPermissions.clear();
			ListNBT customPermissionsList = compound.getList("CustomPermissions", Constants.NBT.TAG_COMPOUND);
			for(int i = 0; i < customPermissionsList.size(); ++i)
			{
				CompoundNBT thisCompound = customPermissionsList.getCompound(i);
				PlayerReference player = PlayerReference.load(thisCompound.getCompound("Player"));
				PermissionsList permissions = PermissionsList.load(thisCompound, "Permissions");
				this.customPermissions.put(player, permissions);
			}
		}
		
		//Custom Name
		if(compound.contains("CustomName", Constants.NBT.TAG_STRING))
			this.customName = compound.getString("CustomName");
			
		//Creative
		if(compound.contains("Creative"))
			this.isCreative = compound.getBoolean("Creative");
		
		//Logger
		this.logger.read(compound);
		
	}
	
	@Override
	protected void initSettingsTabs() {
		//Main Tab Contains 
		this.addTab(MainTab.INSTANCE).addBackEndTab(LoggerTab.INSTANCE).addTab(AllyTab.INSTANCE);
	}
	
	
}
