local bigchat = require("bigchat")
local event = require("event")

-----------------------------------

local lib = {}

function lib.create(name, func)
    local obj = {}
    obj.func = func
    obj.name = name
    obj.listen = function(_, message_type, func_name, ...)
        if message_type == "bizicall_call" and func_name == obj.name then
            bigchat.send("bizicall_return", obj.name, pcall(obj.func, ...))
        end
    end
    event.listen("big_chat", obj.listen)
    obj.kill = function() event.ignore("big_chat", obj.listen) end

    return obj
end

function lib.call(name, ...)
    bigchat.send("bizicall_call", name, ...)
    local out = {event.pull(5, "big_chat", "bizicall_return", name)}
    if not out[1] then error("no connection") end
    if out[4] then
        return table.unpack(out, 5, #out)
    end
    error(out[5] or "unkown")
end

return lib