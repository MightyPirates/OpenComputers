local component = require("component")
local fs = require("filesystem")
local internet = require("internet")
local shell = require("shell")
local text = require("text")

if not component.isAvailable("internet") then
  io.stderr:write("This program requires an internet card to run.")
  return
end

local args, options = shell.parse(...)

if #args < 1 then
  io.write("Usage: wget [-fq] url <filename>\n")
  io.write(" -f: Force overwriting existing files.\n")
  io.write(" -q: Quit mode - no status messages.")
  return
end

local url = text.trim(args[1])
local filename = args[2]
if not filename then
  filename = url
  local index = string.find(filename, "/[^/]*$")
  if index then
    filename = string.sub(filename, index + 1)
  end
  index = string.find(filename, "?", 1, true)
  if index then
    filename = string.sub(filename, 1, index - 1)
  end
end
filename = text.trim(filename)
if filename == "" then
  io.stderr:write("could not infer filename, please specify one")
  return
end
filename = shell.resolve(filename)

if fs.exists(filename) then
  if not options.f or not os.remove(filename) then
    io.stderr:write("file already exists")
    return
  end
end

local f, reason = io.open(filename, "wb")
if not f then
  io.stderr:write("failed opening file for writing: " .. reason)
  return
end

if not options.q then
  io.write("Downloading... ")
end
local result, response = pcall(internet.request, url)
if result then
  if not options.q then
    io.write("success.\n")
  end
  for chunk in response do
    f:write(chunk)
  end

  f:close()
  if not options.q then
    io.write("Saved data to " .. filename .. "\n")
  end
else
  if not options.q then
    io.write("failed.\n")
  end
  f:close()
  fs.remove(filename)
  io.stderr:write("HTTP request failed: " .. response .. "\n")
end