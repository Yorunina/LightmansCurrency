package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderinterface;

import java.util.List;

import com.google.common.collect.Lists;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderInterfaceScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TeamSelectWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.IconButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.client.util.TextRenderUtil;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.teams.TeamSaveData;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.TradeData;
import io.github.lightman314.lightmanscurrency.common.menus.traderinterface.TraderInterfaceClientTab;
import io.github.lightman314.lightmanscurrency.common.menus.traderinterface.base.OwnershipTab;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class OwnershipClientTab extends TraderInterfaceClientTab<OwnershipTab>{

	public OwnershipClientTab(TraderInterfaceScreen screen, OwnershipTab tab) { super(screen, tab); }
	
	@Nonnull
    @Override
	public IconData getIcon() { return IconData.of(Items.PLAYER_HEAD); }

	@Override
	public ITextComponent getTooltip() { return EasyText.translatable("tooltip.lightmanscurrency.settings.owner"); }
	
	@Override
	public boolean blockInventoryClosing() { return true; }
	
	TextFieldWidget newOwnerInput;
	Button buttonSetOwner;
	TeamSelectWidget teamSelection;
	Button buttonSetTeamOwner;
	
	long selectedTeam = -1;
	List<Team> teamList = Lists.newArrayList();
	
	IconButton buttonToggleMode;
	
	boolean playerMode = true;
	
	@Override
	public void onOpen() {
		
		this.newOwnerInput = this.screen.addRenderableTabWidget(new TextFieldWidget(this.font, this.screen.getGuiLeft() + 23, this.screen.getGuiTop() + 26, 160, 20, EasyText.empty()));
		this.newOwnerInput.setMaxLength(16);
		
		this.buttonSetOwner = this.screen.addRenderableTabWidget(new Button(this.screen.getGuiLeft() + 23, this.screen.getGuiTop() + 47, 160, 20, EasyText.translatable("gui.button.lightmanscurrency.set_owner"), this::setOwner));
		this.buttonSetOwner.active = false;
		
		this.teamSelection = this.screen.addRenderableTabWidget(new TeamSelectWidget(this.screen.getGuiLeft() + 13, this.screen.getGuiTop() + 25, 4, () -> this.teamList, this::getSelectedTeam, this::selectTeam));
		this.teamSelection.init(screen::addRenderableTabWidget, this.font);
		
		this.buttonSetTeamOwner = this.screen.addRenderableTabWidget(new Button(this.screen.getGuiLeft() + 23, this.screen.getGuiTop() + 117, 160, 20, EasyText.translatable("gui.button.lightmanscurrency.set_owner"), this::setTeamOwner));
		this.buttonSetTeamOwner.active = false;
		
		this.buttonToggleMode = this.screen.addRenderableTabWidget(new IconButton(this.screen.getGuiLeft() + this.screen.getXSize() - IconButton.SIZE - 3, this.screen.getGuiTop() + 3, this::toggleMode, this::getModeIcon, new IconAndButtonUtil.ToggleTooltip(() -> this.playerMode, EasyText.translatable("tooltip.lightmanscurrency.settings.owner.player"), EasyText.translatable("tooltip.lightmanscurrency.settings.owner.team"))));
		
		this.tick();
		
	}
	
	private IconData getModeIcon() { return this.playerMode ? IconData.of(Items.PLAYER_HEAD) : IconData.of(Items.WRITABLE_BOOK); }
	
	private Team getTeam(int teamIndex)
	{
		if(teamIndex < this.teamList.size())
			return this.teamList.get(teamIndex);
		return null;
	}
	
	private Team getSelectedTeam()
	{
		if(this.selectedTeam < 0)
			return null;
		return TeamSaveData.GetTeam(true, this.selectedTeam);
	}
	
	private void refreshTeamList()
	{
		this.teamList = Lists.newArrayList();
		List<Team> allTeams = TeamSaveData.GetAllTeams(true);
		allTeams.forEach(team ->{
			if(team.isMember(this.menu.player))
				this.teamList.add(team);
		});
		this.teamList.sort(Team.sorterFor(this.menu.player));
	}

	@Override
	public void renderBG(MatrixStack pose, int mouseX, int mouseY, float partialTicks) {
		
		if(this.menu.getBE() == null)
			return;
		
		this.font.draw(pose, TextRenderUtil.fitString(EasyText.translatable("gui.button.lightmanscurrency.team.owner", this.menu.getBE().getOwnerName()), this.screen.getXSize() - 20), this.screen.getGuiLeft() + 10, this.screen.getGuiTop() + 10, 0x404040);
		
	}
	
	public boolean changeInTrades() {
		TradeData referencedTrade = this.menu.getBE().getReferencedTrade();
		TradeData trueTrade = this.menu.getBE().getTrueTrade();
		if(referencedTrade == null)
			return false;
		if(trueTrade == null)
			return true;
		return !referencedTrade.compare(trueTrade).Identical();
	}

	@Override
	public void renderTooltips(MatrixStack pose, int mouseX, int mouseY) {
		
		if(this.menu.getBE() == null)
			return;
		
		//Render button tooltips
		if(this.buttonSetOwner.isMouseOver(mouseX, mouseY) || this.buttonSetTeamOwner.isMouseOver(mouseX, mouseY))
		{
			this.screen.renderTooltip(pose, EasyText.translatable("tooltip.lightmanscurrency.warning").withStyle(TextFormatting.BOLD, TextFormatting.YELLOW), mouseX, mouseY);
		}
		
		IconAndButtonUtil.renderButtonTooltips(pose, mouseX, mouseY, Lists.newArrayList(this.buttonToggleMode));
		
	}
	
	private void toggleMode(Button button) {
		this.playerMode = !this.playerMode;
	}
	
	private void setOwner(Button button)
	{
		if(this.newOwnerInput.getValue().isEmpty())
			return;
		this.commonTab.setNewOwner(this.newOwnerInput.getValue());
		this.newOwnerInput.setValue("");
	}
	
	private void setTeamOwner(Button button) {
		if(this.selectedTeam < 0)
			return;
		this.commonTab.setNewTeam(this.selectedTeam);
		this.selectedTeam = -1;
	}
	
	private void selectTeam(int teamIndex)
	{
		Team newTeam = this.getTeam(teamIndex);
		if(newTeam != null)
		{
			if(newTeam.getID() == this.selectedTeam)
				this.selectedTeam = -1;
			else
				this.selectedTeam = newTeam.getID();
		}
	}
	
	@Override
	public void tick() {
		this.refreshTeamList();
		
		this.newOwnerInput.tick();
		
		this.buttonSetOwner.visible = this.newOwnerInput.visible = this.playerMode;
		this.buttonSetTeamOwner.visible = this.teamSelection.visible = !this.playerMode;
		
		this.buttonSetOwner.active = !this.newOwnerInput.getValue().isEmpty();
		this.buttonSetTeamOwner.active = this.getSelectedTeam() != null;
	}

	@Override
	public void onClose() {
		//Reset the selected team & team list to save space
		this.selectedTeam = -1;
		this.teamList = Lists.newArrayList();
	}

}