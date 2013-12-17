local args = shell.parse(...)
if #args == 0 then
  print(component.gpu.getResolution())
  return
end

if #args < 2 then
  print("Usage: resolution [<width> <height>]")
  return
end

local w = tonumber(args[1])
local h = tonumber(args[2])
if not w or not h then
  print("invalid width or height")
  return
end

local result, reason = component.gpu.setResolution(w, h)
if not result then
  print(reason)
end
term.clear()