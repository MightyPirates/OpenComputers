local component = require("component")
local fs = require("filesystem")
local shell = require("shell")

local dirs = shell.parse(...)
if #dirs == 0 then
  io.write("Usage: ln <target> [<name>]\n")
  return 1
end

local target = shell.resolve(dirs[1])

-- don't link from target if it doesn't exist, unless it is a broken link
if not fs.exists(target) and not fs.isLink(target) then
  io.stderr:write("ln: failed to access '" .. target .. "': No such file or directory\n")
  return 1
end

local linkpath
if #dirs > 1 then
  linkpath = shell.resolve(dirs[2])
else
  linkpath = fs.concat(shell.getWorkingDirectory(), fs.name(target))
end

if fs.isDirectory(linkpath) then
  linkpath = fs.concat(linkpath, fs.name(target))
end

local result, reason = fs.link(target, linkpath)
if not result then
  io.stderr:write(reason..'\n')
  return 1
end
