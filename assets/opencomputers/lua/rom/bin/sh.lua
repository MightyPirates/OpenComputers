local history = {}
while true do
  if not term.isAvailable() then -- don't clear when opened by another shell
    while not term.isAvailable() do
      os.sleep()
    end
    term.clear()
    print("OpenOS v1.0 (" .. math.floor(os.totalMemory() / 1024) .. "k RAM)")
  end
  while term.isAvailable() do
    term.write("# ")
    local command = term.read(history)
    if not command then
      return -- eof
    end
    while #history > 10 do
      table.remove(history, 1)
    end
    command = string.trim(command)
    if command == "exit" then
      return
    elseif command ~= "" then
      local result, reason = os.execute(command)
      if not result then
        print(reason)
      end
    end
  end
end
