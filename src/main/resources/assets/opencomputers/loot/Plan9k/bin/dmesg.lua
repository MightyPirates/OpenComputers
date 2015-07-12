local event = require "event"
local component = require "component"
local keyboard = require "keyboard"

local args = {...}
local color, isPal, evt

io.write("Press 'Ctrl-C' to exit\n")
pcall(function()
  repeat
    if #args > 0 then
      evt = table.pack(event.pullMultiple("interrupted", table.unpack(args)))
    else
      evt = table.pack(event.pull())
    end
    if evt[1] then
        io.write("[" .. os.date("%T") .. "] ")
        io.write(tostring(evt[1]) .. string.rep(" ", math.max(10 - #tostring(evt[1]), 0) + 1))
        io.write(tostring(evt[2]) .. string.rep(" ", 37 - #tostring(evt[2])))
        if evt.n > 2 then
          for i = 3, evt.n do
            io.write("  " .. tostring(evt[i]):gsub("\x1b", ""))
          end
        end
        
        io.write("\n")
    end
  until evt[1] == "interrupted"
end)



