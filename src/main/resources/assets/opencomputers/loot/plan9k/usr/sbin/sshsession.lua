local event = require "event"
local network = require "network"
local pipes = require "pipes"
local component = require "component"

local args = {...}
local chanel = args[1]

local AUTH = 0
local ENCRYPTED = 1
local TEXT = 2
local AUTHOK = 3

local passwdf = io.open("/etc/passwd","r")
local pwHmac, pwHash = string.unpack("c16c32", component.data.decode64(passwdf:read("*a")))
passwdf:close()

local pty, mi, mo, si, so = pipes.openPty()

local shpid

local chellengeKey = component.data.random(16)
local authResponse = component.data.sha256(pwHash, chellengeKey)
local key = authResponse:sub(1,16)
local handlers = {}

local function encryptedSend(data)
    local iv = component.data.random(16)
    network.tcp.send(chanel, string.pack("Bc16", ENCRYPTED, iv) .. component.data.encrypt(data, key, iv))
end

handlers[AUTH] = function(data)
    if data == authResponse then
        shpid = os.spawnp("/bin/sh.lua", si, so, so)
        encryptedSend(string.pack("B", AUTHOK))
    else
        network.tcp.close(chanel)
        os.exit()
    end
end

handlers[ENCRYPTED] = function(data)
    local iv, rs = string.unpack("c16", data)
    local d = component.data.decrypt(data:sub(rs), key, iv)
    if not d then
        network.tcp.close(chanel)
        os.exit()
    end
    local ptype, n = string.unpack("B", d)
    handlers[ptype](d:sub(n))
end

handlers[TEXT] = function(data)
    mo:write(data)
    os.sleep(0)
end

network.tcp.send(chanel, string.pack("Bc16c16", AUTH, pwHmac, chellengeKey))

local function handleTcp()
    while true do
        while mi.remaining() > 0 do
            local data = mi:read(math.min(mi.remaining(), 7000))
            encryptedSend(string.pack("B", TEXT) .. data)
        end
        local e = {event.pull()} --Note: if some text arrives it may require some poke to react
        if e[1] then
            if e[1] == "tcp" then
                if e[2] == "message" and e[3] == chanel then
                    local ptype, n = string.unpack("B", e[4])
                    if not handlers[ptype] then
                        network.tcp.close(chanel)
                        os.exit()
                    end
                    handlers[ptype](e[4]:sub(n))
                elseif e[2] == "close" and e[3] == chanel then
                    return
                end
            elseif e[1] == "kill" and e[2] == shpid then
                network.tcp.close(chanel)
                return
            end
        end
    end
end

handleTcp()
