local AppstoreHandle = {}

local isAppstore = VaribleManager:getInstance():getSetting("isAppstore")

function AppstoreHandle:handleRecharge( container )
	
	if isAppstore == "0" then
	    container:getVarNode("mMonthCardTexNode"):setVisible(false) 
	else
		container:getVarNode("mMonthCardTexNode"):setVisible(true) 
	end
	
	container:getVarNode("mLargeRechargeNode"):setVisible(false)
end

function AppstoreHandle:handleActivityToAds( container )
	if isAppstore == "0" then
		container:getVarNode("mAdvertisementNode"):setVisible(true)
	    container:getVarNode("mOpinionNode"):setVisible(false) 
	else
		container:getVarNode("mAdvertisementNode"):setVisible(false)
	    container:getVarNode("mOpinionNode"):setVisible(true) 
	end
end

function AppstoreHandle:handleGuildMain( container )
	if isAppstore == "0" then
	    --container:getVarNode("mGuildContendNode"):setVisible(false) 
		--container:getVarNode("mNotOpenNode"):setVisible(false) 
	else
		--container:getVarNode("mGuildContendNode"):setVisible(true) 
		--container:getVarNode("mNotOpenNode"):setVisible(true)  
	end
end

function AppstoreHandle:handleGuild( container )
	if isAppstore == "0" then
	    container:getVarNode("mContendArrayNode"):setVisible(false) 
	else
		container:getVarNode("mContendArrayNode"):setVisible(true) 
	end
end

function AppstoreHandle:handleCDK( container )
	if isAppstore == "0" then
	    container:getVarNode("mCdkKey"):setVisible(false) 
	else
		container:getVarNode("mCdkKey"):setVisible(true) 
	end
end

function AppstoreHandle:handleFriend( container )
	if isAppstore == "0" then
	    container:getVarNode("mFriendRecommendNode"):setVisible(false) 
	else
		container:getVarNode("mFriendRecommendNode"):setVisible(true) 
	end
end

function AppstoreHandle:handSkill( container )
	if isAppstore == "0" then
	    container:getVarNode("mSkillspecialtyNode"):setVisible(false) 
	else
		container:getVarNode("mSkillspecialtyNode"):setVisible(true) 
	end
end

function AppstoreHandle:handlePopActivity( container )
	if isAppstore == "0" then
	    container:getVarNode("mActivityPicApp"):setVisible(true) 
		container:getVarNode("mAppActBtn"):setVisible(false)
	else
		if container:getVarNode("mActivityPicApp")~=nil then
			container:getVarNode("mActivityPicApp"):setVisible(false)
		end
		container:getVarNode("mAppActBtn"):setVisible(true) 
	end
end

function AppstoreHandle:handleMerHalo( container )
	if isAppstore == "0" then
	    container:getVarNode("mAppHaloNode"):setVisible(false) 
	else
		container:getVarNode("mAppHaloNode"):setVisible(true)
	end
end

function AppstoreHandle:handleSetting( container )
	if isAppstore == "0" then
	    container:getVarNode("mAreaShow"):setVisible(false) 
	else
		container:getVarNode("mAreaShow"):setVisible(true)
	end
end

return AppstoreHandle