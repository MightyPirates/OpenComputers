--[[
This will not support FAT32.
1. Because FAT16 won't even properly fit on a tape or max sized file.
2. Because FAT32's structure is incompatible with FAT12 and FAT16
--]]
--local vcomp = require("vcomponent")
local fs = require("filesystem")
local io = require("io")

local NUL = '\0'

local msdos = {}
local _msdos = {}

function _msdos.readRawString(file, size)
	local str = ""
	while #str < size do
		str = str .. file:read(size - #str)
	end
	return str
end

if string.pack then
	function _msdos.string2number(data)
		return string.unpack("<I" .. #data,data)
	end

	function _msdos.number2string(data, size)
		return string.pack("<I" .. size,data)
	end
else
	function _msdos.string2number(data)
		local count = 0
		for i = 1,#data do
			count = count + bit32.lshift(data:byte(i,i),(i - 1) * 8)
		end
		return count
	end

	function _msdos.number2string(data, size)
		local str = ""
		for i = 1, size do
			str = str .. string.char(bit32.rshift(bit32.band(data, bit32.lshift(0xFF, (i - 1) * 8)), (i - 1) * 8))
		end
		return str
	end
end

function _msdos.validateName(name)
	if name:find(".",nil,true) then
		if #name > 12 then
			return false
		end
		local filename, ext = name:match("(.*)%.(.+)")
		if #filename > 8 or filename:find(".",nil,true) or #ext > 3 then
			return false
		end
	else
		if #name > 8 then
			return false
		end
	end
	return true
end

function _msdos.spacetrim(data)
	while true do
		if data:sub(-1,-1) ~= " " and data:sub(-1,-1) ~= NUL then
			break
		end
		data = data:sub(1,-2)
	end
	return data
end

function _msdos.cleanPath(path)
	return table.concat(fs.segments(path), "/"):lower()
end

function _msdos.readDirEntry(fatset,block,count)
	local entry = {}
	local filename = _msdos.spacetrim(block:sub(1,8))
	local ext = _msdos.spacetrim(block:sub(9,11))
	entry.rawfilename = filename .. (ext ~= "" and "." or "") .. ext
	entry.filename = string.lower(entry.rawfilename)
	entry.attrib = _msdos.string2number(block:sub(12,12))
	entry.modifyT = _msdos.string2number(block:sub(23,24))
	entry.modifyD = _msdos.string2number(block:sub(25,26))
	local cluster
	if fatset.fatsize == 12 then
		cluster = _msdos.string2number(block:sub(27,28)) % 0x1000
	else
		cluster = _msdos.string2number(block:sub(27,28))
	end
	entry.cluster = cluster
	entry.size = _msdos.string2number(block:sub(29,32))
	return entry
end

function _msdos.readDirBlock(fatset, block)
	local list = {}
	for i = 0, (#block / 32) - 1 do
		local data = _msdos.readDirEntry(fatset, block:sub(i * 32 + 1, (i + 1) * 32),i)
		table.insert(list, data)
	end
	return list
end

function _msdos.cluster2block(fatset, cluster)
	return fatset.rb + (fatset.fatc * fatset.fatbc) + (fatset.rdec * 32 / fatset.bps) + ((cluster - 2) * fatset.spc)
end

function _msdos.fatclusterlookup(fatset, cluster)
	return (fatset.bps * fatset.rb) + (cluster * 2)
end

function _msdos.getFATEntry12(fatset, cluster)
	cluster = cluster % 0x1000
	local fatTable = fatset.fatCache.fatTable
	if cluster % 2 == 0 then
		return bit32.band(_msdos.string2number(fatTable:sub((cluster * 1.5) + 1, (cluster * 1.5) + 2)), 0x0FFF)
	else
		return bit32.rshift(_msdos.string2number(fatTable:sub(math.floor(cluster * 1.5) + 1, math.floor(cluster * 1.5) + 2)), 4)
	end
end

function _msdos.getFATEntry16(fatset, cluster)
	cluster = cluster % 0x10000
	local fatTable = fatset.fatCache.fatTable
	return _msdos.string2number(fatTable:sub((cluster * 2) + 1, (cluster * 2) + 2))
end

function _msdos.getFATEntry(fatset, cluster)
	if fatset.fatsize == 12 then
		return _msdos.getFATEntry12(fatset, cluster)
	else
		return _msdos.getFATEntry16(fatset, cluster)
	end
end

function _msdos.setFATEntry12(fatset, file, cluster, data)
	cluster = cluster % 0x1000
	if cluster < 2 then
		error("Attempted to modify cluster " .. cluster .. "\n" .. debug.traceback())
	end
	local fatTable = fatset.fatCache.fatTable
	if cluster % 2 == 0 then
		local fatcrap = _msdos.string2number(fatTable:sub((cluster * 1.5) + 1, (cluster * 1.5) + 2))
		fatcrap = _msdos.number2string(bit32.band(fatcrap,0xF000) + bit32.band(data,0x0FFF), 2)

		for i = 0,fatset.fatc - 1 do
			file:seek("set", (fatset.bps * fatset.rb) + (i * fatset.bps * fatset.fatbc) + (cluster * 1.5))
			file:write(fatcrap)
		end
		fatTable = fatTable:sub(1, (cluster * 1.5)) .. fatcrap .. fatTable:sub((cluster * 1.5) + 3)
	else
		local fatcrap = _msdos.string2number(fatTable:sub(math.floor(cluster * 1.5) + 1, math.floor(cluster * 1.5) + 2))
		fatcrap = _msdos.number2string(bit32.lshift(data,4) + bit32.band(fatcrap,0x000F), 2)

		for i = 0,fatset.fatc - 1 do
			file:seek("set", (fatset.bps * fatset.rb) + math.floor(cluster * 1.5))
			file:write(fatcrap)
		end
		fatTable = fatTable:sub(1, math.floor(cluster * 1.5)) .. fatcrap .. fatTable:sub(math.floor(cluster * 1.5) + 3)
	end
	fatset.fatCache.fatTable = fatTable
end

function _msdos.setFATEntry16(fatset, file, cluster, data)
	cluster = cluster % 0x10000
	if cluster < 2 then
		error("Attempted to modify cluster " .. cluster .. "\n" .. debug.traceback())
	end
	local fatTable = fatset.fatCache.fatTable
	local fatcrap = _msdos.number2string(data, 2)
	for i = 0,fatset.fatc - 1 do
		file:seek("set", (fatset.bps * fatset.rb) + (i * fatset.bps * fatset.fatbc) + (cluster * 2))
		file:write(fatcrap)
	end
	fatTable = fatTable:sub(1, (cluster * 2)) .. fatcrap .. fatTable:sub((cluster * 2) + 3)
	fatset.fatCache.fatTable = fatTable
end

function _msdos.setFATEntry(fatset, file, cluster, data)
	if fatset.fatsize == 12 then
		_msdos.setFATEntry12(fatset, file, cluster, data)
	else
		_msdos.setFATEntry16(fatset, file, cluster, data)
	end
end

function _msdos.getClusterChain(fatset, startcluster)
	local cache = {[startcluster] = true}
	local chain = {startcluster}
	local nextcluster = startcluster
	local highcluster
	if fatset.fatsize == 12 then
		highcluster = 0x0FF7
	else
		highcluster = 0xFFF7
	end
	if nextcluster <= 0x0002 or nextcluster >= highcluster then
		return chain
	end
	while true do
		if fatset.fatsize == 12 then
			nextcluster = _msdos.getFATEntry12(fatset, nextcluster)
		else
			nextcluster = _msdos.getFATEntry16(fatset, nextcluster)
		end
		table.insert(chain, nextcluster)
		if nextcluster <= 0x0002 or nextcluster >= highcluster or cache[nextcluster] == true then
			if nextcluster <= highcluster then
				print("msdos: Bad cluster chain, " .. startcluster)
				print(table.concat(chain, ","))
			end
			break
		end
		cache[nextcluster] = true
	end
	return chain
end

function _msdos.findFreeCluster(fatset)
	local fatTable = fatset.fatCache.fatTable
	for i = 0, fatset.tnoc - 1 do
		if not fatset.reserved[i] then
			local entry
			if fatset.fatsize == 12 then
				entry = _msdos.getFATEntry12(fatset, i)
			else
				entry = _msdos.getFATEntry16(fatset, i)
			end
			if entry == 0 then
				return i
			end
		end
	end
end

function _msdos.readEntireEntry(fatset, file, startcluster)
	local list = _msdos.getClusterChain(fatset, startcluster)
	local data = ""
	for i = 1,#list - 1 do
		file:seek("set", _msdos.cluster2block(fatset, list[i]) * fatset.bps)
		data = data .. _msdos.readRawString(file, fatset.bps * fatset.spc)
	end
	return data
end

function _msdos.searchDirectoryLists(fatset, file, path)
	local pathsplit = {}
	for dir in path:gmatch("[^/]+") do
		if not _msdos.validateName(dir) then
			return false
		end
		table.insert(pathsplit, dir)
	end
	if fatset.fatCache.directoryLists[path] ~= nil then
		return true, fatset.fatCache.directoryLists[path].pos, fatset.fatCache.directoryLists[path].cluster
	end
	local blockpos = (fatset.rb + (fatset.fatc * fatset.fatbc))
	local entrycluster
	local found = true
	for i = 1,#pathsplit do
		local block
		if i == 1 then
			file:seek("set", fatset.bps * blockpos)
			block = _msdos.readRawString(file, fatset.rdec * 32)				
		else
			block = _msdos.readEntireEntry(fatset, file, entrycluster)
		end
		local dirlist = _msdos.readDirBlock(fatset, block)	
		found = false
		for _,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 and data.filename ~= "." and data.filename ~= ".." then
				if bit32.band(data.attrib,0x10) ~= 0 then
					-- Cache folders and their cluster as well
					local cacheName = ""
					for j = 1, i - 1 do
						cacheName = cacheName .. pathsplit[j] .. "/"
					end
					cacheName = cacheName .. data.filename
					fatset.fatCache.directoryLists[cacheName] = {pos = _msdos.cluster2block(fatset, data.cluster), cluster = data.cluster}
				end
			end
		end
		for _,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 and data.filename ~= "." and data.filename ~= ".." then
				if data.filename == pathsplit[i] then
					blockpos = _msdos.cluster2block(fatset, data.cluster)
					entrycluster = data.cluster
					found = true
					break
				end
			end
		end
		if found == false then
			break
		end
	end
	if found == true then
		fatset.fatCache.directoryLists[path] = {pos = blockpos, cluster = entrycluster}
	end
	return found, blockpos, entrycluster
end

function _msdos.doSomethingForFile(fatset, file, path, something)
	local name = path:match(".-([^\\/]-%.?)$")
	if not _msdos.validateName(name) then
		return false
	end
	path = fs.canonical(path .. "/..")
	found, blockpos, entrycluster = _msdos.searchDirectoryLists(fatset, file, path)
	if found == false then
		return false
	end
	file:seek("set", fatset.bps * blockpos)
	local block
	if entrycluster == nil then
		block = _msdos.readRawString(file, fatset.rdec * 32)
	else
		block = _msdos.readEntireEntry(fatset, file, entrycluster)
	end
	local dirlist = _msdos.readDirBlock(fatset, block)
	for index,data in ipairs(dirlist) do
		local fileflag = data.filename:sub(1,1)
		if fileflag == "" then fileflag = NUL end
		if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 and data.filename ~= "." and data.filename ~= ".." then
			if name == data.filename then
				something(data, index, blockpos, entrycluster)
				return true
			end
		end
	end
	return false
end

function _msdos.recursiveKill(fatset, file, entrycluster)
	local killed = {}
	local block = _msdos.readEntireEntry(fatset, file, entrycluster)
	local dirlist = _msdos.readDirBlock(fatset, block)	
	found = false
	for index,data in ipairs(dirlist) do
		local fileflag = data.filename:sub(1,1)
		if fileflag == "" then fileflag = NUL end
		if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 then
			if bit32.band(data.attrib,0x10) ~= 0 and data.filename ~= "." and data.filename ~= ".." then
				local kill = _msdos.recursiveKill(fatset, file, data.cluster)
				for i = 1,#kill do
					table.insert(killed, kill[i])
				end
				table.insert(killed, data.cluster)
			elseif bit32.band(data.attrib,0x10) == 0 and data.size > 0 then
				local kill = _msdos.getClusterChain(fatset, data.cluster)
				for i = 1, #kill - 1 do
					table.insert(killed, kill[i])
				end
			end
		end
	end
	return killed
end

function msdos.proxy(fatfile, fatsize)
	checkArg(1,fatfile,"string")
	checkArg(2,fatsize,"nil","number")
	
	if fatsize == 28 then fatsize = 32 end -- Allow for FAT28
	if fatsize ~= nil and fatsize ~= 12 and fatsize ~= 16 and fatsize ~= 32 then
		error("Invalid FAT size",2)
	end
	if fatsize == 32 then
		error("FAT32 is unsupported by this driver.",2)
	end

	if not fs.exists(fatfile) then
		error("No such file",2)
	end
	local file = io.open(fatfile,"rb")
	if file == nil then
		error("Failed to open file")
	end
	local pos, err = file:seek("set", 0x36)
	if pos == nil then
		error("Seeking failed: " .. err)
	end
	local bbs = _msdos.readRawString(file, 8)
	local fatset = {}
	file:seek("set", 0)
	local boot_block = _msdos.readRawString(file, 62)
	fatset.bps = _msdos.string2number(boot_block:sub(0x0C, 0x0D))
	fatset.spc = _msdos.string2number(boot_block:sub(0x0E, 0x0E))
	fatset.rb = _msdos.string2number(boot_block:sub(0x0F, 0x10))
	fatset.fatc = _msdos.string2number(boot_block:sub(0x11, 0x11))
	fatset.rdec = _msdos.string2number(boot_block:sub(0x12, 0x13))
	fatset.fatbc = _msdos.string2number(boot_block:sub(0x17, 0x18))
	fatset.hbc = _msdos.string2number(boot_block:sub(0x1D, 0x20))
	fatset.vsn = _msdos.string2number(boot_block:sub(0x28, 0x2B))
	fatset.bpblabel = boot_block:sub(0x2C, 0x36)
	fatset.label = _msdos.spacetrim(fatset.bpblabel)
	fatset.ident = boot_block:sub(0x37, 0x3E)
	local tnos = _msdos.string2number(boot_block:sub(0x14, 0x15))
	if tnos == 0 then
		tnos = _msdos.string2number(boot_block:sub(0x21, 0x24))
	end
	fatset.tnos = tnos
	fatset.tnoc = math.floor(tnos / fatset.spc)
	if fatsize == nil then
		print("msdos: Detecting FAT size ...")
		print("msdos: Ident suggests: " .. fatset.ident)
		if fatset.tnoc < 4085 then
			print("msdos: Detected FAT size as FAT12")
			fatsize = 12
		elseif fatset.tnoc < 65525 then
			print("msdos: Detected FAT size as FAT16")
			fatsize = 16
		else
			print("msdos: Detected FAT size as FAT32")
			fatsize = 32
		end
	end
	if fatsize == 32 then
		error("FAT32 is unsupported by this driver.",2)
	end
	fatset.fatsize = fatsize
	fatset.fatCache = {}
	fatset.fatCache.directoryLists = {}
	-- fatset.fatCache. -- TODO: Where was I going with this? >_>
	file:seek("set", fatset.bps * fatset.rb)
	local fats = {}
	for i = 1,fatset.fatc do
		fats[i] = _msdos.readRawString(file, fatset.bps * fatset.fatbc)
	end
	file:close()
	if fatset.fatc > 1 then -- Validate FATs are the same
		for i = 1,fatset.fatc - 1 do
			if fats[i] ~= fats[i+1] then
				print("msdos: FAT tables are inconsistent")
			end
		end
	end
	
	fatset.fatCache.fatTable = fats[1]
	fats = nil
	local filedescript = {}
	local writeBuff = {} -- TODO: Use
	fatset.readBuff = {} -- TODO: Use?
	fatset.reserved = {} -- Clusters reserved but not used
	local proxyObj = {}
	proxyObj.type = "filesystem"
	proxyObj.address = string.format("%04x",math.floor(fatset.vsn/65536)) .. "-" .. string.format("%04x",fatset.vsn % 0x10000) -- FAT Serial Number
	proxyObj.isDirectory = function(path)
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		if path == "" then
			return true
		end
		local isDirectory
		local file = io.open(fatfile,"rb")
		local found = _msdos.doSomethingForFile(fatset, file, path, function(data) isDirectory = bit32.band(data.attrib,0x10) ~= 0 end)
		file:close()
		if not found then
			return false
		end
		return isDirectory
	end
	proxyObj.lastModified = function(path)
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		if path == "" then
			-- No modification date for root directory
			return 0
		end
		local modifyT, modifyD
		local file = io.open(fatfile,"rb")
		local found = _msdos.doSomethingForFile(fatset, file, path, function(data) modifyT, modifyD = data.modifyT, data.modifyD end)
		file:close()
		if not found then
			return 0
		end
		local year = bit32.rshift(bit32.band(modifyD, 0xFE00), 9) + 1980
		local month = bit32.rshift(bit32.band(modifyD, 0x01E0), 5)
		local day = bit32.band(modifyD, 0x001F)
		local hour = bit32.rshift(bit32.band(modifyT, 0xF800), 11)
		local min = bit32.rshift(bit32.band(modifyT, 0x07E0), 5)
		local sec = bit32.band(modifyT, 0x001F) * 2
		return os.time{year=year, month=month, day=day, hour=hour, min=min, sec=sec}
	end
	proxyObj.list = function(path)
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		local file = io.open(fatfile,"rb")
		found, blockpos, entrycluster = _msdos.searchDirectoryLists(fatset, file, path)
		if found == false then
			file:close()
			return nil, "no such file or directory"
		end
		local block
		if entrycluster == nil then
			file:seek("set", fatset.bps * blockpos)
			block = _msdos.readRawString(file, fatset.rdec * 32)
		else
			block = _msdos.readEntireEntry(fatset, file, entrycluster)
		end
		file:close()
		local dirlist = _msdos.readDirBlock(fatset, block)
		local fslist = {}
		for _,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 and data.filename ~= "." and data.filename ~= ".." then
				if bit32.band(data.attrib,0x10) ~= 0 then
					table.insert(fslist, data.filename .. "/")
					-- Cache folders and their cluster as well
					local cacheName = path .. (path ~= "" and "/" or "") .. data.filename
					fatset.fatCache.directoryLists[cacheName] = {pos = _msdos.cluster2block(fatset, data.cluster), cluster = data.cluster}
				else
					table.insert(fslist, data.filename)
				end
			end
		end
		fslist.n = #fslist
		return fslist
	end
	proxyObj.spaceTotal = function()
		return fatset.tnoc * fatset.spc * fatset.bps
	end
	proxyObj.exists = function(path)
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		if path == "" then
			return true
		end
		local file = io.open(fatfile,"rb")
		local found = _msdos.doSomethingForFile(fatset, file, path, function() end)
		file:close()
		return found
	end
	proxyObj.open = function(path, mode)
		checkArg(1,path,"string")
		checkArg(2,mode,"string")
		if mode ~= "r" and mode ~= "rb" and mode ~= "w" and mode ~= "wb" and mode ~= "a" and mode ~= "ab" then
			error("unsupported mode",2)
		end
		path = _msdos.cleanPath(path)
		if path == "" then
			return nil, "file not found"
		end
		local filecluster, filesize
		local file = io.open(fatfile,"rb")
		local found = _msdos.doSomethingForFile(fatset, file, path, function(data) filecluster, filesize = data.cluster, data.size end)
		if not found then
			if mode:sub(1,1) ~= "w" and mode:sub(1,1) ~= "a" then
				file:close()
				return nil, "file not found"
			else
				-- Allocate file.
				local newname = path:match(".-([^\\/]-%.?)$")
				if not _msdos.validateName(newname) then
					return nil, "Invalid file path"
				end

				dirpath = fs.canonical(path .. "/..")
				
				found, blockpos, entrycluster = _msdos.searchDirectoryLists(fatset, file, dirpath)
				
				local block
				if entrycluster == nil then
					file:seek("set", fatset.bps * blockpos)
					block = _msdos.readRawString(file, fatset.rdec * 32)
				else
					block = _msdos.readEntireEntry(fatset, file, entrycluster)
				end
		
				local dirlist = _msdos.readDirBlock(fatset, block)
				local found = false
				for index,data in ipairs(dirlist) do
					local fileflag = data.filename:sub(1,1)
					if fileflag == "" then fileflag = NUL end
					if fileflag == NUL or fileflag == string.char(0xE5) then
						found = true
						local filename, ext
						if newname:find(".",nil,true) ~= nil then
							filename, ext = newname:match("(.*)%.(.+)")
							ext = ext .. string.rep(" ", 3 - #ext)
						else
							filename = newname
							ext = "   "
						end
						filename = filename .. string.rep(" ", 8 - #filename)

						local curDate = os.date("*t")
						local createT = bit32.lshift(curDate.hour, 11) + bit32.lshift(curDate.min, 5) + math.floor(curDate.sec/2)
						local createD = bit32.lshift(math.max(curDate.year - 1980,0), 9) + bit32.lshift(curDate.month, 5) + curDate.day
						if curDate.year - 1980 < 0 then
							print("msdos: WARNING: Current year before 1980, year will be invalid")
						end
						local entry = filename .. ext .. string.char(0x20) .. string.rep(NUL, 10) .. _msdos.number2string(createT,2) .. _msdos.number2string(createD,2) .. string.rep(NUL, 6)
						if entrycluster == nil then
							file:close()
							file = io.open(fatfile,"ab")
							file:seek("set", fatset.bps * blockpos + ((index - 1) * 32))
							file:write(entry)
						else
							local list = _msdos.getClusterChain(fatset, entrycluster)
							file:close()
							local clusterList = math.floor((index - 1) / (fatset.bps * fatset.spc / 32))
							local clusterPos = (index - 1) % (fatset.bps * fatset.spc / 32)
							file = io.open(fatfile,"ab")
							file:seek("set", (fatset.bps * _msdos.cluster2block(fatset, list[clusterList + 1])) + (clusterPos * 32))
							file:write(entry)
						end
						file:close()
						file = io.open(fatfile,"rb")
						filecluster, filesize = 0, 0
						break
					end
				end
				-- TODO: Attempt to find, clean, and add a cluster
				if not found then
					print("msdos: No available entry")
					return nil, "not enough space"
				end
			end
		end
		while true do
			local rnddescrpt = math.random(1000000000,9999999999)
			if filedescript[rnddescrpt] == nil then
				filedescript[rnddescrpt] = {
					seek = 0,
					mode = mode:sub(1,1) == "r" and "r" or "w",
					buffer = "",
					size = filesize,
					path = path
				}
				if filecluster == 0 then
					filedescript[rnddescrpt].chain = {}
				else
					filedescript[rnddescrpt].chain = _msdos.getClusterChain(fatset, filecluster)
				end
				if mode:sub(1,1) == "a" then
					-- TODO: Loading the entire file is bad.
					-- Do a little check
					if filecluster == 0 and filesize ~= 0 then
						print("msdos: Zero cluster with non zero file size")
						print(path)
					elseif filecluster ~= 0 and filesize == 0 then
						print("msdos: Non zero cluster with zero file size")
						print(path)
					elseif filecluster ~= 0 then -- Don't attempt to load nothing
						filedescript[rnddescrpt].buffer = _msdos.readEntireEntry(fatset, file, filecluster)
						filedescript[rnddescrpt].buffer = filedescript[rnddescrpt].buffer:sub(1, filesize)
					end
				end
				file:close()
				return rnddescrpt
			end
		end
	end
	proxyObj.remove = function(path)
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		if path == "" then
			-- TODO: Simply clear the root directory and the fat table.
			return false
		end
		local name = path:match(".-([^\\/]-%.?)$")
		if not _msdos.validateName(name) then
			return false
		end
		path = fs.canonical(path .. "/..")
		local file = io.open(fatfile,"rb")
		found, blockpos, entrycluster = _msdos.searchDirectoryLists(fatset, file, path)
		if found == false then
			file:close()
			return false
		end
		local block
		if entrycluster == nil then
			file:seek("set", fatset.bps * blockpos)
			block = _msdos.readRawString(file, fatset.rdec * 32)
		else
			block = _msdos.readEntireEntry(fatset, file, entrycluster)
		end
		local dirlist = _msdos.readDirBlock(fatset, block)
		for index,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 and data.filename == name then
				local chainlist = {}
				if bit32.band(data.attrib,0x10) == 0 and data.size > 0 then
					chainlist = _msdos.getClusterChain(fatset, data.cluster)
					chainlist[#chainlist] = nil
				end
				if bit32.band(data.attrib,0x10) ~= 0 then
					table.insert(chainlist, data.cluster)
					local murder = _msdos.recursiveKill(fatset, file, data.cluster)
					for i = 1, #murder do
						table.insert(chainlist, murder[i])
					end
				end
				if entrycluster == nil then
					file:close()
					file = io.open(fatfile,"ab")
					file:seek("set", fatset.bps * blockpos + ((index - 1) * 32))
					file:write(string.char(0xE5))
				else
					local list = _msdos.getClusterChain(fatset, entrycluster)
					local clusterList = math.floor((index - 1) / (fatset.bps * fatset.spc / 32))
					local clusterPos = (index - 1) % (fatset.bps * fatset.spc / 32)
					file:close()
					file = io.open(fatfile,"ab")
					file:seek("set", (fatset.bps * _msdos.cluster2block(fatset, list[clusterList + 1])) + (clusterPos * 32))
					file:write(string.char(0xE5))
				end
				local fakefile = {seek = function() end, write = function() end}
				for i = 1, #chainlist do
					_msdos.setFATEntry(fatset, fakefile, chainlist[i], 0x0000)
				end
				file:seek("set", fatset.bps * fatset.rb)
				for i = 1, fatset.fatc do
					file:write(fatset.fatCache.fatTable)
				end
				file:close()
				-- Invalidate list cache entries
				local listCacheKill = {}
				local preppath = ""
				if path ~= "" then
					preppath = path .. "/"
				end
				for k,v in pairs(fatset.fatCache.directoryLists) do
					if k == preppath .. name or k:sub(1, #preppath + #name + 1) == preppath .. name .. "/" then
						table.insert(listCacheKill, k)
					end
				end
				for i = 1, #listCacheKill do
					fatset.fatCache.directoryLists[listCacheKill[i]] = nil
				end
				return true
			end
		end
		file:close()
		return false
	end
	proxyObj.rename = function(path, newpath)
		checkArg(1,path,"string")
		checkArg(2,newpath,"string")
		path = _msdos.cleanPath(path)
		newpath = _msdos.cleanPath(newpath)
		if path == "" or newpath == "" then
			return false
		elseif path == newpath then
			return true
		end
		local name = path:match(".-([^\\/]-%.?)$")
		if not _msdos.validateName(name) then
			return false
		end
		local newname = newpath:match(".-([^\\/]-%.?)$")
		if not _msdos.validateName(newname) then
			return false
		end
		path = fs.canonical(path .. "/..")
		newpath = fs.canonical(newpath .. "/..")
		local file = io.open(fatfile,"rb")
		found, blockpos, entrycluster = _msdos.searchDirectoryLists(fatset, file, path)
		if found == false then
			file:close()
			return false
		end
		found, blockpos2, entrycluster2 = _msdos.searchDirectoryLists(fatset, file, newpath)
		if found == false then
			file:close()
			return false
		end
		local block
		if entrycluster == nil then
			file:seek("set", fatset.bps * blockpos)
			block = _msdos.readRawString(file, fatset.rdec * 32)
		else
			block = _msdos.readEntireEntry(fatset, file, entrycluster)
		end
		local dirlist = _msdos.readDirBlock(fatset, block)
		local entry, firstindex
		for index,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag ~= NUL and fileflag ~= string.char(0xE5) and bit32.band(data.attrib,0x08) == 0 and data.filename == name then
				firstindex = index
				local filename, ext
				if newname:find(".",nil,true) ~= nil then
					filename = newname:match("(.*)%..*")
					ext = newname:match(".*%.(.*)")
				else
					filename = newname
					ext = ""
				end
				filename = filename .. string.rep(" ", 8 - #filename)
				ext = ext .. string.rep(" ", 3 - #ext)
				entry = filename .. ext .. _msdos.number2string(data.attrib, 1) .. string.rep(NUL, 10) .. _msdos.number2string(data.modifyT, 2) .. _msdos.number2string(data.modifyD, 2) .. _msdos.number2string(data.cluster, 2) .. _msdos.number2string(data.size, 4)
				break
			end
		end
		if entry == nil then
			file:close()
			return false
		end
		if entrycluster2 == nil then
			file:seek("set", fatset.bps * blockpos2)
			block = _msdos.readRawString(file, fatset.rdec * 32)
		else
			block = _msdos.readEntireEntry(fatset, file, entrycluster2)
		end
		local dirlist = _msdos.readDirBlock(fatset, block)
		for index,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag == NUL or fileflag == string.char(0xE5) then
				if entrycluster2 == nil then
					file:close()
					file = io.open(fatfile,"ab")
					file:seek("set", fatset.bps * blockpos2 + ((index - 1) * 32))
					file:write(entry)
				else
					local list = _msdos.getClusterChain(fatset, entrycluster2)
					file:close()
					local clusterList = math.floor((index - 1) / (fatset.bps * fatset.spc / 32))
					local clusterPos = (index - 1) % (fatset.bps * fatset.spc / 32)
					file = io.open(fatfile,"ab")
					file:seek("set", (fatset.bps * _msdos.cluster2block(fatset, list[clusterList + 1])) + (clusterPos * 32))
					file:write(entry)
				end
				if entrycluster == nil then
					file:seek("set", fatset.bps * blockpos + ((firstindex - 1) * 32))
					file:write(string.rep(NUL, 32))
				else
					file:close()
					file = io.open(fatfile,"rb")
					local list = _msdos.getClusterChain(fatset, entrycluster)
					local clusterList = math.floor((firstindex - 1) / (fatset.bps * fatset.spc / 32))
					local clusterPos = (firstindex - 1) % (fatset.bps * fatset.spc / 32)
					file:close()
					file = io.open(fatfile,"ab")
					file:seek("set", (fatset.bps * _msdos.cluster2block(fatset, list[clusterList + 1])) + (clusterPos * 32))
					file:write(string.rep(NUL, 32))
				end
				file:close()
				return true
			end
		end
		-- TODO: Attempt to find, clean, and add a cluster
		return false
	end
	proxyObj.read = function(fd, count)
		count = count or 1
		checkArg(1,fd,"number")
		checkArg(2,count,"number")
		if filedescript[fd] == nil or filedescript[fd].mode ~= "r" then
			return nil, "bad file descriptor"
		end
		if #filedescript[fd].buffer >= filedescript[fd].size and filedescript[fd].seek > filedescript[fd].size then
			return nil
		end
		count = math.min(count,8192)
		if filedescript[fd].seek + count > filedescript[fd].size then
			count = filedescript[fd].size - filedescript[fd].seek
		end
		if count == 0 then
			return nil
		end
		while #filedescript[fd].buffer < filedescript[fd].seek + count do
			local nextchain = (#filedescript[fd].buffer / fatset.bps / fatset.spc) + 1
			if filedescript[fd].chain[nextchain] == nil then
				return nil
			end
			local block = _msdos.cluster2block(fatset, filedescript[fd].chain[nextchain])
			local file = io.open(fatfile,"rb")
			file:seek("set", block * fatset.bps)
			local data = _msdos.readRawString(file, fatset.bps * fatset.spc)
			file:close()
			filedescript[fd].buffer = filedescript[fd].buffer .. data
		end
		filedescript[fd].buffer = filedescript[fd].buffer:sub(1,filedescript[fd].size)
		local data = filedescript[fd].buffer:sub(filedescript[fd].seek + 1, filedescript[fd].seek + count)
		filedescript[fd].seek = filedescript[fd].seek + #data
		return data
	end
	proxyObj.close = function(fd)
		checkArg(1,fd,"number")
		if filedescript[fd] == nil then
			return nil, "bad file descriptor"
		end
		if filedescript[fd].mode == "w" then
			local clusters = math.ceil(#filedescript[fd].buffer / fatset.bps / fatset.spc)
			local chain = filedescript[fd].chain
			if clusters ~= #chain then
				print("msdos: Number of allocated clusters doesn't match buffer size")
				print("AC: " .. #chain .. " BS: " .. clusters)
				clusters = math.min(clusters, #chain)
			end
			if fatset.fatsize == 12 then
				chain[clusters + 1] = 0xFFF
			else
				chain[clusters + 1] = 0xFFFF
			end
			local bpc = fatset.bps * fatset.spc
			local file = io.open(fatfile,"ab")
			for i = 1, clusters do
				local block = _msdos.cluster2block(fatset, filedescript[fd].chain[i])
				file:seek("set", block * fatset.bps)
				file:write(filedescript[fd].buffer:sub(((i - 1) * bpc) + 1, i * bpc))
			end
			local fakefile = {seek = function() end, write = function() end}

			for i = 1, clusters do
				_msdos.setFATEntry(fatset, fakefile, filedescript[fd].chain[i], filedescript[fd].chain[i + 1])
			end
			
			file:seek("set", fatset.bps * fatset.rb)
			for i = 1, fatset.fatc do
				file:write(fatset.fatCache.fatTable)
			end
			
			file:close()

			local data, index, blockpos, entrycluster
			local file = io.open(fatfile,"rb")
			local found = _msdos.doSomethingForFile(fatset, file, filedescript[fd].path, function(data1, index1, blockpos1, entrycluster1) data, index, blockpos, entrycluster = data1, index1, blockpos1, entrycluster1 end)
			local filename, ext
			if data.filename:find(".",nil,true) ~= nil then
				filename = data.filename:match("(.*)%..*")
				ext = data.filename:match(".*%.(.*)")
			else
				filename = data.filename
				ext = ""
			end
			filename = filename .. string.rep(" ", 8 - #filename)
			ext = ext .. string.rep(" ", 3 - #ext)
			local curDate = os.date("*t")
			local createT = bit32.lshift(curDate.hour, 11) + bit32.lshift(curDate.min, 5) + math.floor(curDate.sec/2)
			local createD = bit32.lshift(math.max(curDate.year - 1980,0), 9) + bit32.lshift(curDate.month, 5) + curDate.day
			if curDate.year - 1980 < 0 then
				print("msdos: WARNING: Current year before 1980, year will be invalid")
			end
			local entry = filename .. ext .. _msdos.number2string(data.attrib, 1) .. string.rep(NUL, 10) .. _msdos.number2string(createT, 2) .. _msdos.number2string(createD, 2) .. _msdos.number2string(clusters > 0 and filedescript[fd].chain[1] or 0, 2) .. _msdos.number2string(#filedescript[fd].buffer, 4)
			if entrycluster == nil then
				file:close()
				file = io.open(fatfile,"ab")
				file:seek("set", fatset.bps * blockpos + ((index - 1) * 32))
				file:write(entry)
			else
				local list = _msdos.getClusterChain(fatset, entrycluster)
				file:close()
				local clusterList = math.floor((index - 1) / (fatset.bps * fatset.spc / 32))
				local clusterPos = (index - 1) % (fatset.bps * fatset.spc / 32)
				file = io.open(fatfile,"ab")
				file:seek("set", (fatset.bps * _msdos.cluster2block(fatset, list[clusterList + 1])) + (clusterPos * 32))
				file:write(entry)
			end
			file:close()
		end
		filedescript[fd] = nil
	end
	proxyObj.getLabel = function()
		return fatset.label
	end
	proxyObj.seek = function(fd,kind,offset)
		checkArg(1,fd,"number")
		checkArg(2,kind,"string")
		checkArg(3,offset,"number")
		if filedescript[fd] == nil then
			return nil, "bad file descriptor"
		end
		if kind ~= "set" and kind ~= "cur" and kind ~= "end" then
			error("invalid mode",2)
		end
		if offset < 0 then
			return nil, "Negative seek offset"
		end
		local newpos
		if kind == "set" then
			newpos = offset
		elseif kind == "cur" then
			newpos = filedescript[fd].seek + offset
		elseif kind == "end" then
			if filedescript[fd].mode == "r" then
				newpos = filedescript[fd].size + offset
			else
				newpos = #filedescript[fd].buffer + offset
			end
		end
		return filedescript[fd].seek
	end
	proxyObj.size = function(path)
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		if path == "" then
			return 0
		end
		local filesize
		local file = io.open(fatfile,"rb")
		local found = _msdos.doSomethingForFile(fatset, file, path, function(data) filesize = data.size end)
		file:close()
		if not found then
			return 0
		end
		return filesize
	end
	proxyObj.isReadOnly = function()
		return false -- TODO: Check if file is readonly
	end
	proxyObj.setLabel = function(newlabel)
		checkArg(1,newlabel,"string")
		-- TODO: Check readonly status
		newlabel = newlabel:sub(1,11)
		origlabel = newlabel
		fatset.label = newlabel
		if #newlabel < 11 then
			newlabel = newlabel .. string.rep(" ", 11 - #newlabel)
		end
		fatset.bpblabel = newlabel
		local file = io.open(fatfile,"ab")
		file:seek("set", 0x2B)
		file:write(newlabel)
		file:close()
		return origlabel
	end
	proxyObj.makeDirectory = function(path)
		-- TODO: Recursively make folders
		checkArg(1,path,"string")
		path = _msdos.cleanPath(path)
		if path == "" then
			return false
		end
		local name = path:match(".-([^\\/]-%.?)$")
		if not _msdos.validateName(name) then
			return false
		end
		local filename, ext
		if name:find(".",nil,true) then
			filename, ext = name:match("(.*)%.(.+)")
		else
			filename = name
			ext = ""
		end
		filename = filename .. string.rep(" ", 8 - #filename)
		ext = ext .. string.rep(" ", 3 - #ext)
		local freeCluster = _msdos.findFreeCluster(fatset)
		if freeCluster == nil then
			return false
		end
		path = fs.canonical(path .. "/..")
		local file = io.open(fatfile,"rb")
		found, blockpos, entrycluster = _msdos.searchDirectoryLists(fatset, file, path)
		if found == false then
			file:close()
			return false
		end
		local block
		if entrycluster == nil then
			file:seek("set", fatset.bps * blockpos)
			block = _msdos.readRawString(file, fatset.rdec * 32)
		else
			block = _msdos.readEntireEntry(fatset, file, entrycluster)
		end
		local dirlist = _msdos.readDirBlock(fatset, block)
		for index,data in ipairs(dirlist) do
			local fileflag = data.filename:sub(1,1)
			if fileflag == "" then fileflag = NUL end
			if fileflag == NUL or fileflag == string.char(0xE5) then
				local curDate = os.date("*t")
				local createT = bit32.lshift(curDate.hour, 11) + bit32.lshift(curDate.min, 5) + math.floor(curDate.sec/2)
				local createD = bit32.lshift(math.max(curDate.year - 1980,0), 9) + bit32.lshift(curDate.month, 5) + curDate.day
				if curDate.year - 1980 < 0 then
					print("msdos: WARNING: Current year before 1980, year will be invalid")
				end
				local entry = filename .. ext .. string.char(0x30) .. string.rep(NUL, 10) .. _msdos.number2string(createT,2) .. _msdos.number2string(createD,2) .. _msdos.number2string(freeCluster, 2) .. string.rep(NUL, 4)
				if entrycluster == nil then
					file:close()
					file = io.open(fatfile,"ab")
					file:seek("set", fatset.bps * blockpos + ((index - 1) * 32))
					file:write(entry)
				else
					local list = _msdos.getClusterChain(fatset, entrycluster)
					file:close()
					local clusterList = math.floor((index - 1) / (fatset.bps * fatset.spc / 32))
					local clusterPos = (index - 1) % (fatset.bps * fatset.spc / 32)
					file = io.open(fatfile,"ab")
					file:seek("set", (fatset.bps * _msdos.cluster2block(fatset, list[clusterList + 1])) + (clusterPos * 32))
					file:write(entry)
				end
				file:seek("set", _msdos.cluster2block(fatset, freeCluster) * fatset.bps)
				-- TODO: Write dot entries
				file:write(string.rep(NUL, fatset.bps * fatset.spc))
				_msdos.setFATEntry(fatset, file, freeCluster, 0xFFFF)
				file:close()
				return true
			end
		end
		-- TODO: Attempt to find, clean, and add a cluster
		file:close()
		return false
	end
	proxyObj.spaceUsed = function()
		local count = 0
		for i = 0, fatset.tnoc - 1 do
			local entry = _msdos.getFATEntry(fatset, i)
			if entry ~= 0 then
				count = count + 1
			end
		end
		return count * (fatset.spc * fatset.bps)
	end
	proxyObj.write = function(fd,data)
		checkArg(1,fd,"number")
		checkArg(2,data,"string")
		if filedescript[fd] == nil or filedescript[fd].mode ~= "w" then
			return nil, "bad file descriptor"
		end
		-- Check if we need more clusters
		if math.ceil(#filedescript[fd].buffer / (fatset.spc * fatset.bps)) < math.ceil((#filedescript[fd].buffer + #data) / (fatset.spc * fatset.bps)) then
			local freeCluster = _msdos.findFreeCluster(fatset)
			if freeCluster == nil then
				print("msdos: No more clusters")
				return nil, "not enough space"
			end
			local chain = filedescript[fd].chain
			chain[#chain + 1] = freeCluster
			fatset.reserved[freeCluster] = true
		end
		if #filedescript[fd].buffer < filedescript[fd].seek then
			filedescript[fd].buffer = filedescript[fd].buffer .. string.rep(NUL, filedescript[fd].seek - #filedescript[fd].buffer)
		end
		filedescript[fd].buffer = filedescript[fd].buffer:sub(1,filedescript[fd].seek) .. data .. filedescript[fd].buffer:sub(filedescript[fd].seek + #data + 1)
		filedescript[fd].seek = filedescript[fd].seek + #data
		return true
	end
	proxyObj.fat = fatset
	--vcomp.register(proxyObj.address, proxyObj.type, proxyObj)
	return proxyObj
end
return msdos
