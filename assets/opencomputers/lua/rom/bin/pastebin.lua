local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 2 then
  print("Usage: pastebin [-f] <id> <file>")
  print(" -f: Force overwriting existing files.")
  print(" -k: keep line endings as-is (will convert")
  print("     Windows line endings to Unix otherwise).")
  return
end

local id = args[1]
local filename = shell.resolve(args[2])

if fs.exists(filename) then
  if not options.f or not os.remove(filename) then
    print("file already exists")
    return
  end
end

local f, reason = io.open(filename, "w")
if not f then
  print("failed opening file for writing: " .. reason)
  return
end

local url = "http://pastebin.com/raw.php?i=" .. id
local result, response = pcall(internet.request, url)
if result then
  for chunk in response do
    if not options.k then
      string.gsub(chunk, "\r\n", "\n")
    end
    f:write(chunk)
  end

  f:close()
  print("saved data to " .. filename)
else
  f:close()
  fs.remove(filename)
  print("http request failed: " .. response)
end