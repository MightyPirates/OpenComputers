
local unsafe={
	["and"]=true, ["break"]=true, ["do"]=true, ["else"]=true,
	["elseif"]=true, ["end"]=true, ["false"]=true, ["for"]=true,
	["function"]=true, ["goto"]=true, ["if"]=true, ["in"]=true,
	["local"]=true, ["nil"]=true, ["not"]=true, ["or"]=true,
	["repeat"]=true, ["return"]=true, ["then"]=true, ["true"]=true,
	["until"]=true, ["while"]=true
}
function serialize(dat)
	local out=""
	local queue={{dat}}
	local cv=0
	local keydat
	local ptbl={}
	while queue[1] do
		local cu=queue[1]
		table.remove(queue,1)
		local typ=type(cu[1])
		local ot
		if typ=="string" then
			ot=string.gsub(string.format("%q",cu[1]),"\\\n","\\n")
		elseif typ=="number" or typ=="boolean" or typ=="nil" then
			ot=tostring(cu[1])
		elseif typ=="table" then
			local empty=true
			ot="{"
			local st=0
			ptbl[#ptbl+1]=cu[1]
			local cnt=1
			local bl={}
			while rawget(cu[1],cnt) do -- rawget because the component metatable borks
				bl[cnt]=true
				st=st+1
				table.insert(queue,st,{k,"nkey"})
				st=st+1
				local val=rawget(cu[1],cnt)
				if type(v)=="table" then
					for n,l in pairs(ptbl) do
						if l==v then
							val="recursive"
							break
						end
					end
				end
				table.insert(queue,st,{val,"value",nil,st/2})
				cnt=cnt+1
			end
			for k,v in pairs(cu[1]) do
				empty=false
				if not bl[k] then
					st=st+1
					table.insert(queue,st,{k,"key"})
					st=st+1
					local val=v
					if type(v)=="table" then
						for n,l in pairs(ptbl) do
							if l==v then
								val="recursive"
							end
						end
					end
					table.insert(queue,st,{val,"value",nil,st/2})
				end
			end
			if empty then
				ot=ot.."}"
				ptbl[#ptbl]=nil
				typ="emptytable"
			else
				cv=cv+1
				if cu[3] then
					queue[st][3]=cu[3]
					cu[3]=nil
				end
				queue[st][3]=(queue[st][3] or 0)+1
			end
		elseif typ=="function" then
			ot="function"
		end
		if cu[2]=="nkey" then
			keydat={"",""}
			ot=""
		elseif cu[2]=="key" then
			if typ=="string" then
				if cu[1]:match("^[%a_]([%w_]*)$") and not unsafe[cu[1]] then
					ot=cu[1].."="
				else
					ot="["..ot.."]="
				end
			else
				ot="["..ot.."]="
			end
			keydat={cu[1],ot}
			ot=""
		elseif cu[2]=="value" then
			if keydat and keydat[1]~=cu[4] then
				ot=keydat[2]..ot
			end
			if cu[3] then
				ot=ot..("}"):rep(cu[3])
				for l1=1,cu[3] do
					ptbl[#ptbl]=nil
				end	
				cv=cv-cu[3]
				if cv~=0 then
					ot=ot..","
				end
			elseif typ~="table" then
				ot=ot..","
			end
		end
		out=out..ot
	end
	if #out>1000 then
		out=out:sub(1,1000).."..."
	end
	return out
end

local component = require("component")
local package = require("package")
local term = require("term")

local history = {}
local env = setmetatable({}, {__index = function(t, k)
	return _ENV[k] or package.loaded[k]
end})

print("Lua 5.2.3 Copyright (C) 1994-2013 Lua.org, PUC-Rio")

while term.isAvailable() do
	local foreground = component.gpu.setForeground(0x00FF00)
	term.write("lua> ")
	component.gpu.setForeground(foreground)
	local command = term.read(history)
	if command == nil then -- eof
		return
	end
	while #history > 10 do
		table.remove(history, 1)
	end
	local statement, result = load(command, "=stdin", "t", env)
	local expression = load("return " .. command, "=stdin", "t", env)
	local code = expression or statement
	if code then
		local result = table.pack(pcall(code))
		if not result[1] then
			print(result[2])
		else
			for i=1,result.n do
				result[i]=serialize(result[i])
			end
			print(table.unpack(result,2,result.n))
		end
	else
		print(serialize(result))
	end
end
