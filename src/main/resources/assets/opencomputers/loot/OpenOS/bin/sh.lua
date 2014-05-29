local component = require("component")
local computer = require("computer")
local event = require("event")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")

local function expand(value)
  return value:gsub("%$(%w+)", os.getenv):gsub("%$%b{}",
    function(match) return os.getenv(expand(match:sub(3, -2))) or match end)
end

local function evaluate(value)
  local init, result = 1, ""
  repeat
    local match = value:match("^%b''", init)
    if match then -- single quoted string. no variable expansion.
      match = match:sub(2, -2)
      init = init + 2
      result = result .. match
    else
      match = value:match('^%b""', init)
      if match then -- double quoted string.
        match = match:sub(2, -2)
        init = init + 2
      else
        -- plaintext?
        match = value:match("^([^']+)%b''", init)
        if not match then -- unmatched single quote.
          match = value:match('^([^"]+)%b""', init)
          if not match then -- unmatched double quote.
            match = value:sub(init)
          end
        end
      end
      result = result .. expand(match)
    end
    init = init + #match
  until init > #value
  return result
end

local function execute(command, ...)
  local parts, reason = text.tokenize(command)
  if not parts then
    return false, reason
  elseif #parts == 0 then
    return true
  end
  local program, args = shell.resolveAlias(parts[1], table.pack(select(2, table.unpack(parts))))
  program = evaluate(program)
  local program, reason = shell.resolve(program, "lua")
  if not program then
    return false, reason
  end
  for i = 1, args.n do
    args[i] = evaluate(args[i])
  end
  for _, arg in ipairs(table.pack(...)) do
    table.insert(args, arg)
  end
  table.insert(args, 1, true)
  args.n = #args
  local thread, reason = process.load(shell.resolve(program, "lua"), env, nil, command)
  if not thread then
    return false, reason
  end
  local result = nil
  -- Emulate CC behavior by making yields a filtered event.pull()
  while args[1] and coroutine.status(thread) ~= "dead" do
    result = table.pack(coroutine.resume(thread, table.unpack(args, 2, args.n)))
    if coroutine.status(thread) ~= "dead" then
      if type(result[2]) == "string" then
        args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
      else
        args = {true, n=1}
      end
    end
  end
  if not args[1] then
    return false, args[2]
  end
  if not result[1] and type(result[2]) == "table" and result[2].reason == "terminated" then
    if result[2].code then
      return true
    else
      return false, "terminated"
    end
  end
  return table.unpack(result, 1, result.n)
end

local args, options = shell.parse(...)
local history = {}

if #args == 0 and (io.input() == io.stdin or options.i) and not options.c then
  -- interactive shell.
  while true do
    if not term.isAvailable() then -- don't clear unless we lost the term
      while not term.isAvailable() do
        event.pull("term_available")
      end
      term.clear()
    end
    while term.isAvailable() do
      local foreground, palette = component.gpu.setForeground(0xFF0000)
      term.write(expand(os.getenv("PS1") or "$ "))
      component.gpu.setForeground(foreground, palette)
      local command = term.read(history)
      if not command then
        term.write("exit\n")
        return -- eof
      end
      while #history > 10 do
        table.remove(history, 1)
      end
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = execute(command)
        if term.getCursor() > 1 then
          term.write("\n")
        end
        if not result then
          io.stderr:write((tostring(reason) or "unknown error").. "\n")
        end
      end
    end
  end
else
  -- execute command.
  local result = table.pack(execute(...))
  if not result[1] then
    error(result[2], 0)
  end
  return table.unpack(result, 2)
end