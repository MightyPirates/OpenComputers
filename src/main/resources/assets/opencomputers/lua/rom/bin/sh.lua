local component = require("component")
local computer = require("computer")
local event = require("event")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")

local args, options = shell.parse(...)
local history = {}

if options.v or not process.running(2) then
  io.write(_OSVERSION .. " (" .. math.floor(computer.totalMemory() / 1024) .. "k RAM)\n")
end

while true do
  if not term.isAvailable() then -- don't clear unless we lost the term
    while not term.isAvailable() do
      event.pull("term_available")
    end
    term.clear()
    if options.v then
      io.write(_OSVERSION .. " (" .. math.floor(computer.totalMemory() / 1024) .. "k RAM)\n")
    end
  end
  while term.isAvailable() do
    local foreground = component.gpu.setForeground(0xFF0000)
    term.write(os.getenv("PS1") or "# ")
    component.gpu.setForeground(foreground)
    local command = term.read(history)
    if not command then
      io.write("exit\n")
      return -- eof
    end
    while #history > 10 do
      table.remove(history, 1)
    end
    command = text.trim(command)
    if command == "exit" then
      return
    elseif command ~= "" then
      local result, reason = os.execute(command)
      if not result then
        io.stderr:write(reason .. "\n")
      elseif term.getCursor() > 1 then
        io.write("\n")
      end
    end
  end
end