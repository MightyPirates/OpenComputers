local fs = require("filesystem")
local su = require("superUtiles")
local sh = require("sh")
local term = require("term")

--------------------------------------------

local lib = {}

function lib.check(path, env, ...)
    local args = {...}
    local data, err = su.getFile(path)
    if not data then return nil, err end
    if data:find("ECS") or data:find("ecs") or data:find("mineOS") then
        return nil, "ECS virus, eeprom writer, computer blocker"
    end
    return true
end

function lib.execute(path, env, ...)
    if not env then
        env = su.createEnv()
    end

    local data, err = su.getFile(path)
    if not data then return nil, err end
    local ok, errCheck = lib.check(path, env, ...)
    if not ok then
        print("программа потанциально небезопастная отчет антивируса", errCheck)
        print("50 раз подумайте перед ее запуском")
        print("вы уверены запустить? [Y/n]")
        local str = term.read()
        if str ~= "y\n" and str ~= "Y\n" then
            return nil, "user canceled"
        end
    end

    return sh.execute(env, path, ...)
end

return lib