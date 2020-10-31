local fs = require("filesystem")
local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: ln <target> [<name>]\n")
  return 1
end

local target_name = args[1]
local target = shell.resolve(target_name)

-- don't link from target if it doesn't exist, unless it is a broken link
if not fs.exists(target) and not fs.isLink(target) then
  io.stderr:write("ln: failed to access '" .. target_name .. "': No such file or directory\n")
  return 1
end

local linkpath
if #args > 1 then
  linkpath = shell.resolve(args[2])
else
  linkpath = fs.concat(shell.getWorkingDirectory(), fs.name(target))
end

if fs.isDirectory(linkpath) then
  linkpath = fs.concat(linkpath, fs.name(target))
end

local result, reason = fs.link(target_name, linkpath)
if not result then
  io.stderr:write(reason..'\n')
  return 1
end
