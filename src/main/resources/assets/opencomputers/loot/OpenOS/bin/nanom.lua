local event = require("event")
local component = require("component")
local term = require("term")

local modem = component.modem
modem.open(1)
modem.broadcast(1, "nanomachines", "setResponsePort", 1)

local lastResponse = ""
local function printResponse()
  local w, h = component.gpu.getResolution()
  component.gpu.fill(1, h, w, h, " ")
  component.gpu.set(1, h, lastResponse)
end
local function handleModemMessage(_, _, _, _, _, header, command, ...)
  if header ~= "nanomachines" then return end
  lastResponse = "Last response: " .. command
  for _, v in ipairs({...}) do
    lastResponse = lastResponse .. ", " .. tostring(v)
  end
  printResponse()
end

event.listen("modem_message", handleModemMessage)

local function send(command, ...)
  component.modem.broadcast(1, "nanomachines", command, ...)
end

local function readNumber(name, validator)
  local index
  while not index do
    io.write(name..": ")
    index = tonumber(io.read())
    if not index or validator and not validator(index) then
      index = nil
      io.write("invalid input\n")
    end
  end
  return index
end

local running = true
local commands = {
  { "Get power state",
    function()
      send("getPowerState")
    end
  },

  { "Get active effects",
    function()
      send("getActiveEffects")
    end
  },
  { "Get input",
    function()
      local index = readNumber("index")
      send("getInput", index)
    end
  },
  { "Set input",
    function()
      local index = readNumber("index")
      io.write("1. On\n")
      io.write("2. Off\n")
      local value = readNumber("state", function(x) return x == 1 or x == 2 end)
      send("setInput", index, value == 1)
    end
  },
  { "Get total input count",
    function()
      send("getTotalInputCount")
    end
  },
  { "Get safe active input count",
    function()
      send("getSafeActiveInputs")
    end
  },
  { "Get max active input count",
    function()
      send("getMaxActiveInputs")
    end
  },

  { "Get health",
    function()
      send("getHealth")
    end
  },
  { "Get hunger",
    function()
      send("getHunger")
    end
  },
  { "Get age",
    function()
      send("getAge")
    end
  },
  { "Get name",
    function()
      send("getName")
    end
  },
  { "Get experience",
    function()
      send("getExperience")
    end
  },

  { "Exit",
    function()
      running = false
    end
  }
}

function main()
  while running do
    term.clear()
    for i = 1, #commands do
      local command = commands[i]
      io.write(i,". ",command[1],"\n")
    end
    printResponse()

    local command = readNumber("command", function(x) return x > 0 and x <= #commands end)
    commands[command][2]()
  end
end

local result, reason = pcall(main)
if not result then
  io.stderr:write(reason, "\n")
end

event.ignore("modem_message", handleModemMessage)
term.clear()
