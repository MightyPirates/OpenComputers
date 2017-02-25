--[[
OpenPrograms package manager, browser and downloader, for easy access to many programs
Author: Vexatos

Warning! This file is just an auto-installer for OPPM!
DO NOT EVER TRY TO INSTALL A PACKAGE WITH THIS!
Once you have installed OPPM, you can remove the floppy disk
and run the installed OPPM version just fine.
]]
local component = require("component")
local event = require("event")
local fs = require("filesystem")
local serial = require("serialization")
local shell = require("shell")
local term = require("term")

local gpu = component.gpu

local internet
local wget

local args, options = shell.parse(...)

local function getInternet()
  if not component.isAvailable("internet") then
    io.stderr:write("This program requires an internet card to run.")
    return false
  end
  internet = require("internet")
  wget = loadfile("/bin/wget.lua")
  return true
end

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

local NIL = {}
local function cached(f)
  return options.nocache and f or setmetatable(
    {},
    {
      __index=function(t,k)
        local v = f(k)
        t[k] = v
        return v
      end,
      __call=function(t,k)
        if k == nil then
          k = NIL
        end
        return t[k]
      end,
    }
  )
end


local getRepos = cached(function()
  local success, sRepos = pcall(getContent,"https://raw.githubusercontent.com/OpenPrograms/openprograms.github.io/master/repos.cfg")
  if not success then
    io.stderr:write("Could not connect to the Internet. Please ensure you have an Internet connection.")
    return -1
  end
  return serial.unserialize(sRepos)
end)

local getPackages = cached(function(repo)
  local success, sPackages = pcall(getContent,"https://raw.githubusercontent.com/"..repo.."/master/programs.cfg")
  if not success or not sPackages then
    return -1
  end
  return serial.unserialize(sPackages)
end)

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
    if fs.exists(path) then
      error("file already exists and option -f is not enabled")
    end
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
      local tProcess = os.getenv("_")
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
        elseif type(lPacks) == "table" then
          for k,kt in pairs(lPacks) do
            if not kt.hidden then
              table.insert(packages,k)
            end
          end
        end
      end
    end
    local lRepos = readFromFile(2)
    if lRepos and lRepos.repos then
      for _,j in pairs(lRepos.repos) do
        for k,kt in pairs(j) do
          if not kt.hidden then
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

local function parseFolders(pack, repo, info)

  local function getFolderTable(repo, namePath, branch)
    local success, filestring = pcall(getContent,"https://api.github.com/repos/"..repo.."/contents/"..namePath.."?ref="..branch)
    if not success or filestring:find('"message": "Not Found"') then
      io.stderr:write("Error while trying to parse folder names in declaration of package "..pack..".\n")
      if filestring:find('"message": "Not Found"') then
        io.stderr:write("Folder "..namePath.." does not exist.\n")
      else
        io.stderr:write(filestring.."\n")
      end
      io.stderr:write("Please contact the author of that package.\n")
      return nil
    end
    return serial.unserialize(filestring:gsub("%[", "{"):gsub("%]", "}"):gsub("(\"[^%s,]-\")%s?:", "[%1] = "), nil)
  end

  local function nonSpecial(text)
    return text:gsub("([%^%$%(%)%%%.%[%]%*%+%-%?])", "%%%1")
  end

  local function unserializeFiles(files, repo, namePath, branch, relPath)
    if not files then return nil end
    local tFiles = {}
    for _,v in pairs(files) do
      if v["type"] == "file" then
        local newPath = v["download_url"]:gsub("https?://raw.githubusercontent.com/"..nonSpecial(repo).."(.+)$", "%1"):gsub("/*$",""):gsub("^/*","")
        tFiles[newPath] = relPath
      elseif v["type"] == "dir" then
        local newFiles = unserializeFiles(getFolderTable(repo, namePath.."/"..v["name"], branch), repo, namePath, branch, fs.concat(relPath, v["name"]))
        for p,q in pairs(newFiles) do
          tFiles[p] = q
        end
      end
    end
    return tFiles
  end

  local newInfo = info
  for i,j in pairs(info.files) do
    if string.find(i,"^:")  then
      local iPath = i:gsub("^:","")
      local branch = string.gsub(iPath,"^(.-)/.+","%1"):gsub("/*$",""):gsub("^/*","")
      local namePath = string.gsub(iPath,".-(/.+)$","%1"):gsub("/*$",""):gsub("^/*","")
      local absolutePath = j:find("^//")

      local files = unserializeFiles(getFolderTable(repo, namePath, branch), repo, namePath, branch, j:gsub("^//","/"))
      if not files then return nil end
      for p,q in pairs(files) do
        if absolutePath then
          newInfo.files[p] = "/"..q
        else
          newInfo.files[p] = q
        end
      end
      newInfo.files[i] = nil
    end
  end
  return newInfo
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
            return parseFolders(pack, j.repo, lPacks[k]),j.repo
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
          return parseFolders(pack, i, j[k]),i
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
  if info.files then
    local c = 0
    for i in pairs(info.files) do
      c = c + 1
    end
    if c > 0 then
      print("Number of files: "..tostring(c))
      done = true
    end
  end
  if not done then
    print("No information provided.")
  end
end

local function installPackage(pack,path,update)
  local tPacks = readFromFile(1)
  update = update or false
  if not pack then
    printUsage()
    return
  end
  if not path then
    local lConfig = readFromFile(2)
    path = lConfig.path or "/usr"
    if not update then
      print("Installing package to "..path.."...")
    end
  elseif not update then
    path = shell.resolve(path)
    if not update then
      print("Installing package to "..path.."...")
    end
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
    if not tPacks[pack] then
      io.stderr:write("error while checking update path\n")
      return
    end
    for i,j in pairs(info.files) do
      if not string.find(j,"^//") then
        for k,v in pairs(tPacks[pack]) do
          if k==i then
            path = string.gsub(fs.path(v),j.."/?$","/")
            break
          end
        end
        if path then
          break
        end
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
      response = response or "no error message"
      term.write("Error while installing files for package '"..pack.."': "..response..". Reverting installation... ")
      fs.remove(nPath)
      for o,p in pairs(tPacks[pack]) do
        fs.remove(p)
        tPacks[pack][o]=nil
      end
      print("Done.\nPlease contact the package author about this problem.")
      return
    end
  end
  saveToFile(tPacks)

  if info.dependencies then
    term.write("Done.\nInstalling Dependencies...\n")
    for i,j in pairs(info.dependencies) do
      local nPath
      if string.find(j,"^//") then
        nPath = string.sub(j,2)
      else
        nPath = fs.concat(path,j)
      end
      if string.lower(string.sub(i,1,4))=="http" then
        nPath = fs.concat(nPath, string.gsub(i,".+(/.-)$","%1"),nil)
        local success,response = pcall(downloadFile,i,nPath)
        if success and response then
          tPacks[pack][i] = nPath
          saveToFile(tPacks)
        else
          response = response or "no error message"
          term.write("Error while installing files for package '"..pack.."': "..response..". Reverting installation... ")
          fs.remove(nPath)
          for o,p in pairs(tPacks[pack]) do
            fs.remove(p)
            tPacks[pack][o]=nil
          end
          saveToFile(tPacks)
          print("Done.\nPlease contact the package author about this problem.")
          return tPacks
        end
      else
        local depInfo = getInformation(string.lower(i))
        if not depInfo then
          term.write("\nDependency package "..i.." does not exist.")
        end
        local tNewPacks = installPackage(string.lower(i),nPath,update)
        if tNewPacks then
          tPacks = tNewPacks
        end
      end
    end
  end
  saveToFile(tPacks)
  term.write("Done.\n")
  print("Successfully installed package "..pack)
  return tPacks
end

local function uninstallPackage(pack)
  local tFiles = readFromFile(1)
  if not tFiles then
    io.stderr:write("Error while trying to read package names")
    return
  elseif tFiles[1]==-1 then
    table.remove(tFiles,1)
  end
  if not tFiles[pack] then
    print("Package has not been installed.")
    print("If it has, the package could not be identified.")
    print("In this case you have to remove it manually.")
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

if options.iKnowWhatIAmDoing then
  if args[1] == "list" then
    if not getInternet() then return end
    local packs = listPackages(args[2])
    printPackages(packs)
  elseif args[1] == "info" then
    if not getInternet() then return end
    provideInfo(args[2])
  elseif args[1] == "install" then
    if not getInternet() then return end
    return installPackage(args[2], args[3], false)
  elseif args[1] == "update" then
    if not getInternet() then return end
    updatePackage(args[2])
  elseif args[1] == "uninstall" then
    uninstallPackage(args[2])
  else
    printUsage()
    return
  end
end

io.stderr:write("Please install oppm by running /bin/install.lua")
