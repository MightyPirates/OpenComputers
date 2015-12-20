local event = require "event"
local network = require "network"
local component = require "component"
local shell = require "shell"
local term = require "term"

local network = require "network"
local event = require "event"

local args = {...}

local AUTH = 0
local ENCRYPTED = 1
local TEXT = 2
local AUTHOK = 3

local port = 22
local addr

for _,par in ipairs(args) do
    if not addr then
        addr = par
    end
end

if not addr then
    print("Usage: ssh [address]")
    return
end

io.write("Password: ")
local pass = term.read(nil, nil, nil, "*")

local chanel
local key
local handlers = {}
local ready = false


local function encryptedSend(data)
    local iv = component.data.random(16)
    network.tcp.send(chanel, string.pack("Bc16", ENCRYPTED, iv) .. component.data.encrypt(data, key, iv))
end

handlers[AUTH] = function(data)
    local hmacKey, chellengeKey = string.unpack("c16c16", data)
    local response = component.data.sha256(component.data.sha256(pass, hmacKey), chellengeKey)
    key = response:sub(1, 16)
    encryptedSend(string.pack("Bc32", AUTH, response))
    print("Authorize")
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
    io.write(data)
end

handlers[AUTHOK] = function(data)
    io.write("AUTH OK\n")
    ready = true
end

local eof = false
local function handleTcp()
    while true do
        while (io.input().remaining and io.input().remaining() ~= 0 or (not eof and not io.input().remaining)) and ready do
            local toread = 7000
            if io.input().remaining then toread = io.input().remaining() end
            local data = io.read(math.min(toread or 7000, 7000))
            if not data then
                eof = true
            else
                encryptedSend(string.pack("B", TEXT) .. data)
            end
        end
        local e = {event.pull()}
        if e[1] then
            if e[1] == "tcp" then
                if e[2] == "connection" then
                    if e[3] == chanel and e[6] ~= "incoming" then
                        chanel = e[3]
                    end
                elseif e[2] == "close" and e[3] == chanel then
                    return
                elseif e[2] == "message" and e[3] == chanel then
                    local ptype, n = string.unpack("B", e[4])
                    if not handlers[ptype] then
                        network.tcp.close(chanel)
                        os.exit()
                    end
                    handlers[ptype](e[4]:sub(n))
                end
            elseif e[1] == "interrupted" then
                --TODO relay interrupt
                network.tcp.close(chanel)
                os.exit()
            end
        end
    end
end

chanel = network.tcp.open(addr, port)
handleTcp()
