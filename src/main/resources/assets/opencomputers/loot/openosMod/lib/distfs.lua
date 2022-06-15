local event = require("event")
local bigchat = require("bigchat")
local fs = require("filesystem")

local appkey = "distfs"

local function create(index, folder, readonly)
    local appindex = index
    local readonly = readonly or false
    local proxyFS = require("proxyFS").createFS(folder, index)

    local files = {}

    local function backresult(...)
        bigchat.send(appkey, appindex, "return", ...)
    end

    local function nup()
        bigchat.send(appkey, appindex, "nup")
    end

    local function readonlysend()
        bigchat.send(appkey, appindex, "readonly")
    end

    local function listen(eventName, key, index, command, data, data2, data3, data4)
        if type(key) == "string" and type(index) == "string" and type(command) == "string" then
            if eventName == "big_chat" and key == appkey and index == appindex then
                if command == "open" then
                    if readonly and data2:sub(1, 1) == "w" then
                        readonlysend()
                    else
                        local file = proxyFS.open(data, data2)
                        files[#files + 1] = file
                        backresult(#files)
                    end
                elseif command == "close" then
                    local file = files[data]
                    local out
                    if file then
                        out = file:close(data2)
                    end
                    backresult(out)
                elseif command == "read" then
                    local file = files[data]
                    local out
                    if file then
                        out = file:read(data2)
                    end
                    backresult(out)
                elseif command == "write" then
                    if readonly then 
                        readonlysend()
                    else
                        local file = files[data]
                        local out
                        if file then
                            out = file:write(data2)
                        end
                        backresult(out)
                    end
                elseif command == "seek" then
                    local file = files[data]
                    local out
                    if file then
                        if data2 then
                            out = file:seek(data2, data3, data4)
                        else
                            file:seek("cur", 1)
                            out = file:seek("cur", -1)
                        end
                    end
                    backresult(out)
                elseif command == "rename" then
                    if readonly then 
                        readonlysend()
                    else
                        backresult(proxyFS.rename(data, data2))
                    end
                elseif command == "remove" then
                    if readonly then 
                        readonlysend()
                    else
                        backresult(proxyFS.remove(data, data2))
                    end
                elseif command == "exists" then
                    backresult(proxyFS.exists(data))
                elseif command == "isDirectory" then
                    backresult(proxyFS.isDirectory(data))
                elseif command == "getLabel" then
                    backresult("disk")
                elseif command == "setLabel" then
                    nup()
                elseif command == "isReadOnly" then
                    backresult(readonly)
                elseif command == "lastModified" then
                    backresult(proxyFS.lastModified(data))
                elseif command == "list" then
                    local out = {}
                    for var in proxyFS.list(data) do
                        out[#out + 1] = var
                    end
                    backresult(table.concat(out, "\n"))
                elseif command == "makeDirectory" then
                    if readonly then 
                        readonlysend()
                    else
                        backresult(proxyFS.makeDirectory(data))
                    end
                elseif command == "size" then
                    backresult(proxyFS.size(data))
                elseif command == "spaceTotal" then
                    backresult(proxyFS.spaceTotal())
                elseif command == "spaceUsed" then
                    backresult(proxyFS.spaceUsed())
                end
            end
        end
    end
    event.listen("big_chat", listen)

    return {kill = function() event.ignore("big_chat", listen) end}
end
local function connect(index, folder)
    local appindex = index
    local mount = folder

    ----------------------------------------------

    local files = {}

    local function upresult(...)
        bigchat.send(appkey, appindex, ...)
    end

    local function getResult()
        local eventName, key, index, command, data, data2, data3 = event.pull(4, "big_chat", appkey, appindex)
        if eventName == "big_chat" and key == appkey and index == appindex then
            if command == "return" or "nup" then
                return data, data2, data3
            end
        end
        if command == "readonly" then
            error("filesystem is readonly")
        else
            error("no connection")
        end
    end

    ----------------------------------------------

    local functions = {"list", "open", "close", "read", "write", "seek", "rename", "remove", "exists", "isDirectory", "getLabel", "setLabel", "isReadOnly", "lastModified", "makeDirectory", "size", "spaceTotal", "spaceUsed"}

    local proxy = {}
    for _, value in pairs(functions) do
        if value == "list" then
            proxy[value] = function(...)
                upresult(value, ...)
                local out = getResult()
                tab = {}
                for data in out:gmatch("[^\n]+") do
                    tab[#tab + 1] = data
                end
                return tab
            end
        else
            proxy[value] = function(...) upresult(value, ...) return getResult() end
        end
    end

    fs.mount(proxy, mount)
end
return {create = create, connect = connect}