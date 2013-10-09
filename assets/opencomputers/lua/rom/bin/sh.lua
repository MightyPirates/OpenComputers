local function onTermAvailable()
  term.clear()
  print("OpenOS v1.0 (" .. math.floor(os.totalMemory() / 1024) .. "k RAM)")
  while term.isAvailable() do
    io.write("> ")
    local command = io.read()
    if not command then
      return -- eof
    end
    local code, result = load("return " .. command, "=stdin")
    if not code then
      code, result = load(command, "=stdin") -- maybe it's a statement
    end
    if code then
      local result = table.pack(pcall(code))
      if not result[1] or result.n > 1 then
        print(table.unpack(result, 2, result.n))
      end
    else
      print(result)
    end
  end
end

event.listen("term_available", onTermAvailable)
