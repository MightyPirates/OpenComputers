local component = require("component")
local shell = require("shell")
local term = require("term")

local args = shell.parse(...)
if #args == 0 then
  local w, h = component.gpu.getResolution()
  io.write(w .. " " .. h)
  return
end

if #args < 2 then
  io.write("Usage: resolution [<width> <height>]")
  return
end

local w = tonumber(args[1])
local h = tonumber(args[2])
if not w or not h then
  io.stderr:write("invalid width or height")
  return
end

local result, reason = component.gpu.setResolution(w, h)
if not result then
  if reason then -- otherwise we didn't change anything
    io.stderr:write(reason)
  end
  return
end
term.clear()