--[[ This program allows downloading and uploading from and to pastebin.com.
     Authors: Sangar, Vexatos ]]
local component = require("component")
local fs = require("filesystem")
local internet = require("internet")
local shell = require("shell")
local term = require("term")

if not component.isAvailable("internet") then
  print("This program requires an internet card to run.")
  return
end

local args, options = shell.parse(...)

--This gets code from the website and stores it in the specified file
local function get(pasteId, filename)
  local f, reason = io.open(filename, "w")
  if not f then
    print("Failed opening file for writing: " .. reason)
    return
  end

  term.write("Downloading from pastebin.com... ")
  local url = "http://pastebin.com/raw.php?i=" .. pasteId
  local result, response = pcall(internet.request, url)
  if result then
    print("success.")
    for chunk in response do
      if not options.k then
        string.gsub(chunk, "\r\n", "\n")
      end
      f:write(chunk)
    end

    f:close()
    print("Saved data to " .. filename)
  else
    f:close()
    fs.remove(filename)
    print("HTTP request failed: " .. response)
  end
end

-- This makes a string safe for being used in a URL.
function encode(code)
  if code then
    code = string.gsub(code, "([^%w ])", function (c)
      return string.format("%%%02X", string.byte(c))
    end)
    code = string.gsub (code, " ", "+")
  end
  return code 
end

-- This stores the program in a temporary file, which it will
-- delete after the program was executed.
function run(pasteId, ...)
  local tmpFile = os.tmpname()
  get(pasteId, tmpFile)
  print("Running...")

  local success, reason = shell.execute(tmpFile, _ENV, ...)
  if not success then
    print(reason)
  end
  fs.remove(tmpFile)
end

-- Uploads the specified file as a new paste to pastebin.com.
function put(path)
  local config = {}
  local configFile = loadfile("/etc/pastebin.conf", "t", config)
  if configFile then
    local result, reason = pcall(configFile)
    if not result then
      print("Failed loading config: " .. reason)
    end
  end
  config.key = config.key or "fd92bd40a84c127eeb6804b146793c97"
  local file, reason = io.open(path, "r")

  if not file then
    print("Failed opening file for reading: " .. reason)
    return
  end

  local data = file:read("*a")
  file:close()

  term.write("Uploading to pastebin.com... ")
  local result, response = pcall(internet.request,
        "http://pastebin.com/api/api_post.php", 
        "api_option=paste&" ..
        "api_dev_key=" .. config.key .. "&" ..
        "api_paste_format=lua&" ..
        "api_paste_expire_date=N&" ..
        "api_paste_name=" .. encode(fs.name(path)) .. "&" ..
        "api_paste_code=" .. encode(data))

  if result then
    local info = ""
    for chunk in response do
      info = info .. chunk
    end
    if string.match(info, "^Bad API request, ") then
      print("failed.")
      print(info)
    else
      print("success.")
      local pasteId = string.match(info, "[^/]+$")
      print("Uploaded as " .. info)
      print('Run "pastebin get ' .. pasteId .. '" to download anywhere.')
    end
  else
    print("failed: " .. response)
  end
end

local command = args[1]
if command == "put" then
  if #args == 2 then
    put(shell.resolve(args[2]))
    return
  end
elseif command == "get" then
  if #args == 3 then
    local path = shell.resolve(args[3])
    if fs.exists(path) then
      if not options.f or not os.remove(path) then
        print("file already exists")
        return
      end
    end
    get(args[2], path)
    return
  end
elseif command == "run" then
  if #args >= 2 then
    run(args[2], table.unpack(args, 3))
    return
  end
end

-- If we come here there was some invalid input.
print("Usages:")
print("pastebin put [-f] <file>")
print("pastebin get [-f] <id> <file>")
print("pastebin run [-f] <id> [<arguments...>]")
print(" -f: Force overwriting existing files.")
print(" -k: keep line endings as-is (will convert")
print("     Windows line endings to Unix otherwise).")