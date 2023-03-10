package io.github.lightman314.lightmanscurrency.common.emergency_ejection;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.common.commands.CommandLCAdmin;
import io.github.lightman314.lightmanscurrency.common.data_updating.DataConverter;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.ownership.OwnerData;
import io.github.lightman314.lightmanscurrency.common.player.PlayerReference;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.teams.TeamSaveData;
import io.github.lightman314.lightmanscurrency.common.util.IClientTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;

public class EjectionData implements IInventory, IClientTracker {

	private final OwnerData owner = new OwnerData(this, o -> {});
	IFormattableTextComponent traderName = EasyText.empty();
	public IFormattableTextComponent getTraderName() { return this.traderName; }
	List<ItemStack> items = new ArrayList<>();
	
	private boolean isClient = false;
	public void flagAsClient() { this.isClient = true; }
	public boolean isClient() { return this.isClient; }
	
	private EjectionData() {}
	
	private EjectionData(OwnerData owner, IFormattableTextComponent traderName, List<ItemStack> items) {
		this.owner.copyFrom(owner);
		this.traderName = traderName;
		this.items = items;
	}
	
	public boolean canAccess(PlayerEntity player) {
		if(CommandLCAdmin.isAdminPlayer(player))
			return true;
		if(this.owner == null)
			return false;
		return this.owner.isMember(player);
	}
	
	public CompoundNBT save() {

		CompoundNBT compound = new CompoundNBT();
		
		compound.put("Owner", this.owner.save());
		
		compound.putString("Name", ITextComponent.Serializer.toJson(this.traderName));
		
		ListNBT itemList = new ListNBT();
		for (ItemStack item : this.items) {
			itemList.add(item.save(new CompoundNBT()));
		}
		compound.put("Items", itemList);
		
		return compound;
	}
	
	public void load(CompoundNBT compound) {
		
		//Load old owner data
		if(compound.contains("PlayerOwned"))
		{
			if(compound.getBoolean("PlayerOwned"))
				this.owner.SetOwner(PlayerReference.of(compound.getUUID("Owner"), "UNKNOWN"));
			else
			{
				Team team = TeamSaveData.GetTeam(this.isClient, DataConverter.getNewTeamID(compound.getUUID("Owner")));
				if(team != null)
					this.owner.SetOwner(team);
			}
		}
		else if(compound.contains("Owner"))
			this.owner.load(compound.getCompound("Owner"));
		if(compound.contains("Name"))
			this.traderName = ITextComponent.Serializer.fromJson(compound.getString("Name"));
		if(compound.contains("Items"))
		{
			ListNBT itemList = compound.getList("Items", Constants.NBT.TAG_COMPOUND);
			this.items = new ArrayList<>();
			for(int i = 0; i < itemList.size(); ++i)
			{
				this.items.add(ItemStack.of(itemList.getCompound(i)));
			}
		}
		
	}
	
	public static EjectionData create(World level, BlockPos pos, BlockState state, IDumpable trader) {
		return create(level, pos, state, trader, true);
	}
	
	public static EjectionData create(World level, BlockPos pos, BlockState state, IDumpable trader, boolean dropBlock) {
		
		OwnerData owner = trader.getOwner();
		
		IFormattableTextComponent traderName = trader.getName();
		
		List<ItemStack> items = trader.getContents(level, pos, state, dropBlock);
		
		return new EjectionData(owner, traderName, items);
		
	}
	
	public static EjectionData loadData(CompoundNBT compound) {
		EjectionData data = new EjectionData();
		data.load(compound);
		return data;
	}

	@Override
	public void clearContent() { this.items.clear(); }

	@Override
	public int getContainerSize() { return this.items.size(); }

	@Override
	public boolean isEmpty() {
		for(ItemStack stack : this.items)
		{
			if(!stack.isEmpty())
				return false;
		}
		return true;
	}

	@Nonnull
	@Override
	public ItemStack getItem(int slot) {
		if(slot >= this.items.size() || slot < 0)
			return ItemStack.EMPTY;
		return this.items.get(slot);
	}

	@Nonnull
	@Override
	public ItemStack removeItem(int slot, int count) {
		if(slot >= this.items.size() || slot < 0)
			return ItemStack.EMPTY;
		return this.items.get(slot).split(count);
	}

	@Nonnull
	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if(slot >= this.items.size() || slot < 0)
			return ItemStack.EMPTY;
		ItemStack stack = this.items.get(slot);
		this.items.set(slot, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public void setItem(int slot, @Nonnull ItemStack item) {
		if(slot >= this.items.size() || slot < 0)
			return;
		this.items.set(slot, item);
	}
	
	private void clearEmptySlots() { this.items.removeIf(ItemStack::isEmpty); }

	@Override
	public void setChanged() {
		if(this.isClient)
			return;
		this.clearEmptySlots();
		if(this.isEmpty())
			EjectionSaveData.RemoveEjectionData(this);
		else
			EjectionSaveData.MarkEjectionDataDirty();
	}

	@Override
	public boolean stillValid(@Nonnull PlayerEntity player) {
		return this.canAccess(player);
	}
	
}