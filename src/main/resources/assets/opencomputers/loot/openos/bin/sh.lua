local event = require("event")
local shell = require("shell")
local term = require("term")
local text = require("text")
local sh = require("sh")

local input = table.pack(...)
local args, options = shell.parse(select(3,table.unpack(input)))
if input[2] then
  table.insert(args, 1, input[2])
end

local history = {}
shell.prime()

if #args == 0 and (io.stdin.tty or options.i) and not options.c then
  -- interactive shell.
  -- source profile
  if not term.isAvailable() then event.pull("term_available") end
  loadfile(shell.resolve("source","lua"))("/etc/profile")
  while true do
    if not term.isAvailable() then -- don't clear unless we lost the term
      while not term.isAvailable() do
        event.pull("term_available")
      end
      term.clear()
    end
    local gpu = term.gpu()
    while term.isAvailable() do
      local foreground = gpu.setForeground(0xFF0000)
      term.write(sh.expand(os.getenv("PS1") or "$ "))
      gpu.setForeground(foreground)
      term.setCursorBlink(true)
      local ok, command = pcall(term.read, history, nil, sh.hintHandler)
      if not ok then
        if command == "interrupted" then -- hard interrupt
          io.write("^C\n")
          break
        elseif not term.isAvailable() then
          break
        else -- crash?
          io.stderr:write("\nshell crashed: " .. tostring(command) .. "\n")
          break
        end
      end
      if not command then
        if command == false then
          break -- soft interrupt
        end
        io.write("exit\n") -- pipe closed
        return -- eof
      end
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = os.execute(command)
        if term.getCursor() > 1 then
          print()
        end
        if not result then
          io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
        end
      end
    end
  end
elseif #args == 0 and not io.stdin.tty then
  while true do
    io.write(sh.expand(os.getenv("PS1") or "$ "))
    local command = io.read("*l")
    if not command then
      command = "exit"
      io.write(command,"\n")
    end
    command = text.trim(command)
    if command == "exit" then
      return
    elseif command ~= "" then
      local result, reason = os.execute(command)
      if not result then
        io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
      end
    end
  end
else
  -- execute command.
  local result = table.pack(sh.execute(...))
  if not result[1] then
    error(result[2], 0)
  end
  return table.unpack(result, 2)
end
