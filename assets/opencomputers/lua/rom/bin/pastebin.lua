local args, options = shell.parse(...)
if #args < 2 then
  print("Usage: pastebin [-f] <id> <file>")
  print(" -f: Force overwriting existing files.")
  return
end

local m = component.modem
if not m or not m.isWireless() then
  print("no primary wireless modem found")
  return
end

if not m.isHttpEnabled() then
  print("http support is not enabled")
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
for line in http.request(url) do
  f:write(line)
end

f:close()
print("saved data to " .. filename)
