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

io.write("\x1b9" .. h .. ";" .. w .. "R")
term.clear()
