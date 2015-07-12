local component = require("component")
local fs = require("filesystem")
local shell = require("shell")

local dirs = shell.parse(...)
if #dirs == 0 then
  io.write("Usage: ln  []")
  return
end

local target = shell.resolve(dirs[1])
local linkpath
if #dirs > 1 then
  linkpath = shell.resolve(dirs[2])
else
  linkpath = fs.concat(shell.getWorkingDirectory(), fs.name(target))
end

local result, reason = fs.link(target, linkpath)
if not result then
  io.stderr:write(reason)
end

