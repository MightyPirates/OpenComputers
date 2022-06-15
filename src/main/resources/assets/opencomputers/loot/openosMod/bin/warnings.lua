local fs = require("filesystem")
local component = require("component")
local shell = require("shell")
local computer = require("computer")
local su = require("superUtiles")

----------------------------------

local args, options = shell.parse(...)

local function componentCount(filter)
    local count = 0
    for address in component.list(filter) do
        count = count + 1
    end
    return count
end

----------------------------------

local warnings = {}

if fs.isAutorunEnabled() then
    table.insert(warnings, "внешняя автозагрузка включена, это очень не безопастно и любой наситель сможет причинить устройству вред")
end

if componentCount("internet") > 1 then
    table.insert(warnings, "нет необходимости в устоновки нескольких интернет плат")
end

if componentCount("gpu") == 0 then
    table.insert(warnings, "нет видео карты, некоторый программы могут не рабоать")
end

if componentCount("screen") == 0 then
    table.insert(warnings, "нет монитора, некоторый программы могут не рабоать")
end

local hddFreeMem = fs.get("/").spaceTotal() - fs.get("/").spaceUsed()
if hddFreeMem < (1024 * 512) then
    table.insert(warnings, "в хранилише доступно мение 512мб(" .. tostring(hddFreeMem // 1024) .. "мб)")
end

if computer.totalMemory() < (1024 * 512) then
    table.insert(warnings, "всего оперативной памяти мение 512мб рекомендуеться добавить еще")
end

if computer.freeMemory() < (1024 * 128) then
    table.insert(warnings, "свободной оперативной памяти мение 128мб(" .. tostring(computer.freeMemory() // 1024) .. "мб) это может привести к сбою")
end

local data = su.getFile("/free/data/powerWarning")
if data then
    table.insert(warnings, "этот компьютер " .. tostring(data) .. " раз завершил свою работу аварийно(не програмно)")
end

----------------------------------

if not options.q then
    for i = 1, #warnings do
        print(warnings[i])
    end
end

----------------------------------

return warnings