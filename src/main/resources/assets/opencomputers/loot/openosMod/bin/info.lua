local event = require("event")
local su = require("superUtiles")
local component = require("component")
local unicode = require("unicode")
local computer = require("computer")
local serialization = require("serialization")
local internet = component.isAvailable("internet") and component.internet
local shell = require("shell")
local fs = require("filesystem")

--------------------------------------------------

local args, options = shell.parse(...)
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

local inData, outData
if internet then
    outData = assert(serialization.unserialize(assert(getInternetFile(url .. versionPath))))
end
if fs.exists(versionPath) then
    inData = assert(serialization.unserialize(assert(su.getFile(versionPath))))
else
    inData = {version = 0}
end

--------------------------------------------------

if outData then
    print("информациа о обновлении ("..tostring(outData.version).."):")
    print(outData.info or "отсутствует")
end
print("информациа о устоновленной версии ("..tostring(inData.version).."):")
print(inData.info or "отсутствует")