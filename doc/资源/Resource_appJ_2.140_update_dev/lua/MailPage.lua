----------------------------------------------------------------------------------
--[[
	FILE:		MailPage.lua
	ENCODING:		UTF-8, no-bomb
	DESCRIPTION:	邮件主页面
	AUTHOR:			Zhao Zhen
	CREATED:		2014-08-06
--]]
----------------------------------------------------------------------------------
--require "ExploreEnergyCore_pb"

local thisPageName = "MailPage"
local Mail_pb = require("Mail_pb");
local HP = require("HP_pb");
local NodeHelper = require("NodeHelper");
local ResManager = require("ResManagerForLua");
local common = require("common");
RegisterLuaPage("MailBattlePage");

------------local variable for system global api--------------------------------------
local tostring = tostring;
local string = string;
local pairs = pairs;
--------------------------------------------------------------------------------

local MailPageBase = {};

local opcodes = 
{
	OPCODE_MAIL_INFO_C = HP.MAIL_INFO_C,
	OPCODE_MAIL_GET_C = HP.MAIL_GET_C,
	OPCODE_MAIL_GET_S = HP.MAIL_GET_S
}

local option = {
	ccbiFile = "MailPopUp.ccbi",
	handlerMap = {
		onClose = "onClose"
	},
	opcode = opcodes
};

local MailItem = {
	ccbiFile 	= "MailContent.ccbi"
}

MailInfo = 
{
	lastMail = {},
	mails ={},
	mailAreanAll = {}
}

local REWRAD_LINE_COUNT = 16;

local mainContainer = nil;

local MailContetnCfg = ConfigManager.getMailContentCfg();

local ZHANBAO_MAILID = {
	MAIN_TYPE = 5,
	SUB_TYPE = 6
}

--注销登陆，刷新信息-------------------------
function RESETINFO_MAILS()
	MailInfo = {}
	MailInfo.mails = {}
	MailInfo.lastMail = {}
	MailInfo.mailAreanAll = {}
	requestId = 0
	mailInvalidateList = {}
end


------------------创建content回掉----------------------
function MailItem.onFunction(eventName, container)
	if eventName == "luaRefreshItemView" then
		MailItem.onRefreshItemView(container);
	elseif eventName == "onReward" or eventName == "onCancel" then
		MailItem.onCancelorGetItem(container);
	elseif eventName == "onView" then
		MailItem.onView( container );
	end
end

--------------------查看战报轮具体信息，进入MailBattlePage--------------------------
function MailItem.onView( container )
	local index = container:getItemDate().mID;
	local mail = MailInfo.mails[index];
	if mail.type == Mail_pb.ARENA then
		PageManager.changePage("ArenaPage")
		return
	end
	if mail.mailId == ZHANBAO_MAILID.MAIN_TYPE or mail.mailId == ZHANBAO_MAILID.SUB_TYPE then
		local maxParams = table.maxn(mail.params);
		requestId = mail.params[maxParams];
		PageManager.pushPage("MailBattlePage")
	else
		MessageBoxPage:Msg_Box("@NotBattleMail")
	end
end

function MailItem.onRefreshItemView(container)

	local index = container:getItemDate().mID
	local mail = MailInfo.mails[index];

	MailItem.changeContentType(container, mail);

end

----------根据邮件类型，刷新content-------------
function MailItem.changeContentType( container, mail )
	local rewardNodeVis = false;
	local normalNodeVis = false;
	local battleNodeVis = false;
	--local titleStr = {};
	local sTitle = ""; 
	local labelNode = "";
	local str = "";

	if mail.mailId ~= 0 then
		local mailObj = MailContetnCfg[mail.mailId];
		if mailObj ~= nil then

			sTitle = mailObj.content;

			local pSize = table.maxn(mail.params);
			for i=1,pSize,1 do
				local vStr = "#v"..i.."#";
				sTitle = GameMaths:replaceStringWithCharacterAll(sTitle,vStr,mail.params[i]);
			end
		end
	else
		sTitle = common:stringAutoReturn(mail.title, REWRAD_LINE_COUNT);
	end

	if mail.type == Mail_pb.Normal then
		normalNodeVis = true;
		rewardNodeVis = false;
		battleNodeVis = false;

		str = FreeTypeConfig[56].content;
		str = GameMaths:replaceStringWithCharacterAll(str,"#v1#",sTitle);
		--titleStr = {mMaillSystem = str};
		labelNode = container:getVarLabelBMFont("mMaillSystem");
	elseif mail.type == Mail_pb.Battle then
		normalNodeVis = false;
		rewardNodeVis = false;
		battleNodeVis = true;

		str = FreeTypeConfig[56].content;
		str = GameMaths:replaceStringWithCharacterAll(str,"#v1#",sTitle);
		--titleStr = {mMaillReports = str};
		labelNode = container:getVarLabelBMFont("mMaillReports");
	elseif mail.type == Mail_pb.Reward then
		normalNodeVis = false;
		rewardNodeVis = true;
		battleNodeVis = false;
		local rewardStr = MailItem.getRewardStr( mail.item );

		str = FreeTypeConfig[55].content;
		str = GameMaths:replaceStringWithCharacterAll(str,"#v1#",sTitle);
		str = GameMaths:replaceStringWithCharacterAll(str,"#v2#",rewardStr);
		--titleStr = {mMaillPrizeExplain = str};
		labelNode = container:getVarLabelBMFont("mMaillPrizeExplain");
	elseif mail.type == Mail_pb.ARENA then
	    normalNodeVis = false
		rewardNodeVis = false
		battleNodeVis = true

		str = FreeTypeConfig[56].content
		str = GameMaths:replaceStringWithCharacterAll(str,"#v1#",sTitle)
		--titleStr = {mMaillReports = str}
		labelNode = container:getVarLabelBMFont("mMaillReports")
	end

	--NodeHelper:setStringForLabel(container, titleStr);
	local tag = GameConfig.Tag.HtmlLable
	local size = CCSizeMake(GameConfig.LineWidth.MailContent, 200);
	if labelNode ~= nil then
		NodeHelper:addHtmlLable(labelNode, str ,tag, size)
	end

	local visibleMap = 
	{
		mMailPrizeNode = rewardNodeVis,
		mMailReportsNode = battleNodeVis,
		mMailSystemNode = normalNodeVis
	}

	NodeHelper:setNodesVisible(container, visibleMap);
end

function MailItem.getRewardStr( items )
	local maxSize = table.maxn(items);

	local str = "";
	for i=1,maxSize,1 do
		local item = items[i];
		local resInfo = ResManager:getResInfoByTypeAndId(item.itemType, item.itemId, item.itemCount);
		str = str .. resInfo.name .."*"..item.itemCount.." ";
	end

	str = common:stringAutoReturn(str, REWRAD_LINE_COUNT);
	return str;
end

----------领取邮件按钮点击事件-------------
function MailItem.onCancelorGetItem( container )
	local index = container:getItemDate().mID
	local mail = MailInfo.mails[index];
	
	if mail then
		MailPageBase:sendMsgForMailGetInfo(mainContainer,mail.id);
	end
end

----------------------------------------------------------------------------------
	
-----------------------------------------------

--------------------开启有新邮件提示标记---------------------
function MailPageBase:sendGetNewInfoMessage( container )
	local msg = MsgMainFrameGetNewInfo:new()
	msg.type = Const_pb.NEW_MAIL;
	MessageManager:getInstance():sendMessageForScript(msg)
end

--------------------关闭有新邮件提示标记---------------------
function MailPageBase:sendClosesNewInfoMessage( container )
	local msg = MsgMainFrameGetNewInfo:new()
	msg.type = GameConfig.NewPointType.TYPE_MAIL_CLOSE;
	MessageManager:getInstance():sendMessageForScript(msg)
end

function MailPageBase:onInit( container )
end

function MailPageBase:onLoad( container )
	container:loadCcbiFile(option.ccbiFile);
	NodeHelper:initScrollView(container, "mContent", 4);
end

function MailPageBase:onEnter(container)
	self:registerPacket(container)
	mainContainer = container;
	self:rebuildAllItem(container)
	self:refreshPage(container);
	--self:sendGetNewInfoMessage( container )
	container:registerMessage(MSG_MAINFRAME_REFRESH)
	self:sendMsgForMailInfo(container);
	--self:rebuildAllItem(container);

end

function MailPageBase:onExecute(container)
end

function MailPageBase:onExit(container)
	local boo = true
	for k,v in ipairs( MailInfo.mails ) do
		if v.type == Mail_pb.Reward then
			boo = false
		end
	end
	if boo then
		self:sendClosesNewInfoMessage(container)
	end
	
	self:removePacket(container)
	container:removeMessage(MSG_MAINFRAME_REFRESH)	
	NodeHelper:deleteScrollView(container);

end
----------------------------------------------------------------

function MailPageBase:refreshPage(container)
	local rewardCount = self:getCurrentRewardCount();
	local mailNoticeStr = common:getLanguageString("@MailNotice", rewardCount);

	if rewardCount <= 0 then
		mailNoticeStr = common:getLanguageString("@MailNoticeNo");
	end

	NodeHelper:setStringForLabel(container, {mMailPromptTex = mailNoticeStr});
end
----------------scrollview-------------------------
function MailPageBase:rebuildAllItem(container)
	self:clearAllItem(container);
	self:buildItem(container);
end

function MailPageBase:clearAllItem(container)
	NodeHelper:clearScrollView(container);
end

function MailPageBase:buildItem(container)
	if (MailInfo.mails == nil) then
		return;
	end

	local maxSize = table.maxn(MailInfo.mails);

	NodeHelper:buildScrollView(container, maxSize, MailItem.ccbiFile, MailItem.onFunction);
end

----------------click event------------------------
function MailPageBase:onClose(container)
	PageManager.popPage(thisPageName);
end

function MailPageBase:onReceivePacket(container)
	local opcode = container:getRecPacketOpcode()
	local msgBuff = container:getRecPacketBuffer()

--[[
	if opcode == opcodes.OPCODE_MAIL_INFO_S then
		local msg = Mail_pb.OPMailInfoRet()
		msg:ParseFromString(msgBuff)
		self:onReceiveMailInfo(container, msg)
		return
	end
--]]
	if opcode == opcodes.OPCODE_MAIL_GET_S then
		local msg = Mail_pb.OPMailGetRet()
		msg:ParseFromString(msgBuff)
		self:onReceiveMailGetInfo(container, msg)
		return
	end
end

--------------------领取回包---------------------
function MailPageBase:onReceiveMailGetInfo( container, msg )
	local deleteId = msg.id;

	local maxSize = table.maxn(MailInfo.mails);

	local deleteIndex = 0;
	local count = 1;

	for i =1, maxSize, 1 do
		local mail = MailInfo.mails[i];

		if mail.id == deleteId then
			deleteIndex = i;
		end
	end

	table.remove(MailInfo.mails, deleteIndex);
    table.remove(mailInvalidateList , deleteId)
	self:rebuildAllItem(container);
	self:refreshPage(container);
end

--------------------请求服务端邮件列表信息---------------------
function MailPageBase:sendMsgForMailInfo( container )
	local msg = Mail_pb.OPMailInfo();

	--local index = table.maxn(MailInfo.mails);
	if MailInfo.lastMail ~= nil and #MailInfo.lastMail ~= 0 then
		--local mail = MailInfo.mails[index];
		--if mail ~= nil then
			msg.version = MailInfo.lastMail.id
		--else
			--msg.version = 0;
		--end
	else
		msg.version = 0;
	end

	local pb_data = msg:SerializeToString();
	container:sendPakcet(opcodes.OPCODE_MAIL_INFO_C, pb_data, #pb_data, true);
end

--------------------请求领取邮件---------------------
function MailPageBase:sendMsgForMailGetInfo( container, id)
	local msg = Mail_pb.OPMailGet();
	msg.id = id;

	local pb_data = msg:SerializeToString();
	container:sendPakcet(opcodes.OPCODE_MAIL_GET_C, pb_data, #pb_data, true);
end


function MailPageBase:registerPacket(container)
	for key, opcode in pairs(opcodes) do
		if string.sub(key, -1) == "S" then
			container:registerPacket(opcode)
		end
	end
end

function MailPageBase:removePacket(container)
	for key, opcode in pairs(opcodes) do
		if string.sub(key, -1) == "S" then
			container:removePacket(opcode)
		end
	end
end


---------得到当前礼品类型邮件总个数----------
function MailPageBase:getCurrentRewardCount()
	if (MailInfo.mails == nil) then
		return;
	end
	--
	local maxSize = table.maxn(MailInfo.mails);
	local count = 0;
	for i=1,maxSize do
		local mail = MailInfo.mails[i];
		if mail.type == Mail_pb.Reward then
			count = count + 1;
		end
	end

	return count;
end


function MailPageBase:onReceiveMessage(container)
	local message = container:getMessage()
	local typeId = message:getTypeId()
	if typeId == MSG_MAINFRAME_REFRESH then
		local pageName = MsgMainFrameRefreshPage:getTrueType(message).pageName
		if pageName == thisPageName then
			self:rebuildAllItem(container);
			self:refreshPage(container);
		end
	end
end

-------------------------------------------------------------------------
local CommonPage = require("CommonPage");
local MailPage = CommonPage.newSub(MailPageBase, thisPageName, option);


