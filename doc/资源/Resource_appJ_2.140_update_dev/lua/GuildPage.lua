----------------------------------------------------------------------------------
--[[
	FILE:			GuildPage.lua
	ENCODING:		UTF-8, no-bomb
	DESCRIPTION:	公会主页面
	AUTHOR:			sunyj
	CREATED:		2014-8-4

	you know: 		Guild == Alliance
--]]
----------------------------------------------------------------------------------

RegisterLuaPage('GuildSearchPopPage')
RegisterLuaPage('GuildRankingPage')
RegisterLuaPage('GuildCreatePage')
RegisterLuaPage('GuildShopPage')
RegisterLuaPage('GuildMembersPage')
RegisterLuaPage('GuildManagePage')
RegisterLuaPage('GuildBossHarmRankPage')
RegisterLuaPage('GuildOpenBossConfirmPage')

local alliance = require('Alliance_pb')
local hp = require('HP_pb')
local player = require('Player_pb')
local NodeHelper = require("NodeHelper")
local firstEnterBossPage = true
local AppstoreHandle = require("AppstoreHandle")

local option = {
	-- when i don't have a alliance
	ccbiFile = "GuildPage.ccbi",
	-- when i have a alliance
	ccbiWithAlliance = 'GuildPartakePage.ccbi',
	handlerMap = {
		-- basic event
		luaInit = "onInit",
		luaLoad = "onLoad",
		luaUnLoad = "onUnload",
		luaExecute = "onExecute",
		luaEnter = "onEnter",
		luaExit = "onExit",
		luaOnAnimationDone = "onAnimationDone",
		luaReceivePacket = "onReceivePacket",
		luaGameMessage = "onReceiveMessage",
		luaInputboxEnter = "onInputboxEnter",
		luaSendPacketFailed = "onPacketError",
		luaConnectFailed = "onPacketError",
		luaTimeout = "onPacketError",
		luaPacketError = "onPacketError",

		-- ccbi: GuildPage.ccbi ：未加入公会，搜索、创建、加入。
		onRefreshList 			= 'refreshJoinList',
		onSearchGuild 			= 'onSearchGuild',
		onEstablishGuild 		= 'onCreateGuild',
		onGuildContend 			= 'onGuildBattle',
		onRanking 				= 'onGuildRanking',
		onHelp 					= 'onHelp',

		-- ccbi: GuildPartakePage.ccbi ：已加入公会，签到、boss、成员、管理。
		onSignIn				= "onSignIn",
		onIntrusion				= "onIntrude",
		onContributionExchange	= "exchangeContribution",
		onMembers				= "showMembers",
		onManage				= "onManage",
		onRank					= "onRank",
		onAutoFight 			= "onAutoFight",
	},
	bossHander = {
		onOpenBossIntrusion				= "openBoss",
		onInspireIntrusion 				= 'onInspire',
		onContributionRankingIntrusion 	= 'onContributionRanking',
		onAttributeOpen 				= "onAttributeOpen",
	}
}

-- 公会操作类型
local OperType = {
	ChangeLeader = 1,
	ChangeViceLeader = 2,
	JoinAlliance = 3,
	QuitAlliance = 4,
	DemoteViceLeader = 5,
}

local PositionType = {
	Leader = 2,
	ViceLeader = 1,
	Normal = 0,
}

-- 创建公会等级要求
local CreateAllianceOpenLevel = 18

allianceInfo = {
	-- 公会基本信息
	commonInfo = nil,
	-- 可加入公会列表
	joinList = nil,
}

-- 我的公会个人信息
MyAllianceInfo = {}

local enterPageTime = 0

-- 初始化公会商店、公会排名、boss排行、成员列表标志位
local shopInfoInited = false
local rankInfoInited = false
local bossRankInited = false
local memberInfoInited = false

local GuildPage = {}

local BossPage = {
	-- CONST
	FreeTimePerWeek = 2, 		-- boss 免费开启次数
	InspirePercent = 20, 		-- boss 鼓舞增加百分比
	InspireCost = 20, 			-- boss 鼓舞花费钻石
	-- end CONST

	-- BOSS STATE
	BossNotOpen = 1, 			-- boss状态：未开启
	BossCanJoin = 2, 			-- boss状态：已开启，可加入
	BossCanInspire = 3, 		-- boss状态：已加入，可鼓舞

	CDTimeKey = 'BossIntrusionCD',

	-- BOSS OPERATION
	BossOperOpen = 1, 			-- boss操作：开始boss
	BossOperJoin = 2, 			-- boss操作：加入boss战斗
	BossOperInspire = 3, 		-- boss操作：鼓舞

	bossBloodLeft = 0,

	bossJoinFlag = false,
}

BossPage.bossCfg = ConfigManager.getAllianceBossCfg()
local mainContainer = {}
local joinListContainer = {}
local allianceContainer = {}
local bossContainer = {}
local bossHitContainer = {}

-- now guild refresh page num
local nowRefreshPageNum = 1
---------------------------- 页面重置 ---------------------------------
function BossPage.reset()
	BossPage.bossBloodLeft = 0
	BossPage.bossJoinFlag = false
end

function GuildPage.reset()
	enterPageTime = 0
	shopInfoInited = false
	rankInfoInited = false
	bossRankInited = false
	memberInfoInited = false

	allianceInfo.commonInfo = nil
	allianceInfo.joinList = nil
	MyAllianceInfo = {}
	
	mainContainer = {}
	joinListContainer = {}
	allianceContainer = {}
	bossContainer = {}
	bossHitContainer = {}
end

function ResetGuildPage()
	BossPage.reset()
	GuildPage.reset()
end
---------------------------- 页面重置 ---------------------------------

-----------------------------------------------
--GuildPage页面中的事件处理
----------------------------------------------
function GuildPage.onFunction(eventName, container)
	local funcName = option.handlerMap[eventName]
	if funcName then
		if not GuildPage[funcName] then
			GuildPage[funcName] = function(container) end
		end
		GuildPage[funcName](container, eventName)
	else
		CCLuaLog('In GuildPage: unknown function name: ' .. eventName)
	end
end

function GuildPage.onLoad(container)
	-- 主节点
    mainContainer = container

	-- '有公会节点'
	allianceContainer = ScriptContentBase:create(option.ccbiWithAlliance)
	allianceContainer:registerFunctionHandler(GuildPage.onFunction)
	-- 'boss节点', 嵌入在'有公会节点'
	local bossNode = allianceContainer:getVarNode('mPartakeGuildBossIntrusionItem')
	if bossNode then
		bossContainer = ScriptContentBase:create('GuildBossIntrusionItem.ccbi')

		-- boss飘血动画节点, 嵌入在'boss节点'
		bossHitContainer = ScriptContentBase:create('BattleNormalNum.ccbi')
		local bossAniNode = bossContainer:getVarNode('mPersonHitNumberNode')
		bossAniNode:addChild(bossHitContainer)
		bossHitContainer:release();

		bossContainer:registerFunctionHandler(BossPage.onFunction)
		bossNode:addChild(bossContainer)
		bossContainer:release();
	end
	container:addChild(allianceContainer)
	allianceContainer:release();

	-- '无公会节点'
	joinListContainer = ScriptContentBase:create(option.ccbiFile)
	joinListContainer:registerFunctionHandler(GuildPage.onFunction)
	
	NodeHelper:initScrollView(joinListContainer, 'mContent', 10)

	-- -------------------- 适配 --------------------------------
	if joinListContainer.mScrollView then
		mainContainer:autoAdjustResizeScrollview(joinListContainer.mScrollView)
	end		
	
	local mScale9Sprite1 = joinListContainer:getVarScale9Sprite("mScale9Sprite1")
	if mScale9Sprite1 then
		mainContainer:autoAdjustResizeScale9Sprite( mScale9Sprite1 )
	end
	
	local mScale9Sprite2 = joinListContainer:getVarScale9Sprite("mScale9Sprite2")
	if mScale9Sprite2 then
		mainContainer:autoAdjustResizeScale9Sprite( mScale9Sprite2 )
	end
	
	local mScale9Sprite3 = joinListContainer:getVarScale9Sprite("mScale9Sprite3")
	if mScale9Sprite3 then
		mainContainer:autoAdjustResizeScale9Sprite( mScale9Sprite3 )
	end
	-- -------------------- 适配 --------------------------------

	container:addChild(joinListContainer)
	joinListContainer:release();

	-- 根据是否有公会来控制显隐
	if MyAllianceInfo then
		joinListContainer:setVisible(not MyAllianceInfo.hasAlliance)
		allianceContainer:setVisible(not (not MyAllianceInfo.hasAlliance))
	else
		joinListContainer:setVisible(true)
		allianceContainer:setVisible(false)
	end
end

function GuildPage.onEnter(container)
	if MyAllianceInfo.hasAlliance then
		BossPage.onAttributeOpen(bossContainer)
	end
	GuildPage.registerPackets(mainContainer)
	GuildPage.registerMessages(mainContainer)
	--AppstoreHandle:handleGuildMain( container )
	UserInfo.sync() --为了判断vip等级是否过3，自动战斗是否显示
	GuildPage.refreshPage()

	-- request basic info
	GuildPage.requestBasicInfo()
end

function GuildPage.onExecute(container)
	if BossPage 
		and allianceInfo.commonInfo 
		and (allianceInfo.commonInfo.bossState == BossPage.BossCanInspire) then
		-- 更新boss击退倒计时
		BossPage.updateCD(bossContainer)
	end

	-- 进入活动片刻后，预加载商店、成员列表、boss排行、公会排行等数据
	local dt = GamePrecedure:getInstance():getFrameTime() * 1000
	enterPageTime = enterPageTime + dt

	if enterPageTime > 100 then
		if not memberInfoInited  then
			if MyAllianceInfo and MyAllianceInfo.hasAlliance then
				GuildPage.getMemberList()
			end
			memberInfoInited = true
		end
	end

	if enterPageTime > 300 then
		if not shopInfoInited then
			if MyAllianceInfo and MyAllianceInfo.hasAlliance then
				GuildPage.getShopList()
			end
			shopInfoInited = true
		end
	end

	if enterPageTime > 500 then
		if not rankInfoInited then
			GuildPage.requestRankingList()
			rankInfoInited = true
		end
	end

	if enterPageTime > 800 then
		if not bossRankInited then
			if MyAllianceInfo and MyAllianceInfo.hasAlliance 
				and allianceInfo.commonInfo.bossState ~= BossPage.BossNotOpen then
				GuildPage.getHarmRank()
				bossRankInited = true
			end
		end
	end
end

function GuildPage.notifyMainPageNews()
	-- 通知主页面去掉红点
	local message = MsgMainFrameGetNewInfo:new()
	message.type = GameConfig.NewPointType.TYPE_ALLIANCE_NEW_CLOSE
	MessageManager:getInstance():sendMessageForScript(message)
end

function GuildPage.onExit(container)
    nowRefreshPageNum = 1
	GuildPage.notifyMainPageNews()
	GuildPage.removePackets(mainContainer)
	GuildPage.removeMessages(mainContainer)
	if joinListContainer then
		NodeHelper:deleteScrollView(joinListContainer)
	end
end
--------------------------------- boss page --------------------------------
function BossPage.onFunction(eventName, container)
	local funcName = option.bossHander[eventName]
	if funcName then
		BossPage[funcName](container, eventName)
	else
		CCLuaLog('unknown eventName: ' .. tostring(eventName))
	end
end

function BossPage.openBoss(container, eventName)

	if not allianceInfo.commonInfo then
		MessageBoxPage:Msg_Box('@GuildDataError')
		return
	end

	local bossState = allianceInfo.commonInfo.bossState
	if BossPage.BossNotOpen == bossState then
		-- check if you are leader 
		if not GuildPage.amILeader() then
			MessageBoxPage:Msg_Box('@GuildOnlyLeaderCanDo')
			return 
		end

		-- pop open boss page
		local message = ''
		local info = allianceInfo.commonInfo
		local needGold = 0;
		---------------------------------开启boss version1-------------------------------------------
		-- if info then
		-- 	if info.bossFunRemSize > 0 then
		-- 		message = common:getLanguageString('@OpenBossDesc', BossPage.FreeTimePerWeek, 0, info.bossFunRemSize)
		-- 	else
		-- 		message = common:getLanguageString('@OpenBossDesc', BossPage.FreeTimePerWeek, info.bossGold, info.bossFunRemSize)
		-- 		needGold = info.bossGold;
		-- 	end
		-- else
		-- 	message = common:getLanguageString('@OpenBossDesc', BossPage.FreeTimePerWeek, 0, 0)
		-- end	

		---------------------------------开启boss version2-------------------------------------------
		if info then
			message = common:getLanguageString('@OpenBossDescNowVersion', allianceInfo.commonInfo.openBossVitality, allianceInfo.commonInfo.curBossVitality)
		end
		-- page message
		GuildOpenBossPageVar.setMessage(message, needGold)

		-- listening this packet in pop page
		-- recover listening in onReceiveMessage
		mainContainer:removePacket(hp.ALLIANCE_CREATE_S)
		
		-- give up PageManager.showConfirm because the GUAJI-156 bug
		PageManager.pushPage('GuildOpenBossConfirmPage')

	elseif BossPage.BossCanJoin == bossState then
		BossPage.doJoinBoss(container)
	else
		MessageBoxPage:Msg_Box('@GuildDataError')
	end
end

function BossPage.doJoinBoss(container)
	local msg = alliance.HPAllianceBossFunOpenC()
	msg.operType = BossPage.BossOperJoin
	local pb = msg:SerializeToString()
	PacketManager:getInstance():sendPakcet(hp.ALLIANCE_BOSSFUNOPEN_C, pb, #pb, false)
	BossPage.bossJoinFlag = true
end

function BossPage.refreshPage(container)
	if not container then return end

	-- titles
	local lb2Str = {
		mBossIntrusionLevel 	= common:getLanguageString('@BossLevelName', 0, ''),
		mBossIntrusionExpNum = 0
	}

	local info = allianceInfo.commonInfo
	if info then
		local cfg = BossPage.getBossCfgByBossId(info.bossId)
		if cfg then
			lb2Str.mBossIntrusionLevel 	= common:getLanguageString('@BossLevelName', cfg.level, cfg.bossName)
			lb2Str.mBossIntrusionExpNum = cfg.bossExp
			lb2Str.mBossVitalityNum 	= info.curBossVitality .. '/' .. info.openBossVitality -- 开启boss需要消耗的元气值
		end
	end
	NodeHelper:setStringForLabel(container, lb2Str)

	-- content
	if not info then
		BossPage.showOpenBossView(container)
	elseif info.bossState == BossPage.BossNotOpen then
		-- not open
		BossPage.showOpenBossView(container)
	elseif info.bossState == BossPage.BossCanJoin then
		-- battle
		BossPage.showBossJoinView(container)
	elseif info.bossState == BossPage.BossCanInspire then
		-- can inspire
		BossPage.showBossBattleView(container)
	end
end
local attributeOpenState = false
-- 魔兽元气tip显示
local vitalityCfg = {
	type = 10000,
	itemId = 2001,
	count = 1,
}
function BossPage.onHideTipHandler()
	attributeOpenState = false
	bossContainer:getVarMenuItemImage("mAttributeBtn"):unselected()
	bossContainer:getVarMenuItemImage("mAttributeBtn"):setEnabled(true)
end
function BossPage.onTip( container )
	GameUtil:showTip(container:getVarNode("mAttributeBtn"),vitalityCfg,BossPage.onHideTipHandler)
end
function BossPage.onAttributeOpen( container )
	BossPage.onTip( container )
	bossContainer:getVarMenuItemImage("mAttributeBtn"):selected()

	attributeOpenState = true
	bossContainer:getVarMenuItemImage("mAttributeBtn"):setEnabled(false)
end

-- 显示‘开启boss’界面
function BossPage.showOpenBossView(container)
	-- 第一次进入页面显示tip，以后不显示
	if firstEnterBossPage == true then
		firstEnterBossPage = false
		BossPage.onAttributeOpen(bossContainer)
	end

	NodeHelper:setNodeVisible(container:getVarNode('mOpenBossNode'), true)
	NodeHelper:setNodeVisible(container:getVarNode('mBossOpenNoticeNode'), true)
	NodeHelper:setNodeVisible(container:getVarNode('mBossIntrusionBattle'), false)
	NodeHelper:setNodeVisible(container:getVarNode('mCDIntrusionNode'), false)
	local leftCount = allianceInfo.commonInfo and allianceInfo.commonInfo.bossFunRemSize or 0
	NodeHelper:setStringForLabel(container, { mOpenBossIntrusion = common:getLanguageString('@OpenBoss', leftCount)})
end

-- 显示‘加入战斗’界面
function BossPage.showBossJoinView(container)
	NodeHelper:setNodeVisible(container:getVarNode('mOpenBossNode'), true)
	NodeHelper:setNodeVisible(container:getVarNode('mBossOpenNoticeNode'), false)
	NodeHelper:setNodeVisible(container:getVarNode('mBossIntrusionBattle'), false)
	NodeHelper:setNodeVisible(container:getVarNode('mCDIntrusionNode'), false)
	NodeHelper:setStringForLabel(container, { mOpenBossIntrusion = common:getLanguageString('@GuildBossJoin')})
end

-- 显示‘战斗’界面
function BossPage.showBossBattleView(container)
	NodeHelper:setNodeVisible(container:getVarNode('mOpenBossNode'), false)
	NodeHelper:setNodeVisible(container:getVarNode('mBossOpenNoticeNode'), false)
	NodeHelper:setNodeVisible(container:getVarNode('mBossIntrusionBattle'), true)
	NodeHelper:setNodeVisible(container:getVarNode('mCDIntrusionNode'), true)
	local info = allianceInfo.commonInfo
	local lb2Str = { }
	local totalBlood = 0
	if info then
		local cfg = BossPage.getBossCfgByBossId(info.bossId)
		if cfg then
			totalBlood = cfg.bossBlood
		end
		lb2Str.mBossIntrusionHpNum = tostring(BossPage.bossBloodLeft) .. '/' .. tostring(totalBlood)
		lb2Str.mInspireIntrusionNum = common:getLanguageString('@GuildBossInspireRatio', info.bossPropAdd) 
	else
		lb2Str.mBossIntrusionHpNum = '0/0'
		lb2Str.mInspireIntrusionNum = common:getLanguageString('@GuildBossInspireRatio', info.bossPropAdd) 
	end
	-- inspire desc
	lb2Str.mEncouragePromptTex = common:getLanguageString('@GuildInspirePreview', BossPage.InspirePercent, BossPage.InspireCost)

	NodeHelper:setStringForLabel(container, lb2Str)

	-- progress bar
	local scale = 0.0
	if totalBlood ~= 0 then
		scale = BossPage.bossBloodLeft / totalBlood * 1.09
		if scale < 0 then scale = 0.0 end
	end

	local expBar = container:getVarScale9Sprite('mIntrusionExp')
	if expBar then
		expBar:setScaleX(scale)
	end
end

function BossPage.updateCD(container)
	if not container then return end

	local cdString = '00:00:00'
	if TimeCalculator:getInstance():hasKey(BossPage.CDTimeKey) then
		local timeleft = TimeCalculator:getInstance():getTimeLeft(BossPage.CDTimeKey)
		if timeleft > 0 then
			 cdString = GameMaths:formatSecondsToTime(timeleft)
		 else
			 -- boss 倒计时结束，判断打没打死
			 TimeCalculator:getInstance():removeTimeCalcultor(BossPage.CDTimeKey)
			 GuildPage.requestBasicInfo()
		end
	end
	NodeHelper:setStringForLabel(container, { mCD = cdString})
end

-- 鼓舞
function BossPage.onInspire(container, eventName)
	if UserInfo.isGoldEnough(BossPage.InspireCost) then
		BossPage.doInspire()
	end		
					
end

function BossPage.doInspire()
	local msg = alliance.HPAllianceBossFunOpenC()
	msg.operType = BossPage.BossOperInspire
	local pb = msg:SerializeToString()
	PacketManager:getInstance():sendPakcet(hp.ALLIANCE_BOSSFUNOPEN_C, pb, #pb, false)
end

function BossPage.onContributionRanking(container, eventName)
	PageManager.pushPage('GuildBossHarmRankPage')
end

function BossPage.getBossCfgByBossId(target)
	for k, v in pairs(BossPage.bossCfg) do
		if tonumber(v.bossId) == tonumber(target) then
			return v
		end
	end
	return nil
end
--------------------------------- end boss page --------------------------------

--------------------------ui function--------------------------------------

-- view when you have an alliance
function GuildPage.showAllianceView()
	joinListContainer:setVisible(false)
	allianceContainer:setVisible(true)

	-- alliance info
	GuildPage.showAllianceInfo()

	-- refresh boss
	BossPage.refreshPage(bossContainer)
end

-- view when you don't have an alliance
function GuildPage.showJoinListView()
	joinListContainer:setVisible(true)
	allianceContainer:setVisible(false)

	if joinListContainer.mScrollView 
		and joinListContainer.m_pScrollViewFacade 
		and allianceInfo.joinList then
		--显示刷新页数
		NodeHelper:setStringForLabel(joinListContainer, 
			{mPageNum =common:getLanguageString("@GuildRecommendListPage") .. allianceInfo.curPage .. '/' .. allianceInfo.maxPage})

		GuildPage.rebuildAllItem()
	end
end

function GuildPage.refreshPage()
	if MyAllianceInfo.hasAlliance then
		-- if i have a alliance
		GuildPage.showAllianceView()
	else
		-- i don't belong to any alliance
		GuildPage.showJoinListView()
	end
end

function GuildPage.showAllianceInfo(container)
	local lb2Str = {
		mPartakeLV					= common:getLanguageString("@GuildLevelName", 0, ''),
		mPartakeGuildID				= common:getLanguageString("@GuildID", 'NO'),
		mNumberPeople				= 'NO / NO',
		mPartakeGuildExp			= 'NO / NO',
		mPartakeGuildAnnouncements	= common:getLanguageString('@GuildAnnoucementDefault'),
		mPartakeNumberPeople 		= 0,
	}

	-- exp bar zoom scale
	local scale = 0.0
	local info = allianceInfo.commonInfo
	if info then
		lb2Str.mPartakeLV				= common:getLanguageString("@GuildLevelName", info.level, info.name)
		lb2Str.mPartakeGuildID 			= common:getLanguageString("@GuildID", info.id)
		lb2Str.mPartakeNumberPeople		= info.currentPop .. ' / ' .. info.maxPop
		lb2Str.mPartakeGuildExp			= info.currentExp .. ' / ' .. info.nextExp

		if info.nextExp ~= 0 then
			scale = info.currentExp / info.nextExp
		end

		if info.annoucement and common:trim(info.annoucement) ~= '' then
			-- 如果公告太长，取前20个字
			local length = GameMaths:calculateStringCharacters(info.annoucement)
			if length > 20 then
				lb2Str.mPartakeGuildAnnouncements = GameMaths:getStringSubCharacters(info.annoucement, 0, 20)
			else
				lb2Str.mPartakeGuildAnnouncements = info.annoucement
			end
		end
	end
	NodeHelper:setStringForLabel(allianceContainer, lb2Str)

	-- 自动战斗,如果是vip3以下隐藏
	NodeHelper:setNodeVisible(allianceContainer:getVarNode("mAutoFightNode"),UserInfo.playerInfo.vipLevel>=3)
	NodeHelper:setNodeVisible(allianceContainer:getVarSprite("mAutoFightSprite"), MyAllianceInfo.myInfo.autoFight==1)
	-- exp bar
	local expBar = allianceContainer:getVarScale9Sprite('mPartakeExp')
	if expBar then
		expBar:setScaleX(scale)
	end
end

----------------scrollview item of 可加入公会列表 -------------------------
local JoinListItem = {
	ccbiFile = 'GuildRecommendContent.ccbi',
}

function JoinListItem.onFunction(eventName, container)
	if eventName == "luaRefreshItemView" then
		JoinListItem.onRefreshItemView(container)
	elseif eventName == "onPartake" then
		JoinListItem.joinAlliance(container)
	end
end

function JoinListItem.onRefreshItemView(container)
	local index = container:getItemDate().mID
	local info = allianceInfo.joinList[index]
	if not info then return end
	local lb2Str = {
		mGuildLv 		= common:getLanguageString("@GuildLevel", info.level),
		mGuildName 		= info.name,
		mGuildNum		= info.currnetPop .. ' / ' .. info.maxPoj
	}
	NodeHelper:setStringForLabel(container, lb2Str)
end	

-- 加入公会
function JoinListItem.joinAlliance(container)
	local index = container:getItemDate().mID
	local info = allianceInfo.joinList[index]
	if not info then return end

	local msg = alliance.HPAllianceOperC()
	msg.operType = OperType.JoinAlliance
	msg.targetId = info.id
	local pb = msg:SerializeToString()
	allianceInfo.sendJoinRequestFlag = true
	PacketManager:getInstance():sendPakcet(hp.ALLIANCE_OPER_C, pb, #pb, false)
end

----------------scrollview-------------------------
function GuildPage.rebuildAllItem()
	GuildPage.clearAllItem(joinListContainer)
	GuildPage.buildItem()
end

function GuildPage.clearAllItem()
	NodeHelper:clearScrollView(joinListContainer)
end

function GuildPage.buildItem()
	NodeHelper:buildScrollView(joinListContainer, #allianceInfo.joinList, JoinListItem.ccbiFile, JoinListItem.onFunction);
end

----------------click event------------------------
function GuildPage.onHelp(container)
	PageManager.showHelp(GameConfig.HelpKey.HELP_ALLIANCE)
end

function GuildPage.refreshJoinList(container)
	nowRefreshPageNum = math.max((nowRefreshPageNum+1)%(allianceInfo.maxPage+1), 1) 
	GuildPage.getJoinList(container)
end

-- 搜索公会
function GuildPage.onSearchGuild(container)
	GuildSearchPageCallback = function (allianceId)
		-- allianceId is a number
		allianceId = allianceId or 0
		local msg = alliance.HPAllianceFindC()
		msg.id = allianceId
		local pb = msg:SerializeToString()
		-- no FIND_S, so set arg-4 to false, don't wait return
		mainContainer:sendPakcet(hp.ALLIANCE_FIND_C, pb, #pb, false)
	end
	PageManager.pushPage('GuildSearchPopPage')
end

-- 创建公会
function GuildPage.onCreateGuild(container)
	UserInfo.sync()
	if UserInfo.roleInfo.level < CreateAllianceOpenLevel then
		MessageBoxPage:Msg_Box(common:getLanguageString('@GuildCreateLevel', CreateAllianceOpenLevel))
		return
	end

	GuildCreatePageCallback = function (name)
		name = tostring(name)
		if common:trim(name) == '' then
			MessageBoxPage:Msg_Box('@GuildNameEmpty')
			return
		end
		GuildPage.createAlliance(container, name)
	end
	PageManager.pushPage('GuildCreatePage')
end

function GuildPage.onGuildBattle(container)
	MessageBoxPage:Msg_Box('@CommingSoon')
end

function GuildPage.onGuildRanking(container)
	PageManager.pushPage('GuildRankingPage')
end

-- 签到
function GuildPage.onSignIn(container)
	if MyAllianceInfo.myInfo.hasReported then
		MessageBoxPage:Msg_Box('@GuildSignInAlready')
		return
	end
	local msg = alliance.HPAllianceReportC()
	local pb = msg:SerializeToString()
	-- don't wait ALLIANCE_REPORT_S , no this packet.
	mainContainer:sendPakcet(hp.ALLIANCE_REPORT_C, pb, #pb, false)
	allianceInfo.signInFlag = true
end

-- 下面一排按钮里面的boss入侵，显示‘boss伤害排行榜'
function GuildPage.onIntrude(container)
	local bossState = allianceInfo.commonInfo.bossState or BossPage.BossCanInspire
	if BossPage.BossNotOpen == bossState then
		MessageBoxPage:Msg_Box('@GuildBossWaitToOpen')
	elseif BossPage.BossCanJoin == bossState then
		MessageBoxPage:Msg_Box('@GuildBossPleaseJoin')
	elseif BossPage.BossCanInspire == bossState then
		PageManager.pushPage('GuildBossHarmRankPage')
	end
end

-- 贡献兑换
function GuildPage.exchangeContribution(container)
	-- listening this packet in the pop page
	mainContainer:removePacket(hp.ALLIANCE_ENTER_S)
	PageManager.pushPage('GuildShopPage')
end

function GuildPage.showMembers(container)
	PageManager.pushPage('GuildMembersPage')
end

function GuildPage.onManage(container, eventName)
	-- listening this packet in the pop page
	mainContainer:removePacket(hp.ALLIANCE_CREATE_S)
	PageManager.pushPage('GuildManagePage')
end

-- 自动战斗
function GuildPage.onAutoFight(container)
	-- 如果是开启状态，点击取消勾选
	if MyAllianceInfo.myInfo.autoFight==1 then
		GuildPage.sendAutoFightPacket(container)
	else
		local autoFightCost = VaribleManager:getInstance():getSetting("autoAllianceFightCost")
		local title = common:getLanguageString('@AllianceAutoFightTitle')
	 	local message = common:getLanguageString('@AllianceAutoFightDesc', autoFightCost)
	 	PageManager.showConfirm(title, message,
	   		function (agree)
			    if agree and UserInfo.isGoldEnough(autoFightCost) then
			    	 GuildPage.sendAutoFightPacket(container)
			    end
		   	end
	  	)
	 end
end
function GuildPage.sendAutoFightPacket(container)
	common:sendEmptyPacket(HP_pb.ALLIANCE_AUTO_FIGHT_C,false);
end

function GuildPage.registerMessages(container)
	mainContainer:registerMessage(MSG_MAINFRAME_POPPAGE)
	mainContainer:registerMessage(MSG_MAINFRAME_REFRESH)
end

function GuildPage.removeMessages(container)
	mainContainer:removeMessage(MSG_MAINFRAME_POPPAGE)
	mainContainer:removeMessage(MSG_MAINFRAME_REFRESH)
end

--继承此类的活动如果同时开，消息监听不能同时存在,通过tag来区分
function GuildPage.onReceiveMessage(container)
	local message = container:getMessage()
	local typeId = message:getTypeId()
	if typeId == MSG_MAINFRAME_POPPAGE then
		local pageName = MsgMainFramePopPage:getTrueType(message).pageName
		if pageName == "GuildShopPage" then 
			mainContainer:registerPacket(hp.ALLIANCE_ENTER_S)
		elseif pageName == 'GuildManagePage' then
			mainContainer:registerPacket(hp.ALLIANCE_CREATE_S)
			GuildPage.refreshPage()
		elseif pageName == 'GuildOpenBossConfirmPage' then
			mainContainer:registerPacket(hp.ALLIANCE_CREATE_S)
		end
	elseif typeId == MSG_MAINFRAME_REFRESH then
		local pageName = MsgMainFrameRefreshPage:getTrueType(message).pageName;
		if pageName == 'GuildPage' then
			-- boss opened by leader
			-- request new alliance info and refresh page
			GuildPage.requestBasicInfo()
		elseif pageName == 'GuildPage_Refresh_Right_Now' then
			GuildPage.refreshPage()
		elseif pageName == "GuildPage_Refresh_BossPage" then
			BossPage.refreshPage(bossContainer)
		end
	end
end

---------------- util function -------------------------
function GuildPage.amILeader()
	if MyAllianceInfo.myInfo then
		return (MyAllianceInfo.myInfo.postion ~= PositionType.Normal)
	else
		return false
	end
end

function GuildPage.amIViceLeader()
	if MyAllianceInfo.myInfo then
		return (MyAllianceInfo.myInfo.postion == PositionType.ViceLeader)
	else
		return false
	end
end
-- =============== packet function =================
function GuildPage.registerPackets(container)
	mainContainer:registerPacket(hp.ALLIANCE_CREATE_S)
	mainContainer:registerPacket(hp.ALLIANCE_OPER_S)
	mainContainer:registerPacket(hp.ALLIANCE_JOIN_LIST_S)
	mainContainer:registerPacket(hp.ALLIANCE_ENTER_S)
	mainContainer:registerPacket(hp.ALLIANCE_BOSSHARM_S)
	mainContainer:registerPacket(hp.ALLIANCE_RANKING_S)
	mainContainer:registerPacket(hp.ALLIANCE_SHOP_S)
	mainContainer:registerPacket(hp.ALLIANCE_HARMSORT_S)
	mainContainer:registerPacket(hp.ALLIANCE_MEMBER_S)
end

function GuildPage.removePackets(container)
	mainContainer:removePacket(hp.ALLIANCE_CREATE_S)
	mainContainer:removePacket(hp.ALLIANCE_OPER_S)
	mainContainer:removePacket(hp.ALLIANCE_JOIN_LIST_S)
	mainContainer:removePacket(hp.ALLIANCE_ENTER_S)
	mainContainer:removePacket(hp.ALLIANCE_BOSSHARM_S)
	mainContainer:removePacket(hp.ALLIANCE_RANKING_S)
	mainContainer:removePacket(hp.ALLIANCE_SHOP_S)
	mainContainer:removePacket(hp.ALLIANCE_HARMSORT_S)
	mainContainer:removePacket(hp.ALLIANCE_MEMBER_S)
end

function GuildPage.requestBasicInfo()
	local msg = alliance.HPAllianceEnterC()
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_ENTER_C, pb, #pb, true)
end

function GuildPage.onReceiveAllianceEnterInfo(container, msg)
	-- check if need change interface(change ccbi)
	MyAllianceInfo = msg
	
	-- request joinlist
	if not MyAllianceInfo.hasAlliance then
		GuildPage.getJoinList(container)
	end

	-- receive this packet from sign in packet
	if allianceInfo.signInFlag then
		allianceInfo.signInFlag = false
		MessageBoxPage:Msg_Box('@GuildSignInSuccess')
	end
end

-- join list
function GuildPage.getJoinList(container)
	local msg = alliance.HPAllianceJoinListC()
	msg.reqPage = nowRefreshPageNum
	common:sendPacket(hp.ALLIANCE_JOIN_LIST_C, msg);
end

function GuildPage.onReceiveJoinList(container, msg)
	if msg.showTag then
		allianceInfo.joinList = msg.rankings
		allianceInfo.curPage = msg.curPage or 1
		allianceInfo.maxPage = msg.maxPage or 1
	else
		MessageBoxPage:Msg_Box('@GuildNoJoinList')
		allianceInfo.curPage = 1
		allianceInfo.maxPage = 1
		allianceInfo.joinList = nil
	end
end

function GuildPage.doBossOperation(container, operType)
	local msg = alliance.HPAllianceBossFunOpenC()
	msg.operType = operType
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_BOSSFUNOPEN_C, pb, #pb, true)
end

function GuildPage.onReceiveBossHarm(container, msg)
	BossPage.bossBloodLeft = tonumber(BossPage.bossBloodLeft - msg.value)
	local harm = common:getLanguageString('@GuildBossHarmValue', tostring(msg.value))
	--MessageBoxPage:Msg_Box(harm)
	if bossHitContainer then
		NodeHelper:setStringForLabel(bossHitContainer, { mNumLabel = harm })
		bossHitContainer:runAnimation('showNum')
	end

	if BossPage.bossBloodLeft <= 0 then
		-- if boss is over, reset page
		GuildPage.requestBasicInfo()
	end
end

-- create alliance
function GuildPage.createAlliance(container, name)
	local msg = alliance.HPAllianceCreateC()
	msg.name = name
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_CREATE_C, pb, #pb, true)
end

-- create result
function GuildPage.onReceiveAllianceInfo(container, msg)
	allianceInfo.commonInfo = msg
	
	-- adjust blood left
	if msg:HasField('bossHp') then
		BossPage.bossBloodLeft = msg.bossHp
	end

	-- 校正boss倒计时
	if msg:HasField('bossTime') then
		local bossTime = tonumber(msg.bossTime) and tonumber(msg.bossTime) or 600
		TimeCalculator:getInstance():createTimeCalcultor(BossPage.CDTimeKey, bossTime)
		if bossTime <= 0 and (msg.bossState ~= BossPage.BossNotOpen) then
			-- boss is over, reset page
			GuildPage.requestBasicInfo()
		end
	end

	if BossPage.bossJoinFlag then
		-- 收到了加入战斗的回包
		BossPage.bossJoinFlag = false
		local bossTime = tonumber(msg.bossTime) and tonumber(msg.bossTime) or 600
		TimeCalculator:getInstance():createTimeCalcultor(BossPage.CDTimeKey, bossTime)
	end
end

function GuildPage.requestRankingList()
	local msg = alliance.HPAllianceRankingC()
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_RANKING_C, pb, #pb, false)
end

function GuildPage.onReceiveRankingList(msg)
	rankInfoInited = true
	mainContainer:removePacket(hp.ALLIANCE_RANKING_S)
	if msg.showTag then
		GuildRankVar.setRankInfo(msg.rankings)
	else
		GuildRankVar.setRankInfo({})
	end
end

-- alliance members
function GuildPage.getMemberList()
	local msg = alliance.HPAllianceMemberC()
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_MEMBER_C, pb, #pb, false)
end

function GuildPage.onReceiveMembers(msg)
	memberInfoInited = true
	mainContainer:removePacket(hp.ALLIANCE_MEMBER_S)
	GuildMembersVar.setMemberList(msg.memberList)
end

function GuildPage.getShopList()
	local msg = alliance.HPAllianceShopC()
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_SHOP_C, pb, #pb, false)
end

function GuildPage.onReceiveShopList(msg)
	shopInfoInited = true
	mainContainer:removePacket(hp.ALLIANCE_SHOP_S)
	GuildShopVar.setShopInfo(msg.items)
end

function GuildPage.getHarmRank(container)
	local msg = alliance.HPAllianceHarmSortC()
	local pb = msg:SerializeToString()
	mainContainer:sendPakcet(hp.ALLIANCE_HARMSORT_C, pb, #pb, false)
end

function GuildPage.onReceiveHarmRank(msg)
	bossRankInited = true
	mainContainer:removePacket(hp.ALLIANCE_HARMSORT_S)
	if msg.showTag then
		GuildBossHarmVar.setHarmList(msg.harms)
	else
		GuildBossHarmVar.setHarmList({})
	end
end

function GuildPage.onReceivePacket(container)
	local opcode = mainContainer:getRecPacketOpcode()
	local msgBuff = mainContainer:getRecPacketBuffer()

	if opcode == hp.ALLIANCE_ENTER_S then
		-- alliance enter
		local msg = alliance.HPAllianceEnterS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveAllianceEnterInfo(container, msg)
		GuildPage.refreshPage()
		return
	end

	if opcode == hp.ALLIANCE_CREATE_S then
		-- create alliance
		local msg = alliance.HPAllianceInfoS()
		msg:ParseFromString(msgBuff)
		BossPage.onAttributeOpen(bossContainer)
		GuildPage.onReceiveAllianceInfo(container, msg)
		GuildPage.refreshPage()
		return
	end

	if opcode == hp.ALLIANCE_HARMSORT_S then
		local msg = alliance.HPAllianceHarmSortS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveHarmRank(msg)
		return
	end

	if opcode == hp.ALLIANCE_SHOP_S then
		-- alliance enter
		local msg = alliance.HPAllianceShopS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveShopList(msg)
		return
	end

	if opcode == hp.ALLIANCE_JOIN_LIST_S then
		-- alliance join list.
		local msg = alliance.HPAllianceJoinListS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveJoinList(container, msg)
		GuildPage.refreshPage()
		return
	end

	if opcode == hp.ALLIANCE_BOSSHARM_S then
		-- alliance join list.
		local msg = alliance.HPAllianceBossHarmS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveBossHarm(container, msg)
		BossPage.refreshPage(bossContainer)
		return
	end

	if opcode == hp.ALLIANCE_RANKING_S then
		local msg = alliance.HPAllianceRankingS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveRankingList(msg)
		return
	end

	if opcode == hp.ALLIANCE_MEMBER_S then
		local msg = alliance.HPAllianceMemberS()
		msg:ParseFromString(msgBuff)
		GuildPage.onReceiveMembers(msg)
		return
	end
end

function luaCreat_GuildPage(container)
	container:registerFunctionHandler(GuildPage.onFunction)
end	

