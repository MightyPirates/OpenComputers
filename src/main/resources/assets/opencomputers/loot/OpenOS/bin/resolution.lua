local shell = require("shell")
local term = require("term")

local function gpu()
  return select(2, term.getGPU())
end

local args = shell.parse(...)
if #args == 0 then
  local w, h = gpu().getViewport()
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

local result, reason = gpu().setResolution(w, h)
if not result then
  if reason then -- otherwise we didn't change anything
    io.stderr:write(reason)
  end
  return
end
term.clear()
