local lastCommand, command = "", ""
local isRunning = false

-------------------------------------------------------------------------------

local function onKeyDown(_, address, char, code)
  if isRunning then return end -- ignore events while running a command
  if address ~= term.keyboard() then return end
  local _, gpu = term.gpu()
  if not gpu then return end
  local x, y = term.cursor()
  local keys = driver.keyboard.keys
  if code == keys.back then
    if command:len() == 0 then return end
    command = command:sub(1, -2)
    term.cursor(command:len() + 3, y) -- from leading "> "
    gpu.set(x - 1, y, "  ") -- overwrite cursor blink
  elseif code == keys.enter then
    if command:len() == 0 then return end
    term.cursorBlink(false)
    print()
    local code, result = load("return " .. command, "=stdin")
    if not code then
      code, result = load(command, "=stdin") -- maybe it's a statement
    end
    if code then
      isRunning = true
      local result = table.pack(pcall(code))
      isRunning = false
      if not result[1] or result.n > 1 then
        print(table.unpack(result, 2, result.n))
      end
    else
      print(result)
    end
    lastCommand = command
    command = ""
    term.write("> ")
    term.cursorBlink(true)
  elseif code == keys.up then
    command = lastCommand
    gpu.fill(3, y, screenWidth, 1, " ")
    term.cursor(3, y)
    term.write(command)
    term.cursor(command:len() + 3, y)
  elseif not keys.isControl(char) then
    char = string.char(char)
    command = command .. char
    term.write(char)
  end
end

local function onClipboard(_, address, value)
  if isRunning then return end
  if address ~= term.keyboard() then return end
  value = value:match("([^\r\n]+)")
  if value and value:len() > 0 then
    command = command .. value
    term.write(value)
  end
end

event.listen("term_available", function()
  term.clear()
  command = ""
  print("OpenOS v1.0 (" .. math.floor(os.totalMemory() / 1024) .. "k RAM)")
  term.write("> ")
  event.listen("key_down", onKeyDown)
  event.listen("clipboard", onClipboard)
end)

event.listen("term_unavailable", function()
  event.ignore("key_down", onKeyDown)
  event.ignore("clipboard", onClipboard)
end)

-------------------------------------------------------------------------------

term.cursorBlink(true)
