local component = require("component")
local computer = require("computer")
--local event = require("event")
local tprotect = raw_dofile("/lib/tprotect.lua")
_G.package.loaded.tprotect = tprotect

local bit32 = raw_dofile("/lib/bit32.lua")
_G.package.loaded.bit32 = bit32

local uuid = raw_dofile("/lib/uuid.lua")
_G.package.loaded.uuid = uuid

--------------------------------------------

local lib = {}

--зашишенное хранилишя данных

local cryptoDatasPassword = {}
local cryptoDatas = {}

function lib.setCryptoData(name, password, data)
    if cryptoDatas[name] then
        if password == cryptoDatasPassword[name] then
            cryptoDatas[name] = data
            return true
        else
            return false, "uncorrect password"
        end
    else
        cryptoDatas[name] = data
        cryptoDatasPassword[name] = password
        return true
    end
end

function lib.isCryptoData(name)
    return not not cryptoDatas[name]
end

function lib.removeCryptoData(name, password)
    if cryptoDatas[name] then
        if password == cryptoDatasPassword[name] then
            cryptoDatas[name] = nil
            cryptoDatasPassword[name] = nil
            return true
        else
            return false, "uncorrect password"
        end
    else
        return false, "no this data part"
    end
end

function lib.getCryptoData(name, password)
    if cryptoDatas[name] then
        if password == cryptoDatasPassword[name] then
            return cryptoDatas[name]
        else
            return false, "uncorrect password"
        end
    else
        return false, "no this data part"
    end
end

--подмена доступа к компонентам

local fakeMethods = {}
local origInvoke = component.invoke

function component.invoke(address, method, ...)
    if fakeMethods[address] then
        if fakeMethods[address][method] then
            return fakeMethods[address][method](address, function(...)
                return origInvoke(address, method, ...)
            end, method, ...)
        end
    end
    return origInvoke(address, method, ...)
end

function lib.addFilterMethod(address, methodName, toFunc)
    if not fakeMethods[address] then fakeMethods[address] = {} end
    if fakeMethods[address][methodName] then return false end
    fakeMethods[address][methodName] = toFunc

    return function()
        if fakeMethods[address][methodName] then
            fakeMethods[address][methodName] = nil
            return true
        end
        return false
    end
end

--ограничения прав доступа

local globalPermitsPassword = uuid.next()
local readonlyEnable = true
local eepromCrypto = true
local readonlyLists = {
{"/bin", "/lib", "/boot", "/autoruns/system",
"/init.lua", "/etc/motd", "/etc/profile.lua",
"/etc/palette", "/etc/logoBW.pic", "/etc/logo.pic",
"/etc/lowPower.pic", "/etc/system.cfg", "/filelist.txt",
"/version.cfg"}}

function lib.setGlobalPermitsPassword(password)
    if globalPermitsPassword then
        return false, "global permits password setted"
    else
        globalPermitsPassword = password
        return true
    end
end

function lib.resetGlobalPermitsPassword(password)
    if not globalPermitsPassword then
        return false, "global permits password is not setted"
    else
        if password == globalPermitsPassword then
            globalPermitsPassword = nil
            return true
        else
            return false, "uncorrect global password"
        end
    end
end

function lib.isGlobalPermitsPassword()
    return not not globalPermitsPassword
end

function lib.getGlobalReadOnlyFiles()
    local list = {}

    for k, v in ipairs(readonlyLists) do
        for k, v in ipairs(v) do
            table.insert(list, v)
        end
    end

    return list
end

function lib.getRealReadOnlyTables(password)
    if not globalPermitsPassword or globalPermitsPassword == password then
        return readonlyLists
    end
    return false, "uncorrect global password"
end

function lib.isReadOnly(path)
    local fs = require("filesystem")
    local su = require("superUtiles")
    if path:sub(1, 1) ~= "/" then path = "/" .. path end
    path = fs.canonical(path)

    local state = su.inTable(lib.getGlobalReadOnlyFiles(), path)

    if not state then
        for _, file in ipairs(lib.getGlobalReadOnlyFiles()) do
            if path:sub(1, #file) == file then
                state = true
                break
            end
        end
    end

    if not state then
        for _, file in ipairs(lib.getGlobalReadOnlyFiles()) do
            if file:sub(1, #path) == path then
                state = true
                break
            end
        end
    end

    return state
end

function lib.addReadOnlyList(globalPassword, tbl)
    local su = require("superUtiles")
    if not globalPermitsPassword or globalPermitsPassword == globalPassword then
        if not su.inTable(readonlyLists, tbl) then
            table.insert(readonlyLists, tbl)
            return true
        else
            return false, "this list has already been added"
        end
    end
    return false, "uncorrect global password"
end

function lib.resetReadOnlyList(globalPassword, tbl)
    local su = require("superUtiles")
    if not globalPermitsPassword or globalPermitsPassword == globalPassword then
        if su.inTable(readonlyLists, tbl) then
            su.tableRemove(readonlyLists, tbl)
            return true
        else
            return false, "list is not found"
        end
    end
    return false, "uncorrect global password"
end

function lib.setReadOnlyState(password, state)
    if not globalPermitsPassword or globalPermitsPassword == password then
        readonlyEnable = state
        return true
    end
    return false, "uncorrect global password"
end

function lib.setEepromCrypto(password, state)
    if not globalPermitsPassword or globalPermitsPassword == password then
        eepromCrypto = state
        return true
    end
    return false, "uncorrect global password"
end

function lib.isPassword()
    return not not globalPermitsPassword
end

function lib.requirePassword()
    return false, "cancel"
end

local function customFsMethod(_, method, methodName, ...)
    local tbl = {...}
    if readonlyEnable then
        if methodName == "open" then
            if (tbl[2]:sub(1, 1) == "w" or tbl[2]:sub(1, 1) == "a") and lib.isReadOnly(tbl[1]) then return nil, "file is readonly" end
        elseif methodName == "copy" then
            if lib.isReadOnly(tbl[2]) then return nil, "file is readonly" end
        elseif methodName == "rename" then
            if lib.isReadOnly(tbl[1]) or lib.isReadOnly(tbl[2]) then return nil, "file is readonly" end
        elseif methodName == "remove" then
            --if tbl[1] == "" or tbl[1] == "/" or tbl[1] == "." or tbl[1] == ".." then return nil, "format canceled" end
            if lib.isReadOnly(tbl[1]) then return nil, "file is readonly" end
        end
    end
    return method(...)
end

local function customEepromMethod(_, method, methodName, ...)
    local tbl = {...}
    if eepromCrypto then
        if methodName == "set" then
            return nil, "storage is readonly"
        elseif methodName == "get" then
            return nil, "dump is not allow"
        elseif methodName == "setData" then
            return nil, "eeprom-data readonly"
        elseif methodName == "getData" then
            return nil, "data dump is not allow"
        elseif methodName == "getChecksum" then
            return nil, "getChecksum is not allow"
        elseif methodName == "makeReadonly" then
            return nil, "makeReadonly is not allow"
        elseif methodName == "setLabel" then
            return nil, "label is readonly"
        elseif methodName == "getLabel" then
            return nil, "getLabel is not allow"
        end
    end
    return method(...)
end

if not _G.recoveryMod and false then --disabled
    local address = computer.getBootAddress()
    lib.addFilterMethod(address, "open", customFsMethod)
    lib.addFilterMethod(address, "copy", customFsMethod)
    lib.addFilterMethod(address, "rename", customFsMethod)
    lib.addFilterMethod(address, "remove", customFsMethod)

    local eepromAddress = component.list("eeprom")()
    if eepromAddress then
        lib.addFilterMethod(eepromAddress, "setLabel", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "getLabel", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "set", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "get", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "setData", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "getData", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "getChecksum", customEepromMethod)
        lib.addFilterMethod(eepromAddress, "makeReadonly", customEepromMethod)
    end
end

local newlib = tprotect.protect(lib)
return newlib