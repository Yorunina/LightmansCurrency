package io.github.lightman314.lightmanscurrency.client.gui.screen;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamBankAccountTab;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamMemberEditTab;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamMemberListTab;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamNameTab;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamOwnerTab;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamSelectionTab;
import io.github.lightman314.lightmanscurrency.client.gui.team.TeamTab;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.TabButton;
import io.github.lightman314.lightmanscurrency.client.util.RenderUtil;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.teams.TeamSaveData;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class TeamManagerScreen extends Screen {

	public static final ResourceLocation GUI_TEXTURE =  new ResourceLocation(LightmansCurrency.MODID, "textures/gui/teammanager.png");
	
	public TeamManagerScreen() {
		super(EasyText.empty());
		this.tabs.forEach(tab -> tab.setScreen(this));
	}
	
	public PlayerEntity getPlayer() { return this.minecraft.player; }
	public FontRenderer getFont() { return this.font; }
	
	public final int guiLeft() { return (this.width - this.xSize) / 2; }
	public final int guiTop() { return (this.height - this.ySize) / 2; }
	public final int xSize = 200;
	public final int ySize = 200;
	
	private long activeTeamID = -1;
	public Team getActiveTeam()
	{
		if(this.activeTeamID < 0)
			return null;
		Team team = TeamSaveData.GetTeam(true, this.activeTeamID);
		if(team != null && team.isMember(this.getPlayer()))
			return team;
		return null;
	}
	public void setActiveTeam(long teamID) { this.activeTeamID = teamID; }
	
	List<Widget> tabWidgets = Lists.newArrayList();
	List<IGuiEventListener> tabListeners = Lists.newArrayList();
	
	List<TeamTab> tabs = Lists.newArrayList(TeamSelectionTab.INSTANCE, TeamMemberListTab.INSTANCE, TeamNameTab.INSTANCE, TeamMemberEditTab.INSTANCE, TeamBankAccountTab.INSTANCE, TeamOwnerTab.INSTANCE);
	public TeamTab currentTab() { return tabs.get(MathUtil.clamp(currentTabIndex, 0, this.tabs.size() - 1)); }
	List<TabButton> tabButtons = Lists.newArrayList();
	int currentTabIndex = 0;
	
	@Override
	public void init()
	{
		//Initialize the tab buttons
		for(int i = 0; i < this.tabs.size(); ++i)
		{
			TabButton button = this.addButton(new TabButton(this::clickedOnTab, this.font, this.tabs.get(i)));
			button.active = i != this.currentTabIndex;
			button.visible = this.tabs.get(i).allowViewing(this.getPlayer(), this.getActiveTeam());
			this.tabButtons.add(button);
		}
		this.positionTabButtons();
		
		//Initialize the starting tab
		this.currentTab().initTab();
		
	}
	
	private int getTabPosX(int index)
	{
		if(index < 8)
			return this.guiLeft() + 25 * index;
		if(index < 16)
			return this.guiLeft() + this.xSize;
		if(index < 24)
			return this.guiLeft() + this.xSize - 25 * (index - 15);
		return this.guiLeft() - 25;
	}
	
	private int getTabPosY(int index)
	{
		if(index < 8)
			return this.guiTop() - 25;
		if(index < 16)
			return this.guiTop() + 25 * (index - 10);
		if(index < 24)
			return this.guiTop() + this.ySize;
		return this.guiTop() + this.ySize - 25 * (index - 23);
	}
	
	private int getTabRotation(int index)
	{
		if(index < 8)
			return 0;
		if(index < 16)
			return 1;
		if(index < 24)
			return 2;
		return 3;
	}
	
	private void positionTabButtons()
	{
		int index = 0;
		for (TabButton thisButton : this.tabButtons) {
			if (thisButton.visible) {
				thisButton.reposition(this.getTabPosX(index), this.getTabPosY(index), this.getTabRotation(index));
				index++;
			}
		}
	}
	
	@Override
	public void render(@Nonnull MatrixStack pose, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(pose);
		//Render the background
		RenderUtil.bindTexture(GUI_TEXTURE);
		RenderUtil.color4f(1f, 1f, 1f, 1f);
		this.blit(pose, this.guiLeft(), this.guiTop(), 0, 0, this.xSize, this.ySize);
		//Render the tab buttons
		super.render(pose, mouseX, mouseY, partialTicks);
		
		try {
			//Pre-render the tab
			this.currentTab().preRender(pose, mouseX, mouseY, partialTicks);
			//Render the renderables
			this.tabWidgets.forEach(widget -> widget.render(pose, mouseX, mouseY, partialTicks));
			//Post-render the tab
			this.currentTab().postRender(pose, mouseX, mouseY, partialTicks);
		} catch(Exception ignored) { }
		
		
		//Render the tab button tooltips
		for (TabButton tabButton : this.tabButtons) {
			if (tabButton.isMouseOver(mouseX, mouseY))
				this.renderTooltip(pose, tabButton.tab.getTooltip(), mouseX, mouseY);
		}
	}
	
	
	@Override
	public void tick()
	{
		if(this.activeTeamID < 0 && this.currentTabIndex != 0)
		{
			this.changeTab(0);
		}
		//Update the tabs visibility
		boolean updateTabs = false;
		for(int i = 0; i < this.tabs.size(); ++i)
		{
			boolean visible = this.tabs.get(i).allowViewing(this.getPlayer(), this.getActiveTeam());
			if(visible != this.tabButtons.get(i).visible)
			{
				updateTabs = true;
				this.tabButtons.get(i).visible = visible;
			}
		}
		if(updateTabs)
			this.positionTabButtons();
		
		if(!this.currentTab().allowViewing(this.getPlayer(), this.getActiveTeam()) && this.currentTabIndex != 0)
			this.changeTab(0);
		
		//Tick the current tab
		this.currentTab().tick();
	}
	
	public <T extends Widget> T addRenderableTabWidget(T widget)
	{
		this.tabWidgets.add(widget);
		return widget;
	}
	
	public void removeRenderableTabWidget(Widget widget)
	{
		this.tabWidgets.remove(widget);
	}
	
	public <T extends IGuiEventListener> T addTabListener(T listener)
	{
		this.tabListeners.add(listener);
		return listener;
	}
	
	public void removeTabListener(IGuiEventListener listener)
	{
		this.tabListeners.remove(listener);
	}
	
	public void changeTab(int tabIndex)
	{
		//Confirm that the new tab can be viewed
		if(!this.tabs.get(tabIndex).allowViewing(this.getPlayer(), this.getActiveTeam()))
			return;
		
		//Close the old tab
		this.currentTab().closeTab();
		this.tabButtons.get(this.currentTabIndex).active = true;
		this.currentTabIndex = tabIndex;
		this.tabButtons.get(this.currentTabIndex).active = false;
		
		//Clear the previous tabs widgets
		this.tabWidgets.clear();
		this.tabListeners.clear();
		
		//Initialize the new tab
		this.currentTab().initTab();
	}
	
	private void clickedOnTab(Button tab)
	{
		int tabIndex = this.tabButtons.indexOf(tab);
		if(tabIndex < 0)
			return;
		this.changeTab(tabIndex);
	}
	
	@Nonnull
	@Override
	public List<? extends IGuiEventListener> children()
	{
		List<? extends IGuiEventListener> coreListeners = super.children();
		List<IGuiEventListener> listeners = Lists.newArrayList();
		listeners.addAll(coreListeners);
		listeners.addAll(this.tabWidgets);
		listeners.addAll(this.tabListeners);
		return listeners;
	}
	
	@Override
	public boolean isPauseScreen() { return false; }
	
}