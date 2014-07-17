local component = require('component')
local unicode = require('unicode')
local term = require('term')
local shell = require('shell')


local args, options = shell.parse(...)
local listFunctions = false

local max = tonumber(options ['n']) or math.huge
filter = args [#args]

local t = {}
local m = 1
local tm = 1

for k,v in component.list( filter ) do
	if v:len () > m then m = v:len () end
	if type(v):len() > tm then tm = type(v):len() end

	t[k]=v
end

local i = 1
m = m + 10
for k,v in pairs (t) do
	if i <= max then
		term.write ( v .. string.rep (' ', m - v:len()) .. tostring(k) .. "\n" )

		if options ['l'] == true then
			local _p = component.proxy(k)
			local m = 1
			for _k,_v in pairs (_p) do
				if _k:len () > m then m = _k:len() end
			end
			m = m + 3

			for _k,_v in pairs(_p) do
				term.write ( ' ' .. _k .. string.rep (' ', m-_k:len()) .. type(_v) .. string.rep(' ', tm - type(_v):len() + 3) .. tostring(_v) .. "\n" )
			end
		end
	end
	i = i + 1
end
