local Activity_pb = require("Activity_pb");
local HP_pb = require("HP_pb");
local Const_pb = require("Const_pb");
local UserInfo = require("UserInfo");
require("ActivityConfig");

ActivityInfo = {
	closeId = 99999,		--
	ids = {},				--
	rewardCount = {},		--
	newCount = 0,			--
	totalReward = 0,		--
	activities = {},		--
	cache = {}
};
local TodoTrue = false;
--------------------------------------------------------------------------------
------------local variable for system api--------------------------------------
local tostring = tostring;
local tonumber = tonumber;
local string = string;
local pairs = pairs;
--------------------------------------------------------------------------------
--
function onActivityOpenSync(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPOpenActivitySyncS();
	msg:ParseFromString(handler:getRecPacketBuffer());
		
		UserInfo.syncPlayerInfo();
		ActivityInfo.rewardCount = {};
		ActivityInfo.totalReward = 0;
		ActivityInfo.newCount = 0;
		
		local hasNew = false;
		local ids = {};
		local activities = {};
		for _, activity in ipairs(msg.activity) do
			local id = activity.activityId;
			if ActivityConfig[id] ~= nil then
				local actInfo = ActivityInfo.activities[id] or {};
				local oldVersion = actInfo.version;
				local newVersion = math.max(activity.stageId, 1);
				if oldVersion == nil then
					local key = string.format("Activity_%d_%d_%d", UserInfo.serverId, UserInfo.playerInfo.playerId, id);
					oldVersion = CCUserDefault:sharedUserDefault():getIntegerForKey(key) or 0;
				end
			
				--
				local isNew = oldVersion < newVersion;
				if isNew and id~=19 then
					hasNew = true;
					ActivityInfo.newCount = ActivityInfo.newCount + 1;
				end
				activities[id] = {
					version 	= oldVersion,
					newVersion 	= newVersion,
					isNew		= isNew
				};
				table.insert(ids, id);
				
				--
				if id == Const_pb.CONTINUE_RECHARGE then
					local msg = Activity_pb.HPContinueRechargeInfo();
					common:sendPacket(HP_pb.CONTINUE_RECHARGE_INFO_C, msg);
				elseif id == Const_pb.ACCUMULATIVE_RECHARGE then
					local msg = Activity_pb.HPAccRechargeInfo();
					common:sendPacket(HP_pb.ACC_RECHARGE_INFO_C, msg);
				elseif id == Const_pb.ACCUMULATIVE_CONSUME then
					local msg = Activity_pb.HPAccConsumeInfo();
					common:sendPacket(HP_pb.ACC_CONSUME_INFO_C, msg);
				elseif id == Const_pb.WORDS_EXCHANGE then
					common:sendEmptyPacket(HP_pb.WORDS_EXCHANGE_INFO_C);
				elseif id == Const_pb.SINGLE_RECHARGE then
					common:sendEmptyPacket(HP_pb.SINGLE_RECHARGE_INFO_C);
				elseif id == Const_pb.RECHARGE_REBATE then	
		            common:sendEmptyPacket(HP_pb.RECHARGE_REBATE_INFO_C,false)
                elseif id == Const_pb.RECHARGE_REBATE2 then
                	local msg = Activity_pb.HPRebateInfo()
	                msg.activityId = Const_pb.RECHARGE_REBATE2
	                common:sendPacket(HP_pb.RECHARGE_REBATE_INFO_C, msg);
				elseif id == Const_pb.VIP_WELFARE then	
		            common:sendEmptyPacket(HP_pb.VIP_WELFARE_INFO_C,false)
				end
			end
		end
		
		-- 
		if common:table_hasValue(ids, 19) then
			GlobalData.diamondRatio = 0.5
			ids = common:table_removeFromArray(ids, 19)
			table.remove(activities, 19)
		else
			GlobalData.diamondRatio = nil
		end
		
		--
		for id, _ in pairs(ActivityInfo.activities) do
			if id ~= ActivityInfo.closeId and activities[id] == nil then
				local pageName = ActivityConfig[id]["page"] or "";
				if pageName == MainFrame:getInstance():getCurShowPageName() then
					MessageBoxPage:Msg_Box_Lan("@CurrentActivityIsClosed");
					PageManager.changePage("ActivityPage");
				end
			end
		end
		
		ActivityInfo.activities = activities;
		
		--sort ids by order
		table.sort(ids, function(id_1, id_2)
			local order_1 = ActivityConfig[id_1]["order"] or 99999;
			local order_2 = ActivityConfig[id_2]["order"] or 99999;
			
			if order_1 ~= order_2 then
				return order_1 < order_2;
			end
			return id_1 < id_2;
		end);

		--
		for i = #ids + 1, GameConfig.Count.MinActivity do
			table.insert(ids, ActivityInfo.closeId);
		end
		ActivityInfo.ids = ids;
		
		--MainFrame banner
		if hasNew then
			ActivityInfo:showNotice(true);
		end
		
		PageManager.refreshPage("ActivityPage");
	end
end

ActivityListen = PacketScriptHandler:new(HP_pb.OPEN_ACTIVITY_SYNC_S, onActivityOpenSync);

--------------------------------------------------------------------------------
-- 
function onDailyOnceRecharge(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPSingleRechargeInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
		
		local activityId = Const_pb.SINGLE_RECHARGE
		if #msg.canGetAwardCfgId > 0 then
			ActivityInfo.rewardCount[activityId] = 1
		else
			ActivityInfo.rewardCount[activityId] = 0
		end
		ActivityInfo.totalReward = ActivityInfo.totalReward + #msg.canGetAwardCfgId
	end
end
ListenDailyOnceRecharge = PacketScriptHandler:new(HP_pb.SINGLE_RECHARGE_INFO_S, onDailyOnceRecharge);

-- 
function onRechargeRebate(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPRebateInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
		
		local activityId = Const_pb.RECHARGE_REBATE
		table.foreach(ActivityInfo.ids, function(i, v)
			if v == Const_pb.RECHARGE_REBATE or v == Const_pb.RECHARGE_REBATE2 then
				activityId = v
			end
		end)
		if msg:HasField("receiveAward") then
			if msg.receiveAward == 0 then
				ActivityInfo.rewardCount[activityId] = 1
			else
				ActivityInfo.rewardCount[activityId] = 0
			end
		else
			ActivityInfo.rewardCount[activityId] = 0
		end
		ActivityInfo.totalReward = ActivityInfo.totalReward + ActivityInfo.rewardCount[activityId]
	end
end
ListenRechargeRebate = PacketScriptHandler:new(HP_pb.RECHARGE_REBATE_INFO_S, onRechargeRebate);

--
function onActivityContinueRecharge(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPContinueRechargeInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
		
		local activityId = Const_pb.CONTINUE_RECHARGE;
		local count = 0;
		local rechargeDays = msg.continueRechargedays;
		if rechargeDays > 0 then
			local itemCfg = ActivityConfig[activityId]["items"] or {};
			local rewardIds = msg.gotAwardCfgId;
			if #rewardIds < table.maxn(itemCfg) then
				for id, cfg in pairs(itemCfg) do
					if not common:table_hasValue(rewardIds, id) and cfg["d"] <= rechargeDays then
						count = count + 1;
					end
				end
			end
		end

		if count > 0 then
			ActivityInfo:showNotice(true);
		end
		local add = count - (ActivityInfo.rewardCount[activityId] or 0);
		ActivityInfo.totalReward = ActivityInfo.totalReward + add;
		ActivityInfo.rewardCount[activityId] = count;
	end
end
ListenContinueRecharge = PacketScriptHandler:new(HP_pb.CONTINUE_RECHARGE_INFO_S, onActivityContinueRecharge);

--
function onActivityAccumulativeRecharge(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPAccRechargeInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
		
		local activityId = Const_pb.ACCUMULATIVE_RECHARGE;
		local count = 0;
		local rechargeDays = msg.accRechargeGold;
		if rechargeDays > 0 then
			local itemCfg = ActivityConfig[activityId]["items"] or {};
			local rewardIds = msg.gotAwardCfgId;
			if #rewardIds < table.maxn(itemCfg) then
				for id, cfg in pairs(itemCfg) do
					if not common:table_hasValue(rewardIds, id) and cfg["c"] <= rechargeDays then
						count = count + 1;
					end
				end
			end
		end

		if count > 0 then
			ActivityInfo:showNotice(true);
		end
		local add = count - (ActivityInfo.rewardCount[activityId] or 0);
		ActivityInfo.totalReward = ActivityInfo.totalReward + add;
		ActivityInfo.rewardCount[activityId] = count;
	end
end
ListenAccumulativeRecharge = PacketScriptHandler:new(HP_pb.ACC_RECHARGE_INFO_S, onActivityAccumulativeRecharge);

--
function onActivityAccumulativeConsume(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPAccConsumeInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
		
		local activityId = Const_pb.ACCUMULATIVE_CONSUME;
		local count = 0;
		local consumeAmount = msg.accConsumeGold;
		if consumeAmount > 0 then
			local itemCfg = ActivityConfig[activityId]["items"] or {};
			local rewardIds = msg.gotAwardCfgId;
			if #rewardIds < table.maxn(itemCfg) then
				for id, cfg in pairs(itemCfg) do
					if not common:table_hasValue(rewardIds, id) and cfg["n"] <= consumeAmount then
						count = count + 1;
					end
				end
			end
		end

		if count > 0 then
			ActivityInfo:showNotice(true);
		end
		local add = count - (ActivityInfo.rewardCount[activityId] or 0);
		ActivityInfo.totalReward = ActivityInfo.totalReward + add;
		ActivityInfo.rewardCount[activityId] = count;
	end
end

ListenAccumulativeConsume = PacketScriptHandler:new(HP_pb.ACC_CONSUME_INFO_S, onActivityAccumulativeConsume);

--
function onRecieveWordExchange(eventName, handler)
	if eventName == "luaReceivePacket" then
		local msg = Activity_pb.HPWordsExchangeInfo();
		msg:ParseFromString(handler:getRecPacketBuffer());
		
		local activityId = Const_pb.WORDS_EXCHANGE;
		
		local leftTimes = {};
		for i, times in ipairs(msg.leftExchangeTimes) do
			leftTimes[i] = times;
		end
		ActivityInfo.cache[activityId] = {times = leftTimes};
		
		ActivityInfo:checkWordExchange();
	end
end

ListenWordExchange = PacketScriptHandler:new(HP_pb.WORDS_EXCHANGE_INFO_S,onRecieveWordExchange );

--
function ActivityInfo:checkAfterItemSync(itemId, count)
	local ItemManager = require("ItemManager");
	local itemType = ItemManager:getTypeById(itemId);
	if itemType == Const_pb.WORDS_EXCHANGE_NORMAL then
		self:checkWordExchange(count);
	end
end

--
function ActivityInfo:checkWordExchange(itemCount)
	local activityId = Const_pb.WORDS_EXCHANGE;
	if not ActivityInfo.activities[activityId] then return; end
	
	local count = self:canExchangeWord() and 1 or 0;
	if itemCount and itemCount > 0 then	--hack code begin
		count = 1;
	end						--hack code end
	if count > 0 then
		self:showNotice(true);
	end
	local add = count - (ActivityInfo.rewardCount[activityId] or 0);
	ActivityInfo.totalReward = ActivityInfo.totalReward + add;
	ActivityInfo.rewardCount[activityId] = count;
	self:checkReward();
end

function ActivityInfo:canExchangeWord()
	local activityId = Const_pb.WORDS_EXCHANGE;
	if not ActivityInfo.cache[activityId] then return false; end
	
	local actCfg = ActivityConfig[activityId]["items"];
	local leftTimes = ActivityInfo.cache[activityId]["times"];
	for i = #actCfg, 1, -1 do
		local itemCfg = actCfg[i];		
		if (itemCfg.t < 0 or leftTimes[i] > 0) 
			and ResManagerForLua:canConsume(itemCfg.c)
		then
			return true;
		end
	end
	return false;
end
--------------------------------------------------------------------------------
--
function onRecieveWeeklyCardInfo(eventName, handler)
	if eventName == "luaReceivePacket" then
	    local msg = Activity_pb.HPWeekCardInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
      
        local activityId = Const_pb.WEEK_CARD;
        if msg.activeWeekCardId > 0 and msg.isTodayReward == 0 then
            ActivityInfo.rewardCount[activityId] = 1
            if msg.isNeedYestReward~=0 then
                ActivityInfo.rewardCount[activityId] = 2
            end
        else
            ActivityInfo.rewardCount[activityId] = 0
        end
	end
end
ListenWeekCard = PacketScriptHandler:new(HP_pb.WEEK_CARD_INFO_S,onRecieveWeeklyCardInfo );
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
--
CumulativeLoginConfig = require("CumulativeLoginConfig")

function onRecieveCumulativeLoginInfo(eventName, handler)
	if eventName == "luaReceivePacket" then
	    local msg = Activity_pb.HPAccLoginInfoRet();
		msg:ParseFromString(handler:getRecPacketBuffer());
      
        local activityId = Const_pb.ACCUMULATIVE_LOGIN;
        local gotRewardCount = #msg.gotAwardCfgId or 0
        local remainReward = msg.loginDays
        if msg.loginDays > (#CumulativeLoginConfig.DailyRewardItem) then
            remainReward = #CumulativeLoginConfig.DailyRewardItem
        end
        
        if gotRewardCount < remainReward  then
            ActivityInfo.rewardCount[activityId] = remainReward - gotRewardCount
        else
            ActivityInfo.rewardCount[activityId] = 0
        end
	end
end
ListenAccumulativeLogin = PacketScriptHandler:new(HP_pb.ACC_LOGIN_INFO_S,onRecieveCumulativeLoginInfo );


function onRecieveCumulativeLoginAwardInfo(eventName, handler)
	if eventName == "luaReceivePacket" then
        local activityId = Const_pb.ACCUMULATIVE_LOGIN;
        ActivityInfo.rewardCount[activityId] = ActivityInfo.rewardCount[activityId] - 1
        if ActivityInfo.rewardCount[activityId] < 0 then
            ActivityInfo.rewardCount[activityId] = 0
        end
	end
end
ListenAccumulativeLoginAward = PacketScriptHandler:new(HP_pb.ACC_LOGIN_AWARDS_S,onRecieveCumulativeLoginAwardInfo );
--------------------------------------------------------------------------------
function onRecieveVipWelfNotice( eventName , handler )
	if eventName == "luaReceivePacket" then
		UserInfo.syncPlayerInfo()
		if UserInfo.playerInfo.vipLevel <= 0 then
			return
		end
		local activityId = Const_pb.VIP_WELFARE
		local msg = Activity_pb.HPVipWelfareInfoRet()
		msg:ParseFromString(handler:getRecPacketBuffer())
		if msg.awardStatus == 0 then
			ActivityInfo.activities[activityId]["isNew"] = true
			ActivityInfo:showNotice(true)
		else
			ActivityInfo.activities[activityId]["isNew"] = false
			ActivityInfo:showNotice(false)
		end
	end

end
ListenerVipWelf = PacketScriptHandler:new(HP_pb.VIP_WELFARE_INFO_S,onRecieveVipWelfNotice );

--
function ActivityInfo:decreaseReward(activityId)
	local count = ActivityInfo.rewardCount[activityId] or 0;
	ActivityInfo.rewardCount[activityId] = math.max(count - 1, 0);
	ActivityInfo.totalReward = math.max(ActivityInfo.totalReward - 1 ,0);
	self:checkReward();
end

--MainFrame banner
function ActivityInfo:checkReward()
	if ActivityInfo.newCount <= 0 and ActivityInfo.totalReward <= 0 then
		self:showNotice(false);
	else
		self:showNotice(true);
	end
end

function ActivityInfo:showNotice(visible)
	if visible ~= NoticePointState.ACTIVITY_POINT then
		NoticePointState.ACTIVITY_POINT = visible;
		NoticePointState.isChange= true;
	end
end

function ActivityInfo:validateAndRegister()
	CCLuaLog("ActivityInfo:validateAndRegister()")	
	ListenAccumulativeLogin:registerFunctionHandler(onRecieveCumulativeLoginInfo)
	ListenAccumulativeLoginAward:registerFunctionHandler(onRecieveCumulativeLoginAwardInfo)
	ListenWeekCard:registerFunctionHandler(onRecieveWeeklyCardInfo)
	ListenWordExchange:registerFunctionHandler(onRecieveWordExchange)
	ListenAccumulativeConsume:registerFunctionHandler(onActivityAccumulativeConsume)
	ListenContinueRecharge:registerFunctionHandler(onActivityContinueRecharge) 
	ListenAccumulativeRecharge:registerFunctionHandler(onActivityAccumulativeRecharge) 
	ActivityListen:registerFunctionHandler(onActivityOpenSync) 	
	ListenerVipWelf:registerFunctionHandler(onRecieveVipWelfNotice) 	
end

--
function ActivityInfo:saveVersion(id)
	local activity = ActivityInfo.activities[id] or {};
	if not activity.isNew then return; end
	
	local key = string.format("Activity_%d_%d_%d", UserInfo.serverId, UserInfo.playerInfo.playerId, id);
	CCUserDefault:sharedUserDefault():setIntegerForKey(key, activity.newVersion);
	CCUserDefault:sharedUserDefault():flush();
	ActivityInfo.activities[id]["version"] = activity.newVersion;
	ActivityInfo.activities[id]["isNew"] = false;
	ActivityInfo.newCount = math.max(ActivityInfo.newCount - 1, 0);
	
	PageManager.refreshPage('ActivityPage');
end

--------------------------------------------------------------------------------