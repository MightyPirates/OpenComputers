local computer = require("computer")
local fs = require("filesystem")
local shell = require("shell")
local lib = {}

function lib.locate(path)
  for _,lookup in ipairs(computer.getProgramLocations()) do
    if lookup[1] == path then
      return lookup[2]
    end
  end
end

function lib.reportNotFound(path, reason)
  checkArg(1, path, "string")
  if fs.isDirectory(shell.resolve(path)) then
    io.stderr:write(path .. ": is a directory\n")
    return 126
  end
  local loot = lib.locate(path)
  if loot then
    io.stderr:write("The program '" .. path .. "' is currently not installed.  To install it:\n" ..
      "1. Craft the '" .. loot .. "' floppy disk and insert it into this computer.\n" ..
      "2. Run `install " .. loot  .. "`")
  elseif type(reason) == "string" then
    io.stderr:write(path .. ": " .. reason .. "\n")
  end
  return 127
end

return lib
