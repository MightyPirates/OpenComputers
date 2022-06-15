local su = require("superUtiles")
local computer = require("computer")

-----------------------------------

return {
    create = function(protection)
        local env = {
            math = su.simpleTableClone(math),
            string = su.simpleTableClone(string),
            table = su.simpleTableClone(table),

            _VERSION = _VERSION,

            type = type,
            tonumber = tonumber,
            tostring = tostring,
            ipairs = ipairs,
            pairs = pairs,
            pcall = pcall,
            xpcall = xpcall,
            error = error,
            assert = assert,
            checkArg = checkArg,
            utf8 = su.simpleTableClone(utf8),
            getmetatable = getmetatable,
            setmetatable = setmetatable,
            select = select,
            next = next,
            rawequal = rawequal,
            rawget = rawget,
            rawlen = rawlen,
            rawset = rawset
        }
        env._G = env

        env.load = function(data, name, mode, lenv)
            if not lenv then lenv = env end --для коректной таблицы env
            return load(data, name, mode, lenv)
        end

        if protection then
            env.pcall = function(...)
                local ret = {pcall(...)}
                protection() --зашита от краша
                return table.unpack(ret)
            end
            env.xpcall = function(...)
                local ret = {xpcall(...)}
                protection() --зашита от краша
                return table.unpack(ret)
            end
        end

        return env
    end
}