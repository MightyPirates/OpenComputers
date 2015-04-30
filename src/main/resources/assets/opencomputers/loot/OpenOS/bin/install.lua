local component = require("component")
local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local shell = require("shell")
local unicode = require("unicode")

-- Modified version of recurse from /bin/cp.lua
local function rcopy(fromPath, toPath, update)
  update = update or false
  os.sleep(0) -- allow interrupting
  if fs.isDirectory(fromPath) then
    if fs.canonical(fromPath) == fs.canonical(fs.path(toPath)) then
      return nil, "cannot copy a directory, `" .. fromPath .. "', into itself, `" .. toPath .. "'\n"
    end
    if fs.exists(toPath) and not fs.isDirectory(toPath) then
      -- my real cp always does this, even with -f, -n or -i.
      return nil, "cannot overwrite non-directory `" .. toPath .. "' with directory `" .. fromPath .. "'"
    end
    print(fromPath .. " -> " .. toPath)
    fs.makeDirectory(toPath)
    for file in fs.list(fromPath) do
      local result, reason = rcopy(fs.concat(fromPath, file), fs.concat(toPath, file), update)
      if not result then
        return nil, reason
      end
    end
    return true
  else
    if fs.exists(toPath) then
      if fs.canonical(fromPath) == fs.canonical(toPath) then
        return nil, "`" .. fromPath .. "' and `" .. toPath .. "' are the same file"
      end
      if fs.isDirectory(toPath) then
        return nil, "cannot overwrite directory `" .. toPath .. "' with non-directory"
      else
        if update then
          local fromFile = io.open(fs.canonical(fromPath), "r")
          local toFile = io.open(fs.canonical(toPath), "r")
          local skipreason = "Unchanged file"
          
          repeat
            local fromStr = fromFile:read(4096)
            local toStr = toFile:read(4096)
            if fromStr ~= toStr then
              local answer = ""
              repeat
                io.write("Overwrite " .. toPath .. "? [y/n/q] ")
                answer = io.read()
              until answer ~= ""
              
              if answer == "y" then
                skipreason = false
              elseif answer == "n" then
                skipreason = "Not overwriting"
              else
                return nil, "aborted by user"
              end
              break
            end
          until fromStr == nil and toStr == nil
          
          fromFile:close()
          toFile:close()
          
          if skipreason then
            print(skipreason .. ": " .. toPath)
            return true
          end
        end
        -- else: default to overwriting
      end
      fs.remove(toPath)
    end
    print(fromPath .. " -> " .. toPath)
    return fs.copy(fromPath, toPath)
  end
end

local args, options = shell.parse(...)

local candidates = {}
for address in component.list("filesystem") do
  local dev = component.proxy(address)
  if not dev.isReadOnly() and dev.address ~= computer.tmpAddress() then
    table.insert(candidates, dev)
  end
end

if #candidates == 0 then
  print("No writable disks found, aborting.")
  return
end

for i = 1, #candidates do
  local label = candidates[i].getLabel()
  if label then
    label = label .. " (" .. candidates[i].address:sub(1, 8) .. "...)"
  else
    label = candidates[i].address
  end
  print(i .. ") " .. label)
end

print("To select the device to install to, please enter a number between 1 and " .. #candidates .. ".")
print("Press 'q' to cancel the installation.")
local choice
while not choice do
  result = io.read()
  if result:sub(1, 1):lower() == "q" then
    return
  end
  local number = tonumber(result)
  if number and number > 0 and number <= #candidates then
    choice = candidates[number]
  else
    print("Invalid input, please try again.")
  end
end
candidates = nil

local name = options.name or "OpenOS"
print("Installing " .. name .. " to device " .. (choice.getLabel() or choice.address))
os.sleep(0.25)
local origin = options.from and options.from:sub(1,3) or computer.getBootAddress():sub(1, 3)
local fromDir = options.fromDir or "/"
local mnt = choice.address:sub(1, 3)

local result, reason = rcopy("/mnt/" .. origin .. fromDir, "/mnt/" .. mnt .. "/", options.update)
if not result then
  error(reason, 0)
end
if not options.nolabelset then pcall(choice.setLabel, name) end

if not options.noreboot then
  print("All done! " .. ((not options.noboot) and "Set as boot device and r" or "R") .. "eboot now? [Y/n]")
  local result = io.read()
  if not result or result == "" or result:sub(1, 1):lower() == "y" then
    if not options.noboot then computer.setBootAddress(choice.address)end
    print("\nRebooting now!")
    computer.shutdown(true)
  end
end
print("Returning to shell.")
