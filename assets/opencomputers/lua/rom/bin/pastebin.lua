--This program gets code from Pastebin.com.
--It also allows you to put your code on the website
--Edited by Vexatos
local fs = require("filesystem")
local internet = require("internet")
local component = require("component")
local shell = require("shell")
local term = require("term")

local args, options = shell.parse(...)
local function printUsage()
  print("Usages:")
  print("pastebin put [-f] <file>")
  print("pastebin get [-f] <id> <file>")
  print("pastebin run [-f] <id> <arguments>")
  print(" -f: Force overwriting existing files.")
  print(" -k: keep line endings as-is (will convert")
  print("     Windows line endings to Unix otherwise).")
end
if #args < 2 then
  printUsage()
  return
end

if not component.isAvailable("internet") then
  print( "Error: Pastebin requires an Internet Card to run" )
  return
end

--This gets code from the website and stores it in the specified file
local function get(paste, filename)
term.write( "Connecting to pastebin.com... " )
local f, reason = io.open(filename, "w")
if not f then
  print("failed opening file for writing: " .. reason)
  return
end

local url = "http://pastebin.com/raw.php?i=" .. paste
local result, response = pcall(internet.request, url)
if result then
  print("Success.")
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
end

--This makes a string safe for being used in a URL
function encode( code )
	if code then
		code = string.gsub (code, "([^%w ])",
		function (c)
			return string.format ("%%%02X", string.byte(c))
		end)
		code = string.gsub (code, " ", "+")
	end
	return code	
end

--This stores the program in a temporary file, which it will
--delete after the program was executed
function run(paste)
local tmpFile = "/tmp/tmp_pastebin.lua"
get(paste,shell.resolve(tmpFile))
term.clear()
print("Running...")

local success, msg = shell.execute(tmpFile, _ENV, table.unpack(args, 3))
    if not success then
      print( msg )
    end
    fs.remove(tmpFile)
end

--This lets you put your own code on pastebin.com
function put(file)
  --local fKey = io.open("/etc/pastebin.key","r")
  --local key = fKey:read("*a")
  --fKey:close()
  local path = shell.resolve( file )
  local sName = fs.name( path )
  local key = "fd92bd40a84c127eeb6804b146793c97"
  local fText, reason = io.open(path,"r")

  if not fText then
    print("failed opening file for reading: " .. reason)
    return
  end

  local sText = fText:read("*a")
  fText:close()

  local result,response = pcall(internet.request,
        "http://pastebin.com/api/api_post.php", 
        "api_option=paste&"..
        "api_dev_key="..key.."&"..
        "api_paste_format=lua&"..
        "api_paste_expire_date=N&"..
        "api_paste_name="..encode(sName).."&"..
        "api_paste_code="..encode(sText)
    )
    
    if response then
        print( "Success." )
        local sResponse = ""
        for chunk in response do
          sResponse = sResponse..chunk
        end        
        local sCode = string.match( sResponse, "[^/]+$" )
        print( "Uploaded as "..sResponse )
        print( "Run \"pastebin get "..sCode.."\" to download anywhere" )
 
    else
        print( "Failed: "..response )
    end
end

local sCommand = args[1]
local kArg = args[2]
if #args >=2 then
  if sCommand=="put" then
    if #args == 2 then
      local file = kArg
      put(file)
    else
      printUsage()
      return
    end
  elseif sCommand == "get" then
    if #args == 3 then
      local sFile = shell.resolve(args[3])
      if fs.exists(sFile) then
        if not options.f or not os.remove(sFile) then
          print("file already exists")
          return
        end
      end
      get(kArg,sFile)
    else
      printUsage()
      return
    end
  elseif sCommand == "run" then
      run(kArg)
  end
else
  printUsage()
  return
end
