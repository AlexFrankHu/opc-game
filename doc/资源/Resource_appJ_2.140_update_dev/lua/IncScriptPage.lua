local pageMap = {};

function RegisterLuaPage(pageName)
	if pageMap[pageName] == nil then
		registerScriptPage(pageName);
		pageMap[pageName] = 1;
	end
end

local deviceTable = {
	"iPhone3,1",
	"iPhone3,2",
	"iPhone3,3",
	"iPhone4,1",
}
local platformInfo = libOS:getInstance():getPlatformInfo()
local posOfFirstUnderline = string.find(platformInfo, "#") or 1
local iosDeviceName = string.sub(platformInfo,1,posOfFirstUnderline-1)
local bIsLowDevice = false 

table.foreach(deviceTable, function(i, v)
	if iosDeviceName == v then
		bIsLowDevice = true
	else
		bIsLowDevice = false
	end
end)

if true then
	--竞技场页面
	RegisterLuaPage("ArenaPage");
	--礼包页面
	RegisterLuaPage("GiftPage");
	--邮件页面
	RegisterLuaPage("MailPage");
	--充值页面
	RegisterLuaPage("RechargePage");
	--帮助页面
	RegisterLuaPage("HelpPage");
	--熔炼页面
	RegisterLuaPage("MeltPage");
	--公会页面
	RegisterLuaPage("GuildPage");
	--活动
	RegisterLuaPage("ActivityPage");
	RegisterLuaPage("MercenaryUpStepPage")
	--精英副本页面
	RegisterLuaPage("EliteMapInfoPage")
end
--主页面
RegisterLuaPage("MainScenePage")
--战斗、聊天页面
RegisterLuaPage("BattlePage")
--选角色时提示
RegisterLuaPage("PromptPage");
--选人页面
RegisterLuaPage("ChooseRolePage")
--装备页面
RegisterLuaPage("EquipmentPage");
--背包页面
RegisterLuaPage("PackagePage");
--技能页面
RegisterLuaPage("SkillPage");
--商城页面
 RegisterLuaPage("MarketPage");
--佣兵页面
RegisterLuaPage("MercenaryPage");
--个人信息页面
RegisterLuaPage("PlayerInfoPage");
--装备选择页面
RegisterLuaPage("EquipSelectPage");
--多人团战页面
RegisterLuaPage("RegimentWarPage");


