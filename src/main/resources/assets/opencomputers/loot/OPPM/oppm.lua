--[[
OpenPrograms package manager, browser and downloader, for easy access to many programs
Author: Vexatos
]]
local component = require("component")
local event = require("event")
local fs = require("filesystem")
local process = require("process")
local serial = require("serialization")
local shell = require("shell")
local term = require("term")

local wget = loadfile("/bin/wget.lua")

local gpu = component.gpu

if not component.isAvailable("internet") then
  io.stderr:write("This program requires an internet card to run.")
  return
end
local internet = require("internet")

local args, options = shell.parse(...)


local function printUsage()
  print("OpenPrograms Package Manager, use this to browse through and download OpenPrograms programs easily")
  print("Usage:")
  print("'oppm list [-i]' to get a list of all the available program packages")
  print("'oppm list [-i] <filter>' to get a list of available packages containing the specified substring")
  print(" -i: Only list already installed packages")
  print("'oppm info <package>' to get further information about a program package")
  print("'oppm install [-f] <package> [path]' to download a package to a directory on your system (or /usr by default)")
  print("'oppm update <package>' to update an already installed package")
  print("'oppm update all' to update every already installed package")
  print("'oppm uninstall <package>' to remove a package from your system")
  print(" -f: Force creation of directories and overwriting of existing files.")
end

local function getContent(url)
  local sContent = ""
  local result, response = pcall(internet.request, url)
  if not result then
    return nil
  end
    for chunk in response do
      sContent = sContent..chunk
    end
  return sContent
end

local function getRepos()
  local success, sRepos = pcall(getContent,"https://raw.githubusercontent.com/OpenPrograms/openprograms.github.io/master/repos.cfg")
  if not success then
    io.stderr:write("Could not connect to the Internet. Please ensure you have an Internet connection.")
    return -1
  end
  return serial.unserialize(sRepos)
end

local function getPackages(repo)
  local success, sPackages = pcall(getContent,"https://raw.githubusercontent.com/"..repo.."/master/programs.cfg")
  if not success or not sPackages then
    return -1
  end
  return serial.unserialize(sPackages)
end

--For sorting table values by alphabet
local function compare(a,b)
  for i=1,math.min(#a,#b) do
    if a:sub(i,i)~=b:sub(i,i) then
      return a:sub(i,i) < b:sub(i,i)
    end
  end
  return #a < #b
end

local function downloadFile(url,path,force)
  if options.f or force then
    return wget("-fq",url,path)
  else
    return wget("-q",url,path)
  end
end

local function readFromFile(fNum)
  local path
  if fNum == 1 then
    path = "/etc/opdata.svd"
  elseif fNum == 2 then
    path = "/etc/oppm.cfg"
    if not fs.exists(path) then
      local tProcess = process.running()
      path = fs.concat(fs.path(shell.resolve(tProcess)),"/etc/oppm.cfg")
    end
  end
  if not fs.exists(fs.path(path)) then
    fs.makeDirectory(fs.path(path))
  end
  if not fs.exists(path) then
    return {-1}
  end
  local file,msg = io.open(path,"rb")
  if not file then
    io.stderr:write("Error while trying to read file at "..path..": "..msg)
    return
  end
  local sPacks = file:read("*a")
  file:close()
  return serial.unserialize(sPacks) or {-1}
end

local function saveToFile(packs)
  local file,msg = io.open("/etc/opdata.svd","wb")
  if not file then
    io.stderr:write("Error while trying to save package names: "..msg)
    return
  end
  local sPacks = serial.serialize(packs)
  file:write(sPacks)
  file:close()
end

local function listPackages(filter)
  filter = filter or false
  if filter then
    filter = string.lower(filter)
  end
  local packages = {}
  print("Receiving Package list...")
  if not options.i then
    local success, repos = pcall(getRepos)
    if not success or repos==-1 then
      io.stderr:write("Unable to connect to the Internet.\n")
      return
    elseif repos==nil then
        print("Error while trying to receive repository list")
        return
    end
    for _,j in pairs(repos) do
      if j.repo then
        print("Checking Repository "..j.repo)
        local lPacks = getPackages(j.repo)
        if lPacks==nil then
          io.stderr:write("Error while trying to receive package list for " .. j.repo.."\n")
          return
        elseif type(lPacks) == "table" then
          for k in pairs(lPacks) do
            if not k.hidden then
              table.insert(packages,k)
            end
          end
        end
      end
    end
    local lRepos = readFromFile(2)
    if lRepos and lRepos.repos then
      for _,j in pairs(lRepos.repos) do
        for k in pairs(j) do
          if not k.hidden then
            table.insert(packages,k)
          end
        end
      end
    end
  else
    local lPacks = {}
    local packs = readFromFile(1)
    for i in pairs(packs) do
      table.insert(lPacks,i)
    end
    packages = lPacks
  end
  if filter then
    local lPacks = {}
    for i,j in ipairs(packages) do
      if (#j>=#filter) and string.find(j,filter,1,true)~=nil then
          table.insert(lPacks,j)
      end
    end
    packages = lPacks
  end
  table.sort(packages,compare)
  return packages
end

local function printPackages(packs)
  if packs==nil or not packs[1] then
    print("No package matching specified filter found.")
    return
  end
  term.clear()
  local xRes,yRes = gpu.getResolution()
  print("--OpenPrograms Package list--")
  local xCur,yCur = term.getCursor()
  for _,j in ipairs(packs) do
    term.write(j.."\n")
    yCur = yCur+1
    if yCur>yRes-1 then
      term.write("[Press any key to continue]")
      local event = event.pull("key_down")
      if event then
        term.clear()
        print("--OpenPrograms Package list--")
        xCur,yCur = term.getCursor()
      end
    end
  end
end

local function getInformation(pack)
  local success, repos = pcall(getRepos)
  if not success or repos==-1 then
    io.stderr:write("Unable to connect to the Internet.\n")
    return
  end
  for _,j in pairs(repos) do
    if j.repo then
      local lPacks = getPackages(j.repo)
      if lPacks==nil then
        io.stderr:write("Error while trying to receive package list for "..j.repo.."\n")
      elseif type(lPacks) == "table" then
        for k in pairs(lPacks) do
          if k==pack then
            return lPacks[k],j.repo
          end
        end
      end
    end
  end
  local lRepos = readFromFile(2)
  if lRepos then
    for i,j in pairs(lRepos.repos) do
      for k in pairs(j) do
        if k==pack then
          return j[k],i
        end
      end
    end
  end
  return nil
end

local function provideInfo(pack)
  if not pack then
    printUsage()
    return
  end
  pack = string.lower(pack)
  local info = getInformation(pack)
  if not info then
    print("Package does not exist")
    return
  end
  local done = false
  print("--Information about package '"..pack.."'--")
  if info.name then
    print("Name: "..info.name)
    done = true
  end
  if info.description then
    print("Description: "..info.description)
    done = true
  end
  if info.authors then
    print("Authors: "..info.authors)
    done = true
  end
  if info.note then
    print("Note: "..info.note)
    done = true
  end
  if not done then
    print("No information provided.")
  end
end

local tPacks = readFromFile(1)

local function installPackage(pack,path,update)
  update = update or false
  if not pack then
    printUsage()
    return
  end
  if not path and not update then
    local lConfig = readFromFile(2)
    path = lConfig.path or "/usr"
    print("Installing package to "..path.."...")
  elseif not update then
    path = shell.resolve(path)
    print("Installing package to "..path.."...")
  end
  pack = string.lower(pack)

  if not tPacks then
    io.stderr:write("Error while trying to read local package names")
    return
  elseif tPacks[1]==-1 then
    table.remove(tPacks,1)
  end

  local info,repo = getInformation(pack)
  if not info then
    print("Package does not exist")
    return
  end
  if update then
    print("Updating package "..pack)
    path = nil
    for i,j in pairs(info.files) do
      if tPacks[pack] then
        for k,v in pairs(tPacks[pack]) do
          if k==i then
            path = string.gsub(fs.path(v),j.."/?$","/")
            break
          end
        end
        if path then
          break
        end
      else
        io.stderr:write("error while checking update path")
        return
      end
    end
    path = shell.resolve(string.gsub(path,"^/?","/"),nil)
  end
  if not update and fs.exists(path) then
    if not fs.isDirectory(path) then
      if options.f then
        path = fs.concat(fs.path(path),pack)
        fs.makeDirectory(path)
      else
        print("Path points to a file, needs to be a directory.")
        return
      end
    end
  elseif not update then
    if options.f then
      fs.makeDirectory(path)
    else
      print("Directory does not exist.")
      return
    end
  end
  if tPacks[pack] and (not update) then
    print("Package has already been installed")
    return
  elseif not tPacks[pack] and update then
    print("Package has not been installed.")
    print("If it has, uninstall it manually and reinstall it.")
    return
  end
  if update then
    term.write("Removing old files...")
    for _,j in pairs(tPacks[pack]) do
      fs.remove(j)
    end
    term.write("Done.\n")
  end
  tPacks[pack] = {}
  term.write("Installing Files...")
  for i,j in pairs(info.files) do
    local nPath
    if string.find(j,"^//") then
      local lPath = string.sub(j,2)
      if not fs.exists(lPath) then
        fs.makeDirectory(lPath)
      end
      nPath = fs.concat(lPath,string.gsub(i,".+(/.-)$","%1"),nil)
    else
      local lPath = fs.concat(path,j)
      if not fs.exists(lPath) then
        fs.makeDirectory(lPath)
      end
      nPath = fs.concat(path,j,string.gsub(i,".+(/.-)$","%1"),nil)
    end
    local success,response = pcall(downloadFile,"https://raw.githubusercontent.com/"..repo.."/"..i,nPath)
    if success and response then
      tPacks[pack][i] = nPath
    else
      term.write("Error while installing files for package '"..pack.."'. Reverting installation... ")
      fs.remove(nPath)
      for o,p in pairs(tPacks[pack]) do
        fs.remove(p)
        tPacks[pack][o]=nil
      end
      print("Done.\nPlease contact the package author about this problem.")
      return
    end
  end
  if info.dependencies then
    term.write("Done.\nInstalling Dependencies...\n")
    for i,j in pairs(info.dependencies) do
      local nPath
      if string.find(j,"^//") then
        nPath = string.sub(j,2)
      else
        nPath = fs.concat(path,j,string.gsub(i,".+(/.-)$","%1"),nil)
      end
      if string.lower(string.sub(i,1,4))=="http" then
        local success,response = pcall(downloadFile,i,nPath)
        if success and response then
          tPacks[pack][i] = nPath
        else
          term.write("Error while installing dependency package '"..i.."'. Reverting installation... ")
          fs.remove(nPath)
          for o,p in pairs(tPacks[pack]) do
            fs.remove(p)
            tPacks[pack][o]=nil
          end
          print("Done.\nPlease contact the package author about this problem.")
          return
        end
      else
        local depInfo = getInformation(string.lower(i))
        if not depInfo then
          term.write("\nDependency package "..i.." does not exist.")
        end
        installPackage(string.lower(i),fs.concat(path,j),update)
      end
    end
  end
  term.write("Done.\n")
  saveToFile(tPacks)
  print("Successfully installed package "..pack)
end

local function uninstallPackage(pack)
  local info,repo = getInformation(pack)
  if not info then
    print("Package does not exist")
    return
  end
  local tFiles = readFromFile(1)
  if not tFiles then
    io.stderr:write("Error while trying to read package names")
    return
  elseif tFiles[1]==-1 then
    table.remove(tFiles,1)
  end
  if not tFiles[pack] then
      print("Package has not been installed.")
      print("If it has, you have to remove it manually.")
      return
  end
  term.write("Removing package files...")
  for i,j in pairs(tFiles[pack]) do
    fs.remove(j)
  end
  term.write("Done\nRemoving references...")
  tFiles[pack]=nil
  saveToFile(tFiles)
  term.write("Done.\n")
  print("Successfully uninstalled package "..pack)
end

local function updatePackage(pack)
  if pack=="all" then
    print("Updating everything...")
    local tFiles = readFromFile(1)
    if not tFiles then
      io.stderr:write("Error while trying to read package names")
      return
    elseif tFiles[1]==-1 then
      table.remove(tFiles,1)
    end
    local done = false
    for i in pairs(tFiles) do
      installPackage(i,nil,true)
      done = true
    end
    if not done then
      print("No package has been installed so far.")
    end
  else
    installPackage(args[2],nil,true)
  end
end

if args[1] == "list" then
  local packs = listPackages(args[2])
  printPackages(packs)
elseif args[1] == "info" then
  provideInfo(args[2])
elseif args[1] == "install" then
  installPackage(args[2],args[3],false)
elseif args[1] == "update" then
  updatePackage(args[2])
elseif args[1] == "uninstall" then
  uninstallPackage(args[2])
else
  printUsage()
  return
end
