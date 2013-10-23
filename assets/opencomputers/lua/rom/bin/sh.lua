local args = table.pack(...)
if args.n > 0 then
  os.execute(table.concat(args, " ", 1, args.n))
  return
end

local function trim(s) -- from http://lua-users.org/wiki/StringTrim
  local from = s:match"^%s*()"
  return from > #s and "" or s:match(".*%S", from)
end
local dir = shell.cwd() -- backup in case we're being run by another shell
local running = true

while running do
  if not term.isAvailable() then -- don't clear when opened by another shell
    while not term.isAvailable() do
      event.wait()
    end
    term.clear()
    print("OpenOS v1.0 (" .. math.floor(os.totalMemory() / 1024) .. "k RAM)")
  end
  while running and term.isAvailable() do
    io.write("> ")
    local command = io.read()
    if not command then
      return -- eof
    end
    command = trim(command)
    if command == "exit" then
      running = false
    elseif command ~= "" then
      local result, reason = os.execute(command)
      if not result then
        print(reason)
      end
    end
  end
end

shell.cwd(dir) -- restore
