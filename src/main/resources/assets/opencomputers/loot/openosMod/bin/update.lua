local event = require("event")
local su = require("superUtiles")
local component = require("component")
local unicode = require("unicode")
local computer = require("computer")
local serialization = require("serialization")
local shell = require("shell")
local fs = require("filesystem")
if not su.isInternet() then
    print("internet error")
    return
end
local internet = component.internet

--------------------------------------------------

local args = shell.parse(...)
local url = args[1] or systemCfg.updateRepo or "https://raw.githubusercontent.com/igorkll/openOSpath/main"
local versionPath = args[2] or systemCfg.updateVersionCfg or "/version.cfg"

--------------------------------------------------

local function getInternetFile(url)
    local handle, data, result, reason = internet.request(url), ""
    if handle then
        while true do
            result, reason = handle.read(math.huge) 
            if result then
                data = data .. result
            else
                handle.close()
                
                if reason then
                    return nil, reason
                else
                    return data
                end
            end
        end
    else
        return nil, "unvalid address"
    end
end

--------------------------------------------------

local inData
local outData = assert(serialization.unserialize(assert(getInternetFile(url .. versionPath))))
if fs.exists(versionPath) then
    inData = assert(serialization.unserialize(assert(su.getFile(versionPath))))
else
    inData = {version = 0}
end

--------------------------------------------------

if _G.getEnergyPercentages() < 50 then
    io.stderr:write("для обновления необходим заряд минимум 50% у вас " .. tostring(_G.getEnergyPercentages()) .. "%")
    return
end

--------------------------------------------------

if outData.version > inData.version then
    print("ваша версия "..tostring(inData.version)..", продолжив бедет устоновленна "..tostring(outData.version))
elseif outData.version == inData.version then
    print("у вас устоновленна актуальная версия ("..tostring(inData.version).."), обновления имеет смысл только если файлы поврежденны")
elseif outData.version < inData.version then
    print("ваша версия ("..tostring(inData.version)..") выше актуальной предусмотреной вышим перозиториям update ("..tostring(outData.version)..") продолжив обновления произойдет downgrade")
end

print("информациа о обновлении:")
print(outData.info or "отсутствует")
print("информациа о устоновленной версии:")
print(inData.info or "отсутствует")

--------------------------------------------------

print("продолжить? [Y/n]")
local read = io.read()

if read and (read == "Y" or read == "y") then
    --os.execute("wget https://raw.githubusercontent.com/igorkll/fastOS/main/getinstaller.lua /tmp/getinstaller.lua -f -Q")
    --os.execute("/tmp/getinstaller " .. url .. " / -q")
    os.execute("fastupdate -f -n " .. url .. " " .. versionPath)
    computer.shutdown("fast")
end