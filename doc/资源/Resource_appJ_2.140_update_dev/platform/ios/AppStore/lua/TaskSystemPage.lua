require "OP_TaskSystem_pb"
require "TaskSystem_pb"
require "IncPbCommon"
require "JumpPage"
require "GuideLogic"

local EverydayTaskSystemContent = {}
function EverydayTaskSystemContent.onFunction(eventName,container)
    if eventName == "luaRefreshItemView" then
        EverydayTaskSystemContent.onRefreshItemView(container)
    elseif eventName == "onAceeptBtn" then
        EverydayTaskSystemContent.onAcceptBtn(container)
    elseif eventName == "onESACFace" then
        EverydayTaskSystemContent.onESACFace(container)
    end
end

function EverydayTaskSystemContent.onRefreshItemView(container)
    local serverDate = EverydayTaskInfoList[container:getItemDate().mID]
    local contentData = TaskTable[serverDate.taskID]
    local name = contentData.taskGoal .. " (" .. serverDate.taskProgress .. "/" .. contentData.taskTimes .. ")"
    local resInfo = getResTable(contentData.taskRewards)[1]
    resInfo = ResManager:getInstance():getResInfoByTypeAndId(resInfo.type,resInfo.itemId,resInfo.count)    
    
    container:getVarLabelBMFont("mESACName"):setString(name)
    
    local s = contentData.taskDescribe
    local descirbe = ""
    s, descirbe = GameMaths:stringAutoReturn(s, descirbe, 12, 0)
    container:getVarLabelBMFont("mIntroLabel"):setString(descirbe)
    container:getVarLabelBMFont("mESACNum"):setString(resInfo.count)
    container:getVarSprite("mIcoPic"):setTexture(resInfo.icon)
    container:getVarLabelBMFont("mESACActive"):setString(contentData.taskActivityNum)
    
    local color = VaribleManager:getInstance():getSetting("FrameColor_Quality" .. contentData.taskQuality)
    local colorRGB = StringConverter:parseColor3B(color)
    container:getVarMenuItemImage("mESACFace"):setColor(colorRGB)

    container:getVarMenuItemImage("mAceeptBtn"):setVisible(true)
    local label = container:getVarLabelBMFont("mESACAceept")
    label:setVisible(true)
    if serverDate.taskStatus == 1 then
        local str = Language:getInstance():getString("@Task_Accept")
        label:setString(str)
    elseif serverDate.taskStatus == 2 then
        if contentData.taskJumpPage ~= "none" then
            local str = Language:getInstance():getString("@Task_JumpPage")
            label:setString(str)
        else
            container:getVarMenuItemImage("mAceeptBtn"):setVisible(false)
            label:setVisible(false)
        end 
    elseif serverDate.taskStatus == 3 then
        local str = Language:getInstance():getString("@Task_Finish")
        label:setString(str)
    end
end

function EverydayTaskSystemContent.onESACFace(container)
    local serverDate = EverydayTaskInfoList[container:getItemDate().mID]
    local contentData = TaskTable[serverDate.taskID]
    local resInfo = getResTable(contentData.taskRewards)[1]
    local type = ResManager:getInstance():getResMainType(resInfo.type)
    if type == 31 then --Disciple
		DiscipleHandInfoPage:showDisciplePage(resInfo.itemId,true)
	elseif type == 32 then --Soul
        BlackBoard:getInstance().ShowSoul = resInfo.itemId
		local gamemsg = MsgMainFramePushPage:new()
        gamemsg.pageName = "SoulInfoPage"
        MessageManager:getInstance():sendMessageForScript(gamemsg)  
	elseif type == 50 then --equip
		EquipHandInfoPage:showEquipPage(resInfo.itemId,true)
	elseif type == 41 or  type == 42 then --skill
		SkillHandInfoPage:showSkillPage(resInfo.itemId,true)
	elseif type == 60 then --good
	    local toolItem = ToolTableManager:getInstance():getToolItemByID(resInfo.itemId)
	    if toolItem.includeStr == "none" then
		    PropInfoPage:showPropInfoPage(resInfo.itemId, 2, false)
		else
		    local resInfos = getResTable(toolItem.includeStr)
            for i=1,table.maxn(resInfos) do
                local info = resInfos[i]
                info = ResManager:getInstance():getResInfoByTypeAndId(info.type,info.itemId,info.count) 
                GiftPreviewPage:addContent(info.name,info.icon,info.count,info.quality)
            end
            GiftPreviewPage:showPage("@GiftPackPreviewText")
		end
	elseif type == 10 then --USER_PROPERTY
	end
end

function EverydayTaskSystemContent.onAcceptBtn(container)
    local serverDate = EverydayTaskInfoList[container:getItemDate().mID]
    local contentData = TaskTable[serverDate.taskID]
    if serverDate.taskStatus == 1 then
        local msg = TaskSystem_pb.OPAcceptTask()
	    msg.taskID = serverDate.taskID
	    local pb_data = msg:SerializeToString()
	    PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_ACCEPTTASK_C,pb_data,#pb_data,true)
    elseif serverDate.taskStatus == 2 and contentData.taskJumpPage ~= "none" then
        
        local canjump = jumpPage(contentData.taskJumpPage,contentData.taskJumSubTabId,contentData.taskJumpType,contentData.taskJumpCondition,contentData.taskJumpFailedStr)
        if canjump and contentData.taskGuideFunc then
            GuideFuncs[contentData.taskGuideFunc]()
        end
    elseif serverDate.taskStatus == 3 then
        local msg = TaskSystem_pb.OPGetEverydayTaskReward()
	    msg.taskID = serverDate.taskID
	    local pb_data = msg:SerializeToString()
	    PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_GETEVERYDAYTASKREWARD_C,pb_data,#pb_data,true)
    end
end

local GrowingTaskSystemContent = {}
function GrowingTaskSystemContent.onFunction(eventName,container)
    if eventName == "luaRefreshItemView" then
        GrowingTaskSystemContent.onRefreshItemView(container)
    elseif eventName == "onProceedButton" then
        GrowingTaskSystemContent.onProceedButton(container)
    elseif eventName == "onESRCFace" then
        GrowingTaskSystemContent.onESRCFace(container)
    end
end

function GrowingTaskSystemContent.onRefreshItemView(container)
    local serverDate = GrowingTaskInfoList[container:getItemDate().mID]
    local contentData = TaskTable[serverDate.taskID]
    local resInfo = getResTable(contentData.taskRewards)[1]
    resInfo = ResManager:getInstance():getResInfoByTypeAndId(resInfo.type,resInfo.itemId,resInfo.count)  

    container:getVarLabelBMFont("mObjLabel"):setString(contentData.taskGoal)
    container:getVarLabelBMFont("mESRCTem"):setString(serverDate.taskProgress .. "/" .. contentData.taskTimes)
    
    local s = contentData.taskDescribe
    local descirbe = ""
    s, descirbe = GameMaths:stringAutoReturn(s, descirbe, 13, 0)
    container:getVarLabelBMFont("mIntroLabel"):setString(descirbe)
    container:getVarSprite("mEncItemPic"):setTexture(resInfo.icon)
    
    if resInfo.count < 10000 then
        container:getVarLabelBMFont("mESRCnum1"):setString(tostring(resInfo.count))
    else
        container:getVarLabelBMFont("mESRCnum1"):setString(tostring(resInfo.count/10000) .. "W")
    end
    
    container:getVarMenuItemImage("mProceedButton"):setVisible(true)
    local label = container:getVarLabelBMFont("mProLabel")
    label:setVisible(true)
    local effect = container:getVarNode("mBlingBlingEffect")
    effect:setVisible(false)
    if serverDate.taskStatus <= 3 then
        container:getVarSprite("mEncFinish"):setVisible(false)
        if contentData.taskJumpPage ~= "none" then
            local str = Language:getInstance():getString("@Task_JumpPage")
            label:setString(str)
            if serverDate.taskStatus < 3 then
                effect:setVisible(true)
                container:runAnimation("Default Timeline")
            end
        else
            container:getVarMenuItemImage("mProceedButton"):setVisible(false)
            label:setVisible(false)
        end
    elseif serverDate.taskStatus == 4 then
        local str = Language:getInstance():getString("@Task_Finish")
        container:getVarLabelBMFont("mProLabel"):setString(str)
        container:getVarSprite("mEncFinish"):setVisible(true)
    end
    
    if serverDate.taskStatus < 2 then
        GrowingTaskInfoList[container:getItemDate().mID].taskStatus = 2
        local msg = TaskSystem_pb.OPGrowingTaskView()
	    msg.version = 1
	    local pb_data = msg:SerializeToString()
	    PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_GROWINGTASKVIEW_C,pb_data,#pb_data,false)
    end
    
end

function GrowingTaskSystemContent.onESRCFace(container)
    local serverDate = GrowingTaskInfoList[container:getItemDate().mID]
    local contentData = TaskTable[serverDate.taskID]
    local resInfo = getResTable(contentData.taskRewards)[1]
    local type = ResManager:getInstance():getResMainType(resInfo.type)
    if type == 31 then --Disciple
		DiscipleHandInfoPage:showDisciplePage(resInfo.itemId,true)
	elseif type == 32 then --Soul
        BlackBoard:getInstance().ShowSoul = resInfo.itemId
		local gamemsg = MsgMainFramePushPage:new()
        gamemsg.pageName = "SoulInfoPage"
        MessageManager:getInstance():sendMessageForScript(gamemsg)  
	elseif type == 50 then --equip
		EquipHandInfoPage:showEquipPage(resInfo.itemId,true)
	elseif type == 41 or  type == 42 then --skill
		SkillHandInfoPage:showSkillPage(resInfo.itemId,true)
	elseif type == 60 then --good
	    local toolItem = ToolTableManager:getInstance():getToolItemByID(resInfo.itemId)
	    if toolItem.includeStr == "none" then
		    PropInfoPage:showPropInfoPage(resInfo.itemId, 2, false)
		else
		    local resInfos = getResTable(toolItem.includeStr)
            for i=1,table.maxn(resInfos) do
                local info = resInfos[i]
                info = ResManager:getInstance():getResInfoByTypeAndId(info.type,info.itemId,info.count) 
                GiftPreviewPage:addContent(info.name,info.icon,info.count,info.quality)
            end
            GiftPreviewPage:showPage("@GiftPackPreviewText")
		end
	elseif type == 10 then --USER_PROPERTY
	end
end

function GrowingTaskSystemContent.onProceedButton(container)
    local serverDate = GrowingTaskInfoList[container:getItemDate().mID]
    local contentData = TaskTable[serverDate.taskID]
    if serverDate.taskStatus <= 3 then
        if contentData.taskJumpPage ~= "none" then
            if serverDate.taskStatus < 3 then
                GrowingTaskInfoList[container:getItemDate().mID].taskStatus = 3
                local msg = TaskSystem_pb.OPGrowingTaskJump()
	            msg.taskID = GrowingTaskInfoList[container:getItemDate().mID].taskID
	            local pb_data = msg:SerializeToString()
	            PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_GROWINGTASKJUMP_C,pb_data,#pb_data,false)
            end
            local canjump = jumpPage(contentData.taskJumpPage,contentData.taskJumSubTabId,contentData.taskJumpType,contentData.taskJumpCondition,contentData.taskJumpFailedStr)
            if canjump and contentData.taskGuideFunc then
                GuideFuncs[contentData.taskGuideFunc]()
            end
        end
    elseif serverDate.taskStatus == 4 then
        local msg = TaskSystem_pb.OPGetGrowingTaskReward()
	    msg.taskID = GrowingTaskInfoList[container:getItemDate().mID].taskID
	    local pb_data = msg:SerializeToString()
	    PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_GETGROWINGTASKREWARD_C,pb_data,#pb_data,true)
    end
end

SellectHelpID = 0
local TaskSystemHelpContent = {}
function TaskSystemHelpContent.onFunction(eventName,container)
    if eventName == "luaRefreshItemView" then
        TaskSystemHelpContent.onRefreshItemView(container)
    elseif eventName == "onESHCMenuItem" then
        TaskSystemHelpContent.onESHCMenuItem(container)
    end
end

function TaskSystemHelpContent.onRefreshItemView(container)
    local contentData = TaskHelpTable[container:getItemDate().mID]
    container:getVarLabelBMFont("mESHCIndex"):setString(contentData.name)
    container:getVarLabelBMFont("mESHCIcoPic")
    local mHead=container:getVarSprite("mESHCIcoPic")
    if mHead~=nil then
        mHead:setVisible(false)
    end
end

function TaskSystemHelpContent.onESHCMenuItem(container)
    local gamemsg = MsgMainFramePushPage:new()
    gamemsg.pageName = "TaskSystemHelpPopPage"
    MessageManager:getInstance():sendMessageForScript(gamemsg)
    SellectHelpID = container:getItemDate().mID
--[[
    local contentData = TaskHelpTable[container:getItemDate().mID]
    if contentData.taskJumpPage ~= "none" then
        if contentData.taskJumpType == 0 then
            BlackBoard:getInstance().ToAdventruePageType = contentData.taskJumSubTabId
            local gamemsg = MsgMainFrameChangePage:new()
            gamemsg.pageName = contentData.taskJumpPage
            MessageManager:getInstance():sendMessageForScript(gamemsg)
        elseif contentData.taskJumpType == 1 then
            BlackBoard:getInstance().ToAdventruePageType = 0
            local gamemsg = MsgMainFramePushPage:new()
            gamemsg.pageName = contentData.taskJumpPage
            MessageManager:getInstance():sendMessageForScript(gamemsg)
        end
    end
--]]
end

local TaskSystemPage = {}

function luaCreat_TaskSystemPage(container)
    CCLuaLog("OnCreat_TaskSystemPage")
    container:registerFunctionHandler(TaskSystemPage.onFunction)
end

function TaskSystemPage.onFunction(eventName,container)
    if eventName == "luaEnter" then
        TaskSystemPage.onEnter(container)
    elseif eventName == "luaExit" then
        TaskSystemPage.onExit(container)
    elseif eventName == "luaLoad" then
        TaskSystemPage.onLoad(container)
    elseif eventName == "luaReceivePacket" then
		TaskSystemPage.onReceivePacket(container)
	elseif eventName == "onActivityButton" then
		TaskSystemPage.onActivityButton(container)
	elseif eventName == "onRewardsButton" then
		TaskSystemPage.onRewardsButton(container)
	elseif eventName == "onHelpButton" then
		TaskSystemPage.onHelpButton(container)
	elseif eventName == "onEncQuit" then
		TaskSystemPage.onEncQuit(container)
	elseif eventName == "onDirectBtn" then
		TaskSystemPage.onDirectBtn(container)
	elseif eventName == "onFreeBtn" then
		TaskSystemPage.onFreeBtn(container)
	elseif eventName == "onESACIcoPic1" then
		TaskSystemPage.onESACFace(container,1)
	elseif eventName == "onESACIcoPic2" then
		TaskSystemPage.onESACFace(container,2)
	elseif eventName == "onESACIcoPic3" then
		TaskSystemPage.onESACFace(container,3)
	elseif eventName == "onESACIcoPic4" then
		TaskSystemPage.onESACFace(container,4)
    end
end

function TaskSystemPage.onActivityButton(container)
    TaskSystemPage.ChangeTab(container,1)
end

function TaskSystemPage.onRewardsButton(container)
    TaskSystemPage.ChangeTab(container,2)
end

function TaskSystemPage.onHelpButton(container)
    TaskSystemPage.ChangeTab(container,3)
end

function TaskSystemPage.onEncQuit(container)
    local gamemsg = MsgMainFramePopPage:new()
    gamemsg.pageName = "TaskSystemPage"
    MessageManager:getInstance():sendMessageForScript(gamemsg)
end

function TaskSystemPage.onDirectBtn(container)
    container:registerPacket(OP_TaskSystem_pb.OPCODE_DONEEVERYDAYTASKRET_S)
    local msg = TaskSystem_pb.OPDoneEverydayTask()
	msg.version = 1
	local pb_data = msg:SerializeToString()
	PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_DONEEVERYDAYTASK_C,pb_data,#pb_data,true)
end

function TaskSystemPage.onFreeBtn(container)
    if lastRefreshNum == 0 then
        MessageBoxPage:Msg_Box("@EverydayTask_Refresh_Over")
    else
        container:registerPacket(OP_TaskSystem_pb.OPCODE_REFRESHEVERYDAYTASKRET_S)
        local msg = TaskSystem_pb.OPRefreshEverydayTask()
	    msg.version = 1
	    local pb_data = msg:SerializeToString()
	    PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_REFRESHEVERYDAYTASK_C,pb_data,#pb_data,true)
    end
end

function TaskSystemPage.onESACFace(container,index)
    local serverDate = ActivityPackageInfoList[index]
    local status = serverDate.taskStatus
    local package = ToolTableManager:getInstance():getToolItemByID(serverDate.taskID)
    if status == 1 then
        local resInfos = getResTable(package.includeStr)
        for i=1,table.maxn(resInfos) do
            local info = resInfos[i]
            info = ResManager:getInstance():getResInfoByTypeAndId(info.type,info.itemId,info.count) 
            GiftPreviewPage:addContent(info.name,info.icon,info.count,info.quality)
        end
        GiftPreviewPage:showPage("@GiftPackPreviewText")
    elseif status == 2 then
        container:registerPacket(OP_TaskSystem_pb.OPCODE_GETTASKREWARDPACKAGERET_S)
        local msg = TaskSystem_pb.OPGetTaskRewardPackage()
	    msg.packageID = serverDate.Type
	    local pb_data = msg:SerializeToString()
	    PacketManager:getInstance():sendPakcet(OP_TaskSystem_pb.OPCODE_GETTASKREWARDPACKAGE_C,pb_data,#pb_data,true)
	    TaskSystemPage.packageItemID = serverDate.taskID
    elseif status == 3 then
        MessageBoxPage:Msg_Box_Lan("@Task_ActivityPackage_AlreadyReward")
    end
    local icon = container:getVarMenuItemImage("mESACIcoPic"..tostring(serverDate.Type))
	if serverDate.taskStatus == 1 then
		icon:setColor(ccc3(128,128,128))
	else
	    icon:setColor(ccc3(255,255,255))
	end 
end

function TaskSystemPage.onEnter(container)
    TaskSystemPage.sellectTab = 0
    if BlackBoard:getInstance():hasVarible("TaskSystemPageTab") then
        local tab = tonumber((BlackBoard:getInstance():getVarible("TaskSystemPageTab"))) --getVarible return 2 value
        if tab == 0 then 
            TaskSystemPage.ChangeTab(container,1)
        else
            TaskSystemPage.ChangeTab(container,tab)
        end
    else
        TaskSystemPage.ChangeTab(container,1)
    end
    
    TaskSystemPage.ChangeTab(container,TaskSystemPage.sellectTab)
    container:registerPacket(OP_TaskSystem_pb.OPCODE_ACCEPTTASKRET_S)
    container:registerPacket(OP_TaskSystem_pb.OPCODE_GETEVERYDAYTASKREWARDRET_S)
    container:registerPacket(OP_TaskSystem_pb.OPCODE_GETGROWINGTASKREWARDRET_S)
end

function TaskSystemPage.onExit(container)	
    TaskSystemPage.clearAllItem1(container)
    container.m_pScrollViewFacade1:delete()
	container.m_pScrollViewFacade1 = nil
    TaskSystemPage.clearAllItem2(container)
	container.m_pScrollViewFacade2:delete()
	container.m_pScrollViewFacade2 = nil
end

function TaskSystemPage.onLoad(container)
	container:loadCcbiFile("EncSystemPage.ccbi");
	container.mScrollView1 = container:getVarScrollView("mESACSv")
	container.mScrollViewRootNode1 = container.mScrollView1:getContainer()
	container.m_pScrollViewFacade1 = CCReViScrollViewFacade:new(container.mScrollView1)
	container.m_pScrollViewFacade1:init(6,6)
	container.mScrollView2 = container:getVarScrollView("mESRCSv")
	container.mScrollViewRootNode2 = container.mScrollView2:getContainer()
	container.m_pScrollViewFacade2 = CCReViScrollViewFacade:new(container.mScrollView2)
	container.m_pScrollViewFacade2:init(6,6)
	TaskSystemPage.mExpbarInitScale = container:getVarSprite("mESACExp"):getScaleX()
end

function TaskSystemPage.ChangeTab(container,index)
    if TaskSystemPage.sellectTab ~= index then
        TaskSystemPage.sellectTab=index
        TaskSystemPage.refreshPage(container)
        TaskSystemPage.rebuildAllItem(container)
    end

    if TaskSystemPage.sellectTab==1 then
        container:getVarMenuItemImage("mActivityButton"):selected()
        container:getVarMenuItemImage("mRewardsButton"):unselected()
        container:getVarMenuItemImage("mHelpButton"):unselected()
    elseif TaskSystemPage.sellectTab==2 then
        container:getVarMenuItemImage("mActivityButton"):unselected()
        container:getVarMenuItemImage("mRewardsButton"):selected()
        container:getVarMenuItemImage("mHelpButton"):unselected()
    else
        container:getVarMenuItemImage("mActivityButton"):unselected()
        container:getVarMenuItemImage("mRewardsButton"):unselected()
        container:getVarMenuItemImage("mHelpButton"):selected()
    end
end

function TaskSystemPage.refreshPage(container)
    container:getVarLabelBMFont("mESACTaskNum1"):setString(TaskSystemBaseInfo.taskNum)
    container:getVarLabelBMFont("mESACTaskNum2"):setString(TaskSystemBaseInfo.taskMaxNum)
    container:getVarLabelBMFont("mESACLveNum"):setString(TaskSystemBaseInfo.activity)
    if TaskSystemPage.sellectTab==1 then
        container:getVarLabelBMFont("mDirectGoldNum1"):setString(TaskSystemBaseInfo.donePrice)
        
        if TaskSystemBaseInfo.lastRefreshNum ~= 0 then
            container:getVarNode("mFreeRefreshNode"):setVisible(false)
            local label = container:getVarLabelBMFont("mESACRefreshLabel")
            if label then
                local str = Language:getInstance():getString("@FreeRefresh")
                label:setString(str)
            end
        else
            container:getVarNode("mFreeRefreshNode"):setVisible(true)
            container:getVarLabelBMFont("mDirectGoldNum2"):setString(TaskSystemBaseInfo.refreshPrice)
            local label = container:getVarLabelBMFont("mESACRefreshLabel")
            if label then
                local str = Language:getInstance():getString("@GoldRefresh")
                label:setString(str)
            end
        end
        
        container:getVarNode("mESACNode"):setVisible(true)
        container:getVarNode("mESRCSv"):setVisible(false)
        for i=#ActivityPackageInfoList, 1, -1 do
	        local serverDate = ActivityPackageInfoList[i]
	        local iconPic = ToolTableManager:getInstance():getToolItemByID(serverDate.taskID).iconPic
	        local icon = container:getVarMenuItemImage("mESACIcoPic"..tostring(serverDate.Type))
	        --local sprite = CCSprite:create(iconPic)
	        --icon:setNormalImage(sprite)
	        --sprite = CCSprite:create(iconPic)
            --icon:setSelectedImage(sprite)
	        if serverDate.taskStatus == 1 then
			    icon:setColor(ccc3(128,128,128))
	        else
	            icon:setColor(ccc3(255,255,255))
	        end 
	    end
        local scale = TaskSystemBaseInfo.activity/100
        if scale >1 then scale = 1 end
        container:getVarSprite("mESACExp"):setScaleX(scale*TaskSystemPage.mExpbarInitScale)
    else
        container:getVarNode("mESACNode"):setVisible(false)
        container:getVarNode("mESRCSv"):setVisible(true)
	end
	
    local finishnum = 0
    for k, v in ipairs(EverydayTaskInfoList) do
        if v.taskStatus == 3 then
            finishnum = finishnum+1
        end
    end
    if finishnum~=0 then
        container:getVarNode("mEncNew1"):setVisible(true)
        container:getVarLabelBMFont("mEncNewNum1"):setString(tostring(finishnum))
    else
        container:getVarNode("mEncNew1"):setVisible(false)
    end
    
    finishnum = 0
    for k, v in ipairs(GrowingTaskInfoList) do
        if v.taskStatus == 4 then
	        finishnum = finishnum+1
        end
    end
    if finishnum~=0 then
        container:getVarNode("mEncNew2"):setVisible(true)
        container:getVarLabelBMFont("mEncNewNum2"):setString(tostring(finishnum))
    else
        container:getVarNode("mEncNew2"):setVisible(false)
    end
end

function TaskSystemPage.rebuildAllItem(container)
    if TaskSystemPage.sellectTab==1 then
        TaskSystemPage.clearAllItem1(container)
        TaskSystemPage.buildItem1(container)
    else
        TaskSystemPage.clearAllItem2(container)
	    TaskSystemPage.buildItem2(container)
	end
end

function TaskSystemPage.buildItem1(container)
    local iMaxNode = container.m_pScrollViewFacade1:getMaxDynamicControledItemViewsNum()
	local iCount = 0
	local fOneItemHeight = 0
	local fOneItemWidth = 0

    for i=#EverydayTaskInfoList, 1, -1 do
    --for k, v in ipairs(EverydayTaskInfoList) do
		local pItemData = CCReViSvItemData:new()
		pItemData.mID =  i
		pItemData.m_iIdx = i
		pItemData.m_ptPosition = ccp(0, fOneItemHeight*iCount)
		if iCount < iMaxNode then
			local pItem = ScriptContentBase:create("EncSystemActivityContent1.ccbi")
			pItem.id = iCount
			pItem:registerFunctionHandler(EverydayTaskSystemContent.onFunction)
			if  fOneItemHeight < pItem:getContentSize().height then
				fOneItemHeight = pItem:getContentSize().height
			end
			if fOneItemWidth < pItem:getContentSize().width then
				fOneItemWidth = pItem:getContentSize().width
			end
			container.m_pScrollViewFacade1:addItem(pItemData, pItem.__CCReViSvItemNodeFacade__)
		else
               container.m_pScrollViewFacade1:addItem(pItemData)
        end
		iCount = iCount+1
	end
	local size = CCSizeMake(fOneItemWidth, fOneItemHeight*iCount)
	container.mScrollView1:setContentSize(size)
	container.mScrollView1:setContentOffset(ccp(0, container.mScrollView1:getViewSize().height - container.mScrollView1:getContentSize().height*container.mScrollView1:getScaleY()))
	container.m_pScrollViewFacade1:setDynamicItemsStartPosition(iCount-1)
	
	container.mScrollView1:forceRecaculateChildren()
end

function TaskSystemPage.clearAllItem1(container)
    container.m_pScrollViewFacade1:clearAllItems()
    container.mScrollViewRootNode1:removeAllChildren()
end

local offset = nil
function TaskSystemPage.buildItem2(container)
    local iMaxNode = container.m_pScrollViewFacade2:getMaxDynamicControledItemViewsNum()
	local iCount = 0
	local fOneItemHeight = 0
	local fOneItemWidth = 0
    
    local ItemList = nil
    local ccbpage = nil
    local Content = nil
    if TaskSystemPage.sellectTab==2 then
        ItemList = GrowingTaskInfoList
        ccbpage = "EncSystemRewardsContent.ccbi"
        Content = GrowingTaskSystemContent
        
        table.sort(ItemList, 
        function (e1, e2)
            if not e2 then return true end
            if not e1 then return false end
              
            if e1.taskStatus ~= e2.taskStatus then
                if e1.taskStatus == 4 then
				    return true
				elseif e2.taskStatus == 4 then
				    return false
				elseif e1.taskCreateTime == e2.taskCreateTime then 
				    return e1.taskID < e2.taskID
				else
                    return e1.taskCreateTime > e2.taskCreateTime
				end
			else
				if e1.taskCreateTime == e2.taskCreateTime then 
				    return e1.taskID < e2.taskID
				else
                    return e1.taskCreateTime > e2.taskCreateTime
				end
			end
        end
	    )
    else
        ItemList = TaskHelpTable
        ccbpage = "EncSystemHelpContent.ccbi"
        Content = TaskSystemHelpContent
	end
	
	for i=#ItemList, 1, -1 do
    --for k, v in pairs(ItemList) do
		local pItemData = CCReViSvItemData:new()
		pItemData.mID =  i
		pItemData.m_iIdx = i
		pItemData.m_ptPosition = ccp(0, fOneItemHeight*iCount)
		
		if iCount < iMaxNode then
			local pItem = ScriptContentBase:create(ccbpage)
			pItem.id = iCount
			pItem:registerFunctionHandler(Content.onFunction)
			if  fOneItemHeight < pItem:getContentSize().height then
				fOneItemHeight = pItem:getContentSize().height
			end
			if fOneItemWidth < pItem:getContentSize().width then
				fOneItemWidth = pItem:getContentSize().width
			end
			container.m_pScrollViewFacade2:addItem(pItemData, pItem.__CCReViSvItemNodeFacade__)
		else
            container.m_pScrollViewFacade2:addItem(pItemData)
        end
		iCount = iCount+1
	end
	local size = CCSizeMake(fOneItemWidth, fOneItemHeight*iCount)
	container.mScrollView2:setContentSize(size)
	if offset then
	    container.mScrollView2:setContentOffset(offset)
	else
	    container.mScrollView2:setContentOffset(ccp(0, container.mScrollView2:getViewSize().height - container.mScrollView2:getContentSize().height*container.mScrollView2:getScaleY()))
	end
    container.m_pScrollViewFacade2:setDynamicItemsStartPosition(iCount-1)
	
	container.mScrollView2:forceRecaculateChildren()
end

function TaskSystemPage.clearAllItem2(container)
    container.m_pScrollViewFacade2:clearAllItems()
    container.mScrollViewRootNode2:removeAllChildren()
end

function TaskSystemPage.TaskReward(container,reward)
    for k, v in ipairs(reward.toolInfo) do
	    DropManager.gotTool(v)
    end
    for k, v in ipairs(reward.equipInfo) do
	    DropManager.gotEquipment(v)
    end
    for k, v in ipairs(reward.skillInfo) do
	    DropManager.gotSkill(v)
    end
    for k, v in ipairs(reward.soulInfo) do
	    DropManager.gotSoul(v)
    end
    for k, v in ipairs(reward.disciple) do
	    DropManager.gotDisciple(v)
    end
    if reward:HasField("goldcoins") then
        ServerDateManager:getInstance():getUserBasicInfo().goldcoins = reward.goldcoins
    end
    if reward:HasField("silvercoins") then
        ScriptMathToLua:modifySilverCoins(tonumber(reward.silvercoins))
    end
end

function TaskSystemPage.onReceivePacket(container)
    if container:getRecPacketOpcode() == OP_TaskSystem_pb.OPCODE_ACCEPTTASKRET_S then
        local msg = TaskSystem_pb.OPAcceptTaskRet()
	    local msgbuff = container:getRecPacketBuffer()
	    msg:ParseFromString(msgbuff)
	    if msg:HasField("taskNum") then
            TaskSystemBaseInfo.taskNum = msg.taskNum
        end
        if msg:HasField("everydayTaskInfo") then
            local taskInfo = EverydayTaskInfoList_getInfobyTaskID(msg.everydayTaskInfo.taskID)
            if taskInfo then
	            taskInfo["taskStatus"] = msg.everydayTaskInfo.taskStatus
	            taskInfo["taskProgress"] = msg.everydayTaskInfo.taskProgress
            end
        end
	    if msg.status == 1 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Success")
		    TaskSystemPage.refreshPage(container)
		    TaskSystemPage.rebuildAllItem(container)
		elseif msg.status == 0 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Faild")
		elseif msg.status == 2 then
		    MessageBoxPage:Msg_Box("@EverydayTask_AlreadyHas")
		elseif msg.status == 3 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Accept_UpMaxNum")
		end
    elseif container:getRecPacketOpcode() == OP_TaskSystem_pb.OPCODE_REFRESHEVERYDAYTASKRET_S then
        local msg = TaskSystem_pb.OPRefreshEverydayTaskRet()
	    local msgbuff = container:getRecPacketBuffer()
	    msg:ParseFromString(msgbuff)
	    if msg:HasField("lastRefreshNum") then
            TaskSystemBaseInfo.lastRefreshNum = msg.lastRefreshNum
        end
        if msg:HasField("refreshPrice") then
            TaskSystemBaseInfo.refreshPrice = msg.refreshPrice
        end
        if msg:HasField("gold") then
            ServerDateManager:getInstance():getUserBasicInfo().goldcoins = msg.gold
            local gamemsg = MsgTitleStatusChange:new()
            MessageManager:getInstance():sendMessageForScript(gamemsg)
        end
        
        if #msg.everydayTaskInfos > 0 then
            EverydayTaskInfoList = {}
            for k, v in ipairs(msg.everydayTaskInfos) do
                EverydayTaskInfoList[k] = {}
	            EverydayTaskInfoList[k]["taskID"] = v.taskID
	            EverydayTaskInfoList[k]["taskStatus"] = v.taskStatus
	            EverydayTaskInfoList[k]["taskProgress"] = v.taskProgress
            end
        end
        
	    if msg.status == 1 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Refresh_Success")
		    TaskSystemPage.refreshPage(container)
		    TaskSystemPage.rebuildAllItem(container)
		elseif msg.status == 0 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Refresh_Faild")
		elseif msg.status == 2 then
		    MessageBoxPage:Msg_Box("@EverydayTask_goldless")
		elseif msg.status == 3 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Refresh_UpMaxNum")
		end
    elseif container:getRecPacketOpcode() == OP_TaskSystem_pb.OPCODE_DONEEVERYDAYTASKRET_S then
        local msg = TaskSystem_pb.OPDoneEverydayTaskRet()
	    local msgbuff = container:getRecPacketBuffer()
	    msg:ParseFromString(msgbuff)
	    if msg:HasField("lastDoneNum") then
            TaskSystemBaseInfo.lastDoneNum = msg.lastDoneNum
        end
        if msg:HasField("activity") then
            TaskSystemBaseInfo.activity = msg.activity
        end
        if msg:HasField("gold") then
            ServerDateManager:getInstance():getUserBasicInfo().goldcoins = msg.gold
            local gamemsg = MsgTitleStatusChange:new()
            MessageManager:getInstance():sendMessageForScript(gamemsg)
        end
        if msg:HasField("taskNum") then
            TaskSystemBaseInfo.taskNum = msg.taskNum
        end
        
        if #msg.everydayTaskInfos > 0 then
            EverydayTaskInfoList = {}
            for k, v in ipairs(msg.everydayTaskInfos) do
                EverydayTaskInfoList[k] = {}
	            EverydayTaskInfoList[k]["taskID"] = v.taskID
	            EverydayTaskInfoList[k]["taskStatus"] = v.taskStatus
	            EverydayTaskInfoList[k]["taskProgress"] = v.taskProgress
            end
        end
        
        if #msg.activityPackageInfos > 0 then
            ActivityPackageInfoList = {}
            for k, v in ipairs(msg.activityPackageInfos) do
                ActivityPackageInfoList[k] = {}
	            ActivityPackageInfoList[k]["taskID"] = v.taskID
	            ActivityPackageInfoList[k]["taskStatus"] = v.taskStatus
	            ActivityPackageInfoList[k]["Type"] = v.Type
            end
        end
        
        if msg:HasField("taskReward") then
            TaskSystemPage.TaskReward(container,msg.taskReward)
        end
        
	    if msg.status == 1 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Done_Success")
		    TaskSystemPage.refreshPage(container)
		    TaskSystemPage.rebuildAllItem(container)
		elseif msg.status == 0 then
		    MessageBoxPage:Msg_Box("@EverydayTask_NoAcceptTask")
		elseif msg.status == 2 then
		    MessageBoxPage:Msg_Box("@EverydayTask_goldless")
		elseif msg.status == 3 then
		    MessageBoxPage:Msg_Box("@EverydayTask_Done_UpMaxNum")
		end
	elseif container:getRecPacketOpcode() == OP_TaskSystem_pb.OPCODE_GETEVERYDAYTASKREWARDRET_S then
        local msg = TaskSystem_pb.OPGetEverydayTaskRewardRet()
	    local msgbuff = container:getRecPacketBuffer()
	    msg:ParseFromString(msgbuff)
        if msg:HasField("activity") then
            TaskSystemBaseInfo.activity = msg.activity
        end

        if #msg.everydayTaskInfos > 0 then
            EverydayTaskInfoList = {}
            for k, v in ipairs(msg.everydayTaskInfos) do
                EverydayTaskInfoList[k] = {}
	            EverydayTaskInfoList[k]["taskID"] = v.taskID
	            EverydayTaskInfoList[k]["taskStatus"] = v.taskStatus
	            EverydayTaskInfoList[k]["taskProgress"] = v.taskProgress
            end
        end
        
        setNewTask()
        
        if #msg.activityPackageInfos > 0 then
            ActivityPackageInfoList = {}
            for k, v in ipairs(msg.activityPackageInfos) do
                ActivityPackageInfoList[k] = {}
	            ActivityPackageInfoList[k]["taskID"] = v.taskID
	            ActivityPackageInfoList[k]["taskStatus"] = v.taskStatus
	            ActivityPackageInfoList[k]["Type"] = v.Type
            end
        end
        
        if msg:HasField("taskReward") then
            TaskSystemPage.TaskReward(container,msg.taskReward)
        end
        
        if msg.status == 1 then
		    MessageBoxPage:Msg_Box("@EverydayTask_GetReward_Success")
            TaskSystemBaseInfo.taskNum = tonumber(TaskSystemBaseInfo.taskNum) + 1
		    TaskSystemPage.refreshPage(container)
		    TaskSystemPage.rebuildAllItem(container)
		elseif msg.status == 0 then
		    MessageBoxPage:Msg_Box("@EverydayTask_GetReward_Faild")
		elseif msg.status == 2 then
		    MessageBoxPage:Msg_Box("@EverydayTask_GetReward_AlreadyFinish")
		elseif msg.status == 3 then
		    MessageBoxPage:Msg_Box("@EverydayTask_GetReward_UpMaxNum")
		end
	elseif container:getRecPacketOpcode() == OP_TaskSystem_pb.OPCODE_GETTASKREWARDPACKAGERET_S then
	    local msg = TaskSystem_pb.OPGetTaskRewardPackageRet()
	    local msgbuff = container:getRecPacketBuffer()
	    msg:ParseFromString(msgbuff)

        if msg:HasField("activityPackageInfo") then
            local rewardInfo = ActivityPackageInfoList_getInfobyTaskID(msg.activityPackageInfo.taskID)
            if rewardInfo then
	            rewardInfo["taskStatus"] = msg.activityPackageInfo.taskStatus
	            rewardInfo["Type"] = msg.activityPackageInfo.Type
            end
        end
        
        if msg:HasField("taskReward") then
            TaskSystemPage.TaskReward(container,msg.taskReward)
            if TaskSystemPage.packageItemID then
                local package = ToolTableManager:getInstance():getToolItemByID(TaskSystemPage.packageItemID)
                local gamemsg = MsgMainFramePushPage:new()
                gamemsg.pageName = "PackPreviewPage"
                MessageManager:getInstance():sendMessageForScript(gamemsg)
                
                local resInfos = getResTable(package.includeStr)
                for i=1,table.maxn(resInfos) do
                    local resInfo = resInfos[i]
                    resInfo = ResManager:getInstance():getResInfoByTypeAndId(resInfo.type,resInfo.itemId,resInfo.count) 
                    local info = ServerDateManager:getInstance():getAndCreateResInfo(i)
                    info.count = resInfo.count
				    info.itemId = resInfo.itemId
					info.type = resInfo.type
					info.describe = resInfo.describe
					info.icon = resInfo.icon
					info.name = resInfo.name
					info.quality = resInfo.quality
                end
                
                local packmsg = MsgPackPreviewSourceMsg:new()
                packmsg.index = 2
                MessageManager:getInstance():sendMessageForScript(packmsg)
                
                TaskSystemPage.packageItemID = nil
            end
        end
        
        if msg.status == 1 then
		    MessageBoxPage:Msg_Box("@Task_GetActivityPackage_Success")
		elseif msg.status == 0 then
		    MessageBoxPage:Msg_Box("@Task_GetActivityPackage_AlreadyFinish")
		elseif msg.status == 2 then
		    MessageBoxPage:Msg_Box("@Task_GetActivityPackage_LowActivity")
		elseif msg.status == 4 then
		    MessageBoxPage:Msg_Box("@Task_GetActivityPackage_NoItem")
		end
	elseif container:getRecPacketOpcode() == OP_TaskSystem_pb.OPCODE_GETGROWINGTASKREWARDRET_S then
	    local msg = TaskSystem_pb.OPGetGrowingTaskRewardRet()
	    local msgbuff = container:getRecPacketBuffer()
	    msg:ParseFromString(msgbuff)
        
        if #msg.growingTaskInfos >0 then
            GrowingTaskInfoList = {}
            for k, v in ipairs(msg.growingTaskInfos) do
                GrowingTaskInfoList[k] = {}
	            GrowingTaskInfoList[k]["taskID"] = v.taskID
	            GrowingTaskInfoList[k]["taskStatus"] = v.taskStatus
	            GrowingTaskInfoList[k]["taskProgress"] = v.taskProgress
	            GrowingTaskInfoList[k]["taskCreateTime"] = v.taskCreateTime
            end
        end
        
        setNewTask()
        
        if msg:HasField("taskReward") then
            TaskSystemPage.TaskReward(container,msg.taskReward)
        end
        
        if msg.status == 1 then
		    MessageBoxPage:Msg_Box("@GrowingTask_GetReward_Success")
		    TaskSystemPage.refreshPage(container)
		    TaskSystemPage.rebuildAllItem(container)
		elseif msg.status == 0 then
		    MessageBoxPage:Msg_Box("@GrowingTask_GetReward_NoTask")
		elseif msg.status == 2 then
		    MessageBoxPage:Msg_Box("@GrowingTask_GetReward_NoFinish")
		end
    end
end
