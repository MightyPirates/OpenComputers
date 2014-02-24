-- A (very (very!)) simple IRC client. Reference:
-- http://tools.ietf.org/html/rfc2812

local component = require("component")
local event = require("event")
local internet = require("internet")
local shell = require("shell")
local term = require("term")
local text = require("text")

local args, options = shell.parse(...)
if #args < 1 then
  print("Usage: irc nickname [server:port]")
  return
end

local nick = args[1]
local host = args[2] or "irc.esper.net:6667"

-- try to connect to server.
local sock, reason = internet.open(host)
if not sock then
  io.stderr:write(reason .. "\n")
  return
end

-- custom print that uses all except the last line for printing.
local function print(message, overwrite)
  local w, h = component.gpu.getResolution()
  local line
  repeat
    line, message = text.wrap(text.trim(message), w, w)
    if not overwrite then
      component.gpu.copy(1, 1, w, h - 1, 0, -1)
    end
    overwrite = false
    component.gpu.fill(1, h - 1, w, 1, " ")
    component.gpu.set(1, h - 1, line)
  until not message or message == ""
end

-- utility method for reply tracking tables.
function autocreate(table, key)
  table[key] = {}
  return table[key]
end

-- extract nickname from identity.
local function name(identity)
  return identity and identity:match("^[^!]+") or identity or "Anonymous"
end

-- user defined callback for messages (via `lua function(msg) ... end`)
local callback = nil

-- list of whois info per user (used to accumulate whois replies).
local whois = setmetatable({}, {__index=autocreate})

-- list of users per channel (used to accumulate names replies).
local names = setmetatable({}, {__index=autocreate})

-- timer used to drive socket reading.
local timer

-- ignored commands, reserved according to RFC.
-- http://tools.ietf.org/html/rfc2812#section-5.3
local ignore = {
  [213]=true, [214]=true, [215]=true, [216]=true, [217]=true,
  [218]=true, [231]=true, [232]=true, [233]=true, [240]=true,
  [241]=true, [244]=true, [244]=true, [246]=true, [247]=true,
  [250]=true, [300]=true, [316]=true, [361]=true, [362]=true,
  [363]=true, [373]=true, [384]=true, [492]=true,
  -- custom ignored responses.
  [265]=true, [266]=true, [330]=true
}

-- command numbers to names.
local commands = {
  RPL_WELCOME = "001",
  RPL_YOURHOST = "002",
  RPL_CREATED = "003",
  RPL_MYINFO = "004",
  RPL_BOUNCE = "005",
  RPL_LUSERCLIENT = "251",
  RPL_LUSEROP = "252",
  RPL_LUSERUNKNOWN = "253",
  RPL_LUSERCHANNELS = "254",
  RPL_LUSERME = "255",
  RPL_AWAY = "301",
  RPL_UNAWAY = "305",
  RPL_NOWAWAY = "306",
  RPL_WHOISUSER = "311",
  RPL_WHOISSERVER = "312",
  RPL_WHOISOPERATOR = "313",
  RPL_WHOISIDLE = "317",
  RPL_ENDOFWHOIS = "318",
  RPL_WHOISCHANNELS = "319",
  RPL_CHANNELMODEIS = "324",
  RPL_NOTOPIC = "331",
  RPL_TOPIC = "332",
  RPL_NAMREPLY = "353",
  RPL_ENDOFNAMES = "366",
  RPL_MOTDSTART = "375",
  RPL_MOTD = "372",
  RPL_ENDOFMOTD = "376"
}

-- main command handling callback.
local function handleCommand(prefix, command, args, message)
  ---------------------------------------------------
  -- Keepalive

  if command == "PING" then
    sock:write(string.format("PONG :%s\r\n", message))
    sock:flush()

  ---------------------------------------------------
  -- General commands

  elseif command == "NICK" then
    print(name(prefix) .. " is now known as " .. tostring(args[1] or message) .. ".")
  elseif command == "MODE" then
    print("[" .. args[1] .. "] Mode is now " .. tostring(args[2] or message) .. ".")
  elseif command == "QUIT" then
    print(name(prefix) .. " quit (" .. (message or "Quit") .. ").")
  elseif command == "JOIN" then
    print("[" .. args[1] .. "] " .. name(prefix) .. " entered the room.")
  elseif command == "PART" then
    print("[" .. args[1] .. "] " .. name(prefix) .. " has left the room (quit: " .. (message or "Quit") .. ").")
  elseif command == "TOPIC" then
    print("[" .. args[1] .. "] " .. name(prefix) .. " has changed the topic to: " .. message)
  elseif command == "KICK" then
    print("[" .. args[1] .. "] " .. name(prefix) .. " kicked " .. args[2])
  elseif command == "PRIVMSG" then
    print("[" .. args[1] .. "] " .. name(prefix) .. ": " .. message)
  elseif command == "NOTICE" then
    print("[NOTICE] " .. message)
  elseif command == "ERROR" then
    print("[ERROR] " .. message)

  ---------------------------------------------------
  -- Ignored reserved numbers
  -- -- http://tools.ietf.org/html/rfc2812#section-5.3

  elseif tonumber(command) and ignore[tonumber(command)] then
    -- ignore

  ---------------------------------------------------
  -- Command replies
  -- http://tools.ietf.org/html/rfc2812#section-5.1

  elseif command == commands.RPL_WELCOME then
    print(message)
  elseif command == commands.RPL_YOURHOST then -- ignore
  elseif command == commands.RPL_CREATED then -- ignore
  elseif command == commands.RPL_MYINFO then -- ignore
  elseif command == commands.RPL_BOUNCE then -- ignore
  elseif command == commands.RPL_LUSERCLIENT then
    print(message)
  elseif command == commands.RPL_LUSEROP then -- ignore
  elseif command == commands.RPL_LUSERUNKNOWN then -- ignore
  elseif command == commands.RPL_LUSERCHANNELS then -- ignore
  elseif command == commands.RPL_LUSERME then
    print(message)
  elseif command == commands.RPL_AWAY then
    print(string.format("%s is away: %s", name(args[1]), message))
  elseif command == commands.RPL_UNAWAY or command == commands.RPL_NOWAWAY then
    print(message)
  elseif command == commands.RPL_WHOISUSER then
    local nick = args[2]:lower()
    whois[nick].nick = args[2]
    whois[nick].user = args[3]
    whois[nick].host = args[4]
    whois[nick].realName = message
  elseif command == commands.RPL_WHOISSERVER then
    local nick = args[2]:lower()
    whois[nick].server = args[3]
    whois[nick].serverInfo = message
  elseif command == commands.RPL_WHOISOPERATOR then
    local nick = args[2]:lower()
    whois[nick].isOperator = true
  elseif command == commands.RPL_WHOISIDLE then
    local nick = args[2]:lower()
    whois[nick].idle = tonumber(args[3])
  elseif command == commands.RPL_ENDOFWHOIS then
    local nick = args[2]:lower()
    local info = whois[nick]
    print("Nick: " .. info.nick)
    print("User name: " .. info.user)
    print("Real name: " .. info.realName)
    print("Host: " .. info.host)
    print("Server: " .. info.server .. " (" .. info.serverInfo .. ")")
    print("Channels: " .. info.channels)
    print("Idle for: " .. info.idle)
    whois[nick] = nil
  elseif command == commands.RPL_WHOISCHANNELS then
    local nick = args[2]:lower()
    whois[nick].channels = message
  elseif command == commands.RPL_CHANNELMODEIS then
    print("Channel mode for " .. args[1] .. ": " .. args[2] .. " (" .. args[3] .. ")")
  elseif command == commands.RPL_NOTOPIC then
    print("No topic is set for " .. args[1] .. ".")
  elseif command == commands.RPL_TOPIC then
    print("Topic for " .. args[1] .. ": " .. message)
  elseif command == commands.RPL_NAMREPLY then
    local channel = args[3]
    table.insert(names[channel], message)
  elseif command == commands.RPL_ENDOFNAMES then
    local channel = args[2]
    print("Users on " .. channel .. ": " .. (#names[channel] > 0 and table.concat(names[channel], " ") or "none"))
    names[channel] = nil
  elseif command == commands.RPL_MOTDSTART then
    if options.motd then
      print(message .. args[1])
    end
  elseif command == commands.RPL_MOTD then
    if options.motd then
      print(message)
    end
  elseif command == commands.RPL_ENDOFMOTD then -- ignore
  elseif tonumber(command) and (tonumber(command) >= 200 and tonumber(command) < 400) then
    print("[Response " .. command .. "] " .. table.concat(args, ", ") .. ": " .. message)

  ---------------------------------------------------
  -- Error messages. No real point in handling those manually.
  -- http://tools.ietf.org/html/rfc2812#section-5.2

  elseif tonumber(command) and (tonumber(command) >= 400 and tonumber(command) < 600) then
    print("[Error] " .. table.concat(args, ", ") .. ": " .. message)

  ---------------------------------------------------
  -- Unhandled message.

  else
    print("Unhandled command: " .. command)
  end
end

-- catch errors to allow manual closing of socket and removal of timer.
local result, reason = pcall(function()
  -- say hello.
  term.clear()
  print("Welcome to OpenIRC!")

  -- avoid sock:read locking up the computer.
  sock:setTimeout(0.05)

  -- http://tools.ietf.org/html/rfc2812#section-3.1
  sock:write(string.format("NICK %s\r\n", nick))
  sock:write(string.format("USER %s 0 * :%s [OpenComputers]\r\n", nick:lower(), nick))
  sock:flush()

  -- socket reading logic (receive messages) driven by a timer.
  timer = event.timer(0.5, function()
    if not sock then
      return false
    end
    repeat
      local ok, line = pcall(sock.read, sock)
      if ok then
        if not line then
          print("Connection lost.")
          sock:close()
          sock = nil
          return false
        end
        line = text.trim(line) -- get rid of trailing \r
        local match, prefix = line:match("^(:(%S+) )")
        if match then line = line:sub(#match + 1) end
        local match, command = line:match("^(([^:]%S*))")
        if match then line = line:sub(#match + 1) end
        local args = {}
        repeat
          local match, arg = line:match("^( ([^:]%S*))")
          if match then
            line = line:sub(#match + 1)
            table.insert(args, arg)
          end
        until not match
        local message = line:match("^ :(.*)$")

        if callback then
          local result, reason = pcall(callback, prefix, command, args, message)
          if not result then
            print("Error in callback: " .. tostring(reason))
          end
        end
        handleCommand(prefix, command, args, message)
      end
    until not ok
  end, math.huge)

  -- default target for messages, so we don't have to type /msg all the time.
  local target = nil

  -- command history.
  local history = {}

  repeat
    local w, h = component.gpu.getResolution()
    term.setCursor(1, h)
    term.write((target or "?") .. "> ")
    local line = term.read(history)
    if sock and line and line ~= "" then
      line = text.trim(line)
      print("[" .. (target or "?") .. "] me: " .. line, true)
      if line:lower():sub(1, 5) == "/msg " then
        local user, message = line:sub(6):match("^(%S+) (.+)$")
        message = text.trim(message)
        if not user or not message or message == "" then
          print("Invalid use of /msg. Usage: /msg nick|channel message.")
          line = ""
        else
          target = user
          line = "PRIVMSG " .. target .. " :" .. message
        end
      elseif line:lower():sub(1, 6) == "/join " then
        local channel = text.trim(line:sub(7))
        if not channel or channel == "" then
          print("Invalid use of /join. Usage: /join channel.")
          line = ""
        else
          target = channel
          line = "JOIN " .. channel
        end
      elseif line:lower():sub(1, 5) == "/lua " then
        local script = text.trim(line:sub(6))
        local result, reason = load(script, "=stdin", setmetatable({print=print, socket=sock}, {__index=_G}))
        if not result then
          result, reason = load("return " .. script, "=stdin", setmetatable({print=print, socket=sock}, {__index=_G}))
        end
        line = ""
        if not result then
          print("Error: " .. tostring(reason))
        else
          result, reason = pcall(result)
          if not result then
            print("Error: " .. tostring(reason))
          elseif type(reason) == "function" then
            callback = reason
          else
            line = tostring(reason)
          end
        end
      elseif line:sub(1, 1) == "/" then
        line = line:sub(2)
      elseif line ~= "" then
        if not target then
          print("No default target set. Use /msg or /join to set one.")
          line = ""
        else
          line = "PRIVMSG " .. target .. " :" .. line
        end
      end
      if line and line ~= "" then
        sock:write(line .. "\r\n")
        sock:flush()
      end
    end
  until not sock or not line
end)

if sock then
  sock:write("QUIT\r\n")
  sock:close()
end
if timer then
  event.cancel(timer)
end

if not result then
  error(reason, 0)
end
return reason