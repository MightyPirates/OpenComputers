local buffer = require("buffer")
local keyboard = require("keyboard")
local shell = require("shell")
local tty = require("tty")

local args = shell.parse(...)
if #args > 1 then
  io.write("Usage: more <filename>\n")
  io.write("- or no args reads stdin\n")
  return 1
end

local function clear_line()
  tty.window.x = 1 -- move cursor to start of line
  io.write("\27[2K") -- clear line
end

if io.output().tty then
  io.write("\27[2J\27[H")

  local intercept_active = true
  local original_stream = io.stdout.stream
  local custom_stream = setmetatable({
    scroll = function(...)
      local _, height, _, _, _, y = tty.getViewport()
      local lines_below = height - y
      if intercept_active and lines_below < 1 then
        intercept_active = false
        original_stream.scroll(-lines_below) -- if zero no scroll action is made [good]
        tty.setCursor(1, height) -- move to end
        clear_line()
        io.write(":") -- status
        local _, _, _, code = original_stream:pull(nil, "key_down") -- nil timeout is math.huge
        if code == keyboard.keys.q then
          clear_line()
          os.exit(1) -- abort
        elseif code == keyboard.keys["end"] then
          io.stdout.stream.scroll = nil -- remove handler
        elseif code == keyboard.keys.space or code == keyboard.keys.pageDown then
          io.write("\27[2J\27[H") -- clear whole screen, get new page drawn; move cursor to 1,1
        elseif code == keyboard.keys.enter or code == keyboard.keys.down then
          clear_line() -- remove status bar
          original_stream.scroll(1) -- move everything up one
          tty.setCursor(1, height - 1)
        end
        intercept_active = true
      end
      return original_stream.scroll(...)
    end
  }, {__index=original_stream})

  local custom_output_buffer = buffer.new("w", custom_stream)
  custom_output_buffer:setvbuf("no")
  io.output(custom_output_buffer)
end

return loadfile(shell.resolve("cat", "lua"))(...)
