----------------------------------------------------------------------------------
--[[
	FILE:			GuildRankingPage.lua
	ENCODING:		UTF-8, no-bomb
	DESCRIPTION:	公会排行界面
	AUTHOR:			sunyj
	CREATED:		2014-8-4
--]]
----------------------------------------------------------------------------------
require 'common'

local alliance = require('Alliance_pb')
local hp = require('HP_pb')
local NodeHelper = require("NodeHelper")

local thisPageName = 'GuildRankingPage'
local GuildRankingBase = {}
local rankingList = {}

GuildRankVar = {
	setRankInfo = function (rankInfo)
		rankingList = rankInfo
		table.sort( rankingList, function( e1,e2 )
			if e1.level ~= e2.level then
				return e1.level+0 > e2.level+0
			else
				return e1.id <= e2.id
			end
		end )
	end
}
local option = {
	ccbiFile = "GuildRankingPopUp.ccbi",
	handlerMap = {
		onCancel 		= 'onClose',
		onClose 		= 'onClose',
	}
}

function GuildRankingBase:onLoad(container)
	container:loadCcbiFile(option.ccbiFile)
end

function GuildRankingBase:onEnter(container)
	self:registerPackets(container)
	NodeHelper:initScrollView(container, 'mContent', 10)
	self:refreshPage(container)
	self:requestRankingList(container)
end

function GuildRankingBase:onExit(container)
	self:removePackets(container)
	NodeHelper:deleteScrollView(container)
end

function GuildRankingBase:refreshPage(container)
	table.sort( rankingList, function( e1,e2 )
		if e1.level ~= e2.level then
			return e1.level+0 > e2.level+0
		else
			return e1.id <= e2.id
		end
	end )
	self:rebuildAllItem(container)
end

function GuildRankingBase:onClose(container)
	PageManager.popPage(thisPageName)
end

----------------scrollview item-------------------------
local RankListItem = {
	ccbiFile = 'GuildRankingContent.ccbi'
}

function RankListItem.onFunction(eventName, container)
	if eventName == "luaRefreshItemView" then
		RankListItem.onRefreshItemView(container)
	end
end

function RankListItem.onRefreshItemView(container)
	local index = container:getItemDate().mID
	local info = rankingList[index]
	if not info then return end
	local lb2Str = {
		mRanking 		= index,
		mID 			= info.id,
		mGuildLv 		= common:getLanguageString('@LevelDesc', info.level),
		mGuildName 		= info.name,
		mLeadersName 	= info.handName,
	}
	NodeHelper:setStringForLabel(container, lb2Str)
end	

----------------scrollview-------------------------
function GuildRankingBase:rebuildAllItem(container)
	self:clearAllItem(container)
	self:buildItem(container)
end

function GuildRankingBase:clearAllItem(container)
	NodeHelper:clearScrollView(container)
end

function GuildRankingBase:buildItem(container)
	NodeHelper:buildScrollView(container, #rankingList, RankListItem.ccbiFile, RankListItem.onFunction);
end

 ------------------ packet function -----------------------------------
function GuildRankingBase:registerPackets(container)
	container:registerPacket(hp.ALLIANCE_RANKING_S)
end

function GuildRankingBase:removePackets(container)
	container:removePacket(hp.ALLIANCE_RANKING_S)
end

function GuildRankingBase:requestRankingList(container)
	local msg = alliance.HPAllianceRankingC()
	local pb = msg:SerializeToString()
	container:sendPakcet(hp.ALLIANCE_RANKING_C, pb, #pb, true)
end

function GuildRankingBase:onReceiveRankingList(container, msg)
	if msg.showTag then
		rankingList = msg.rankings
	else
		MessageBoxPage:Msg_Box('@GuildNoRankList')
		rankingList = {}
	end
end

function GuildRankingBase:onReceivePacket(container)
	local opcode = container:getRecPacketOpcode()
	local msgBuff = container:getRecPacketBuffer()
	
	if opcode == hp.ALLIANCE_RANKING_S then
		-- alliance enter
		local msg = alliance.HPAllianceRankingS()
		msg:ParseFromString(msgBuff)
		self:onReceiveRankingList(container, msg)
		self:refreshPage(container)
		return
	end
end

local CommonPage = require('CommonPage')
local GuildRankingPage = CommonPage.newSub(GuildRankingBase, thisPageName, option)
