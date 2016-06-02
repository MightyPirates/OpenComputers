
local filesystem = require("filesystem")
local serialization = {}

------------------------------------------------- Private methods -----------------------------------------------------------------

local function doSerialize(array, text, prettyLook, indentationSymbol, oldIndentationSymbol, equalsSymbol)
	text = {"{"}
	table.insert(text, (prettyLook and "\n" or nil))
	
	for key, value in pairs(array) do
		local keyType, valueType, stringValue = type(key), type(value), tostring(value)

		if keyType == "number" or keyType == "string" then
			table.insert(text, (prettyLook and indentationSymbol or nil))
			table.insert(text, "[")
			table.insert(text, (keyType == "string" and table.concat({"\"", key, "\""}) or key))
			table.insert(text, "]")
			table.insert(text, equalsSymbol)
			
			if valueType == "number" or valueType == "boolean" or valueType == "nil" then
				table.insert(text, stringValue)
			elseif valueType == "string" or valueType == "function" then
				table.insert(text, "\"")
				table.insert(text, stringValue)
				table.insert(text, "\"")
			elseif valueType == "table" then
				table.insert(text, table.concat(doSerialize(value, text, prettyLook, table.concat({indentationSymbol, indentationSymbol}), table.concat({oldIndentationSymbol, indentationSymbol}), equalsSymbol)))
			else
				error("Unsupported table value type: " .. valueType)
			end
			
			table.insert(text, ",")
			table.insert(text, (prettyLook and "\n" or nil))
		else
			error("Unsupported table key type: " .. keyType)
		end
	end

	table.remove(text, (prettyLook and #text - 1 or #text))
	table.insert(text, (prettyLook and oldIndentationSymbol or nil))
	table.insert(text, "}")
	return text
end

------------------------------------------------- Public methods -----------------------------------------------------------------

function serialization.serialize(array, prettyLook, indentationWidth, indentUsingTabs)
	checkArg(1, array, "table")
	indentationWidth = indentationWidth or 2
	local indentationSymbol = indentUsingTabs and "	" or " "
	indentationSymbol, indentationSymbolHalf = string.rep(indentationSymbol, indentationWidth)
	return table.concat(doSerialize(array, {}, prettyLook, indentationSymbol, "", prettyLook and " = " or "="))
end

function serialization.unserialize(serializedString)
	checkArg(1, serializedString, "string")
	local success, result = pcall(load("return " .. serializedString))
	if success then return result else return nil, result end
end

function serialization.serializeToFile(path, array, prettyLook, indentationWidth, indentUsingTabs, appendToFile)
	checkArg(1, path, "string")
	checkArg(2, array, "table")
	filesystem.makeDirectory(filesystem.path(path) or "")
	local file = io.open(path, appendToFile and "a" or "w")
	file:write(serialization.serialize(array, prettyLook, indentationWidth, indentUsingTabs))
	file:close()
end

function serialization.unserializeFromFile(path)
	checkArg(1, path, "string")
	if filesystem.exists(path) then
		if filesystem.isDirectory(path) then
			error("\"" .. path .. "\" is a directory")
		else
			local file = io.open(path, "r")
			local data = serialization.unserialize(file:read("*a"))
			file:close()
			return data
		end
	else
		error("\"" .. path .. "\" doesn't exists")
	end
end

------------------------------------------------- Playground -----------------------------------------------------------------

-- local sampleTable = {
-- 	"one",
-- 	"two",
-- 	"three",
-- 	[9] = 9,
-- 	[10] = "ten",
-- 	[11] = "eleven",
-- 	["test"] = "sucess",
-- 	["tableTest"] = {
-- 		["tableInTable"] = {
-- 			[12] = "blah-blah",
-- 			["hello"] = "world",
-- 		},
-- 		"cyka",
-- 		"blyad",
-- 	},
-- 	["nilTest"] = nil,
-- 	["booleanTest"] = true,
-- }

-- local function fillTable(count)
-- 	print("Filling table with " .. count .. " random values...")
-- 	for i = 1, count do
-- 		table.insert(sampleTable, math.random(1, 100000))
-- 	end
-- end

-- local function executeAndPrintExecutionTime(method, ...)
-- 	local oldTime = os.clock()
-- 	method(...)
-- 	print("Execution time: " .. os.clock() - oldTime .. " seconds")
-- end

-- local function compareAlgorithms()
-- 	print(" ")
-- 	fillTable(10000)
-- 	print(" ")
-- 	local oldSerialization = require("openOSSerialization")
-- 	print("Old:")
-- 	executeAndPrintExecutionTime(oldSerialization.serialize, sampleTable)
-- 	print("New:")
-- 	executeAndPrintExecutionTime(serialization.serialize, sampleTable)
-- 	print(" ")
-- end

-- compareAlgorithms()

----------------------------------------------------------------------------------------------------------------------

-- print("Serializing...")
-- local serializedTable = serialization.serialize(sampleTable, true)
-- print(" ")
-- print(serializedTable)
-- print(" ")
-- print("Done.")
-- print("Unserializing...")
-- local unserializedTable = serialization.unserialize(serializedTable)
-- print("Done.")
-- print("Saving to file...")
-- serialization.serializeToFile("serializationTest.lua", sampleTable, true)
-- print("Done.")
-- print("Loading from file...")
-- serialization.unserializeFromFile("serializationTest.lua")
-- print("Done.")

----------------------------------------------------------------------------------------------------------------------

return serialization




