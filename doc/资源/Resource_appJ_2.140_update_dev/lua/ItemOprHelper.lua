local ItemOprHelper = {};
--------------------------------------------------------------------------------
------------local variable for system api--------------------------------------
local tostring = tostring;
local tonumber = tonumber;
local string = string;
local pairs = pairs;
--------------------------------------------------------------------------------
local ItemOpr_pb 	= require("ItemOpr_pb");
local HP_pb		= require("HP_pb");
--------------------------------------------------------------------------------
--使用
function ItemOprHelper:useItem(itemId, count)
	local msg = ItemOpr_pb.HPItemUse();
	msg.itemId = itemId;
	msg.itemCount = count or 1;
	common:sendPacket(HP_pb.ITEM_USE_C, msg);
end	

--打开10个
function ItemOprHelper:useTenItem(itemId)
    local UserItemManager = require("UserItemManager")
    local item = UserItemManager:getUserItemByItemId(itemId)
    if item~=nil and item.count<10 then
        self:useItem(itemId, item.count);
        return;
    end
	self:useItem(itemId, 10);
end

--出售
function ItemOprHelper:sellItem(itemId, count)
	local msg = ItemOpr_pb.HPItemSell();
	msg.itemId = itemId;
	msg.count = count or 1;
	
	common:sendPacket(HP_pb.ITEM_SELL_C, msg, false);
end

--回收
function ItemOprHelper:recycleItem(itemId)
	local msg = ItemOpr_pb.HPGongceWordCycle();
	msg.itemId = itemId;
	common:sendPacket(HP_pb.WORDS_EXHCNAGE_CYCLE_C, msg, false);
end
--------------------------------------------------------------------------------
return ItemOprHelper;