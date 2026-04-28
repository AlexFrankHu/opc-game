----------------------------------------------------------------------------------
--[[
FILE:			SkillPage.lua
ENCODING:		UTF-8, no-bomb
DESCRIPTION:	鑳介噺涔嬫牳: 鎺㈢储銆佹嫾鍚堛�佸垪琛ㄩ〉闈?
AUTHOR:			hgs
CREATED:		2014-01-21
--]]
----------------------------------------------------------------------------------
RegisterLuaPage("ReplaceSkillPage");
local hp = require('HP_pb')
local skillPb = require("Skill_pb")
local SkillManager = require("SkillManager")
local NewbieGuideManager = require("NewbieGuideManager")

local thisPageName = "SkillPage";
local NodeHelper = require("NodeHelper");

local mRebuildLock = true
local mRefreshCout = 0

local option = {
	ccbiFile = "SkillPage.ccbi",
	handlerMap = {
		onHelp				= "showHelp",
		onSkillspecialty	= "enhanceSkill",
		onReplaceSkill		= "replaceSkill",
		onFightSkill		= "onFightSkill",
		onArenaSkill		= "onArenaSkill",
		onDefenseSkill	 	= "onDefenseSkill"
		
	},
	opcode = opcodes
}

local skillCfg = ConfigManager.getSkillCfg();
local skillOpenCfg = ConfigManager.getSkillOpenCfg()

local SkillPageBase = {}
local SkillPageNormalContent = {}
local SkillPageEmptyContent = {}

--------------------------------------------------------------
local SkillItem = {
	ccbiFile_empty 	= "SkillEmptyContent.ccbi",
	ccbiFile_close	 	= "SkillNotOpenContent.ccbi",
	ccbiFile_open	 	= "SkillOpenContent.ccbi",
}

-------------------------------------------------------------


local PageType = {
    FIGHT_SKILL = 1,
    ARENA_SKILL = 2,
	DEFENSE_SKILL = 3
}

local currPageType = PageType.ARENA_SKILL
local mMainContainer = nil
local mInterval = -700
----------------------------------------------------------------------------------

-----------------------------------------------
--SkillPageBase椤甸潰涓殑浜嬩欢澶勭悊
----------------------------------------------
function SkillPageBase:onEnter(container)
	NewbieGuideManager.guide(GameConfig.NewbieGuide.FirstSkill)
	container:registerMessage(MSG_MAINFRAME_REFRESH)
	self:registerPackets(container)
	NodeHelper:initScrollView(container, "mContent", 3);
	container.scrollview=container:getVarScrollView("mContent");	
	
	if container.scrollview~=nil then
		container:autoAdjustResizeScrollview(container.scrollview);
		mTableInterval = container.mScrollView:getViewSize().width + 100
		mTableInterval = -mTableInterval
	end		
	
	local mScale9Sprite = container:getVarScale9Sprite("mScale9Sprite")
	if mScale9Sprite ~= nil then
		container:autoAdjustResizeScale9Sprite( mScale9Sprite )
	end
	
	mMainContainer = container
	
	self:rebuildAllItem(container);
end

function SkillPageBase:onExecute(container)
end

function SkillPageBase:onExit(container)
    container.mScrollView:getContainer():stopAllActions()
	self:removePackets(container)
	container:removeMessage(MSG_MAINFRAME_REFRESH)
	NodeHelper:clearMultiColumnScrollView(container)
	NodeHelper:deleteScrollView(container);
	mMainContainer = nil
	mRebuildLock = true;
end


function SkillPageBase.changeTab(container,index)
    if index == 0 then
        currPageType = 2
    elseif index == 1 then
        currPageType = 3
    else 
        currPageType = 1
    end
    
    SkillPageBase:selectTab( container )
end 

function SkillPageBase:selectTab( container , offset,isDuration)
	if currPageType == PageType.FIGHT_SKILL then
		container:getVarMenuItem("mFightSkill"):selected()
		container:getVarMenuItem("mFightSkill"):setEnabled(false)
		container:getVarMenuItem("mArenaSkill"):unselected()
		container:getVarMenuItem("mArenaSkill"):setEnabled(true) 
		container:getVarMenuItem("mDefenseSkill"):unselected()
		container:getVarMenuItem("mDefenseSkill"):setEnabled(true)
		
		container:getVarNode("mExplain1"):setVisible(true)
		container:getVarNode("mExplain2"):setVisible(false)
		container:getVarNode("mExplain3"):setVisible(false)
	elseif currPageType == PageType.ARENA_SKILL then
		container:getVarMenuItem("mFightSkill"):unselected()
		container:getVarMenuItem("mFightSkill"):setEnabled(true)
		container:getVarMenuItem("mArenaSkill"):selected() 
		container:getVarMenuItem("mArenaSkill"):setEnabled(false)
		container:getVarMenuItem("mDefenseSkill"):unselected()
		container:getVarMenuItem("mDefenseSkill"):setEnabled(true)
		
		container:getVarNode("mExplain1"):setVisible(false)
		container:getVarNode("mExplain2"):setVisible(true)
		container:getVarNode("mExplain3"):setVisible(false)
	else
		container:getVarMenuItem("mFightSkill"):unselected()
		container:getVarMenuItem("mFightSkill"):setEnabled(true)
		container:getVarMenuItem("mArenaSkill"):unselected() 
		container:getVarMenuItem("mArenaSkill"):setEnabled(true)
		container:getVarMenuItem("mDefenseSkill"):selected()
		container:getVarMenuItem("mDefenseSkill"):setEnabled(false)
		
		container:getVarNode("mExplain1"):setVisible(false)
		container:getVarNode("mExplain2"):setVisible(false)
		container:getVarNode("mExplain3"):setVisible(true)
	end
	
	if offset~=nil then
	    local original = container.mScrollView:getContentOffset()
	    if original.x~=offset then
	        original.x = offset
	        container.mScrollView:getContainer():stopAllActions()
	        if isDuration==true then 
	             container.mScrollView:setContentOffsetInDuration(original,0.3)
	        else
	            container.mScrollView:setContentOffset(original,false)
	        end
	        
	    end
	end
end

function SkillPageBase:onFightSkill( container,message )
	currPageType = PageType.FIGHT_SKILL
	local offset = mTableInterval * 2
	
	local isDuration
	if message==nil or message~="NoneAction" then
	    isDuration = true
	else
	    isDuration = false
	end
	self:selectTab( container,offset,isDuration)
	
	
	--self:rebuildAllItem(container)
end

function SkillPageBase:onArenaSkill( container,message )
	currPageType = PageType.ARENA_SKILL
	local offset = 0
	
	local isDuration
	if message==nil or message~="NoneAction" then
	    isDuration = true
	else
	    isDuration = false
	end
	self:selectTab( container,offset,isDuration)
	
	
	--self:rebuildAllItem(container)
end

function SkillPageBase:onDefenseSkill( container,message )
	currPageType = PageType.DEFENSE_SKILL
	local offset = mTableInterval * 1
	
	local isDuration
	if message==nil or message~="NoneAction" then
	    isDuration = true
	else
	    isDuration = false
	end
	self:selectTab( container,offset,isDuration)
	
	--self:rebuildAllItem( container )
end
-----------------------------------------------------
---------------------子节点-----------------------------------
local ChildScrollViewContent = {
    ccbiFile = "BackpackContent.ccbi"
};

function ChildScrollViewContent.onFunction(eventName, container)
	if eventName == "luaRefreshItemView" then
		ChildScrollViewContent.onRefreshItemView(container);
	end
end


function ChildScrollViewContent.onRefreshItemView(container)
    if container.mScrollView == nil then
        NodeHelper:initScrollView(container, "mBackpackContent", 4);
        if mMainContainer~=nil then
            mMainContainer:autoAdjustResizeScrollview(container.mScrollView);
            mMainContainer.ChildContentBase =  mMainContainer.ChildContentBase or {}
            table.insert(mMainContainer.ChildContentBase,container)
        end
        ChildScrollViewContent.clearAllItem(container)
        ChildScrollViewContent.buildAllItem(container)
    end
end

function ChildScrollViewContent.clearAllItem(container)
    NodeHelper:clearScrollView(container);
end

function ChildScrollViewContent.buildAllItem(container)
    local contentId = container:getItemDate().mID;
    local assignedSkills
    if contentId == 1 then
        assignedSkills = SkillManager:getFightSkillList()
    elseif contentId == 2 then
        assignedSkills = SkillManager:getDefenseSkillList()
    else
        assignedSkills = SkillManager:getArenaSkillList()
    end
    if assignedSkills == nil then
		CCLuaLog("ERROR  in assignedSkills == null");
		return
	end

    local skillSize = #assignedSkills

    local iCount = 0
	local iMaxNode = container.m_pScrollViewFacade:getMaxDynamicControledItemViewsNum()
	local fOneItemHeight = 0
	local fOneItemWidth = 0
	local oneHeight = 0
    for i=4, 1, -1 do
		local pItemData = CCReViSvItemData:new_local()
		pItemData.mID = i
		pItemData.m_iIdx = i
		pItemData.m_ptPosition = ccp(0, oneHeight)

		local pItem
		if i<= skillSize then
			local skillItemId = SkillManager:getSkillItemIdUsingId(assignedSkills[i])
			-- create pItem
			if skillItemId > 0 then
				-- carried
				local ccbiFile = SkillItem.ccbiFile_open
				pItem = ScriptContentBase:create(ccbiFile)
				local newStr =GameMaths:stringAutoReturnForLua(skillCfg[skillItemId]["describe"], 17, 0)
				local lb2Str = {
					mSkillName 		= skillCfg[skillItemId]["name"],
					mConsumptionMp 	= skillCfg[skillItemId]["costMP"],
					mSkillTex 		= newStr,
					mNumber			= tostring(i)
				}
				NodeHelper:setStringForLabel(pItem, lb2Str);					
				local sprite2Img = {
					mChestPic = skillCfg[skillItemId]["icon"],
				}
				NodeHelper:setSpriteImage(pItem, sprite2Img);
				if i == 1 then
					pItem:getVarMenuItemImage("mMobile"):setEnabled(false)
				end
				pItem:registerFunctionHandler(SkillPageNormalContent.onFunction)
			else
				-- empty 
				--鏄剧ず宸插紑鍚?
				local ccbiFile = SkillItem.ccbiFile_empty;
				pItem = ScriptContentBase:create(ccbiFile);
				pItem:registerFunctionHandler(SkillPageEmptyContent.onFunction)
			end
		else -- not open skills
			local ccbiFile = SkillItem.ccbiFile_close;
			pItem = ScriptContentBase:create(ccbiFile);
			local label = pItem:getVarLabelBMFont('mOpenLevel')
			if label then
				local openLevel = skillOpenCfg[i] and skillOpenCfg[i].openLevel or 0
				label:setString(common:getLanguageString('@OpenLevel', openLevel))
			end
		end

		if fOneItemHeight < pItem:getContentSize().height then
			fOneItemHeight = pItem:getContentSize().height
		end
		oneHeight = oneHeight + pItem:getContentSize().height
		if fOneItemWidth < pItem:getContentSize().width then
			fOneItemWidth = pItem:getContentSize().width
		end

		if iCount < iMaxNode then
			container.m_pScrollViewFacade:addItem(pItemData, pItem.__CCReViSvItemNodeFacade__)
		else
			container.m_pScrollViewFacade:addItem(pItemData)
		end
		iCount = iCount + 1
	end
	
	local size = CCSizeMake(fOneItemWidth,oneHeight)
	container.mScrollView:setContentSize(size)
	container.mScrollView:setContentOffset(ccp(0, container.mScrollView:getViewSize().height - container.mScrollView:getContentSize().height * container.mScrollView:getScaleY()))
	container.m_pScrollViewFacade:setDynamicItemsStartPosition(iCount - 1);
	container.mScrollView:forceRecaculateChildren();
end




----------------scrollview-------------------------
function SkillPageBase:rebuildAllItem(container)
    --预防同一时间刷新多次
    if mRebuildLock then
        mRebuildLock = false
        self:clearAllItem(container);
	    self:buildItem(container);
        
        --延迟1s
        container:runAction(
			CCSequence:createWithTwoActions(
				CCDelayTime:create(0.1),
				CCCallFunc:create(function()
					mRebuildLock = true;
					--判断是否有未被刷新的情况存在，无论未被刷新多少次都只重新刷新一次
					if mRefreshCout > 0 then
					    mRefreshCout = 0
					    self:rebuildAllItem(container)
					end
				end)
			)
		);
	else
	--记录下未被刷新的次数
	    mRefreshCout = mRefreshCout + 1;
	end
end

function SkillPageBase:clearAllItem(container)
    container.mScrollView:getContainer():stopAllActions()
    NodeHelper:clearMultiColumnScrollView(container)
	NodeHelper:clearScrollView(container);
end

function SkillPageBase:buildItem(container)
	
	
	--浜虹墿鎸備笂鎶�鑳界殑娓呭崟鍦≧oleInfo閲岄潰鐨凴oleSkill鏁扮粍涓?
	UserInfo.sync()

	local buildTable = {
        onTabChange = SkillPageBase.changeTab,
        totalSize = 3
    }
	local buildOne = {
        ccbiFile = ChildScrollViewContent.ccbiFile,
        size = 3,
        funcCallback = ChildScrollViewContent.onFunction
    }
    
    table.insert(buildTable,buildOne)
    NodeHelper:buildMultiColumnScrollView(container,buildTable,100)
	
	if currPageType == PageType.FIGHT_SKILL then
		SkillPageBase:onFightSkill( container,"NoneAction")
	elseif currPageType == PageType.ARENA_SKILL then
		SkillPageBase:onArenaSkill( container,"NoneAction")
	elseif currPageType == PageType.DEFENSE_SKILL then
		SkillPageBase:onDefenseSkill( container,"NoneAction")
	end
	
	--鑲畾涓?涓綅缃?	
	
end

----------------click event------------------------
function SkillPageBase:showHelp(container)
	PageManager.showHelp(GameConfig.HelpKey.HELP_SKILL)
end

--鎶�鑳戒笓绮撅紝鏆傛湭寮�鏀?
function SkillPageBase:enhanceSkill(container)
	MessageBoxPage:Msg_Box('@CommingSoon')
end

function SkillPageBase:replaceSkill(container)
	ReplaceSkillPage_setBagId(currPageType)
	PageManager.pushPage("ReplaceSkillPage")
end

-- ----------------------- empty content----------------------- 
function SkillPageEmptyContent.onFunction(eventName,container)
	if eventName == "luaRefreshItemView" then
		SkillPageEmptyContent.refreshItemView(container)
	elseif eventName == "mSkillBtn" then
	    ReplaceSkillPage_setBagId(currPageType)
		PageManager.pushPage("ReplaceSkillPage")
	end
	
end

function SkillPageEmptyContent.refreshItemView(container)
end
-- ----------------------- open content----------------------- 
function SkillPageNormalContent.onFunction(eventName,container)
	if eventName == "luaRefreshItemView" then
	elseif eventName == "onMobile" then
		SkillPageNormalContent.onMobile(container);
	end
end

function SkillPageNormalContent.onMobile(container)
	local index = container:getItemDate().mID
	UserInfo.sync()
	local assignedSkills
	if currPageType == PageType.FIGHT_SKILL then
		assignedSkills = SkillManager:getFightSkillList()
	elseif currPageType == PageType.ARENA_SKILL then
		assignedSkills = SkillManager:getArenaSkillList()
	elseif currPageType == PageType.DEFENSE_SKILL then
		assignedSkills = SkillManager:getDefenseSkillList()
	end
	local msg = skillPb.HPSkillChangeOrder()
	msg.roleId = UserInfo.roleInfo.roleId
	msg.skillId = assignedSkills[index]
	if index < 2 then MessageBoxPage:Msg_Box('@error index in skill reorder') return end
	msg.srcOrder = index - 1
	msg.dstOrder = index - 2
	msg.skillBagId = currPageType
	local pb = msg:SerializeToString()
	PacketManager:getInstance():sendPakcet(hp.SKILL_CHANGE_ORDER_C, pb, #pb, true)
end
------------------------------- packet function -----------------------------------------

function SkillPageBase:registerPackets(container)
	container:registerPacket(hp.ROLE_CARRY_SKILL_S)
	container:registerPacket(hp.SKILL_CHANGE_ORDER_S)
end

function SkillPageBase:removePackets(container)
	container:removePacket(hp.ROLE_CARRY_SKILL_S)
	container:removePacket(hp.SKILL_CHANGE_ORDER_S)
end

function SkillPageBase:onReceivePacket(container)
	local opcode = container:getRecPacketOpcode()
	local msgBuff = container:getRecPacketBuffer()

	if opcode == hp.SKILL_CHANGE_ORDER_S then
		self:rebuildAllItem(container)
		return
	end

	if opcode == hp.ROLE_CARRY_SKILL_S then
		self:rebuildAllItem(container)
		return
	end
end

function SkillPageBase:onReceiveMessage(container)
	local message = container:getMessage()
	local typeId = message:getTypeId()
	if typeId == MSG_MAINFRAME_REFRESH then
		local pageName = MsgMainFrameRefreshPage:getTrueType(message).pageName;
		if pageName == thisPageName then
			self:rebuildAllItem(container)
		end
	end
end

-------------------------------------------------------------------------
return SkillPageBase
