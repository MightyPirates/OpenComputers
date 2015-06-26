local argv = {...}
local options
local loglevel = 1

local ocData = {
    baseDir = "/var/lib/mpt/mpt.db",
    configDir = "/var/lib/mpt/config.db",
    dir = "/var/lib/mpt/"
}

local function split(text,splitter)
	local rt = {}
	local act = ""
	local x = 1
	while x  <= #text do
		if text:sub(x,x+#splitter-1) == splitter then
			if act ~= "" then
        		rt[#rt+1] = act
        	end
			x = x + #splitter
			act=""
		else
			act = act .. text:sub(x,x)
			x = x + 1
		end
	end
	if act ~= "" then
		rt[#rt+1] = act
	end
	return rt;
end

local function slice(a,i,j) local b = {} for x = i,j do b[x-i+1] = a[x] end return b end

local base, config, backend
local core
local frontends

local ocBackend
ocBackend = {

    --MAIN FLOW
    init = function()
        ocData.io = require("io")
        ocData.fs = require("filesystem")
        ocData.serialization = require("serialization")
        ocData.component = require("component")
        ocData.filesystem = require("filesystem")
        ocData.internet = require("internet")
        ocData.term = require("term")
        ocData.shell = require("shell")
        ocData.wget = loadfile("/bin/wget.lua")
        local a
        a, options = ocData.shell.parse(table.unpack(argv))
        if options.root then
            core.rootDir = options.root
            core.log(1, "OC ","Using custom root directory: "..core.rootDir)
        end
        if not ocData.fs.exists(core.rootDir..ocData.dir) then ocData.fs.makeDirectory(core.rootDir..ocData.dir) end
    end,
    run = function()
        if #argv < 1 then
            error("Missing options, see 'mpt -h'")
        end
        local args, options = ocData.shell.parse(table.unpack(argv))
        
        if options.h or options.help then
            print("Usage:  mpt [-hRSUuy] [packages]")
            print("    -S, --sync")
            print("        Synchronize packages. Packages are installed directly")
            print("        from the remote repositories, including all dependencies")
            print("        required to run the packages. For example, pacman -S qt")
            print("        will download and install qt and all the packages it depends on.")
            print("    -R, --remove")
            print("        Remove package(s) from the system.") 
            --print("    -U, --upgrade")
            --print("        Upgrade or add package(s) to the system and install the required")
            --print("        dependencies from sync repositories. Either a URL or file path can be")
            --print("        specified. This is a ?remove-then-add? process.")
            print("    -u, --upgrades")
            print("        Upgrade all packages that are out-of-date on the")
            print("        local system. Only package versions are used to find outdated packages;")
            print("        replacements are not checked here. This is a ?remove-then-add? process.")
            print("    --root='/some/dir'")
            print("        Set alternative root directory")
            print("    -v")
            print("        More output")
            print("    -y")
            print("        Don't ask any questions, answer automatically")
            return
        end
        
        if options.v then loglevel = 0 end
        
        if options.S or options.sync then
            for _, pack in ipairs(args) do
                core.install(pack)
            end
        end
        
        if options.R or options.remove then
            if options.S or options.sync then
                error("Illegal parameters!")
            end
            for _, pack in ipairs(args) do
                core.remove(pack)
            end
        end
        
        if options.u or options.upgrades then
            core.upgrade()
        end
        
        if options.y then
            ocBackend.prompt = function()return true end
        end
        core.doWork()
    end,
    
    ----FILESYSTEM
    concat = function(...)return ocData.fs.concat(...)end,
    
    readConfig = function()
        if ocData.fs.exists(core.rootDir..ocData.configDir) then
            local f = ocData.io.open(core.rootDir..ocData.configDir, "r")
            local conf = ocData.serialization.unserialize(f:read("*all"))
            f:close()
            return conf
        else
            core.log(1, "OC ","No config, will generate")
            return core.newConfig()
        end
    end,
    readBase = function(root)
        if ocData.fs.exists((root or core.rootDir)..ocData.baseDir) then
            local f = ocData.io.open((root or core.rootDir)..ocData.baseDir, "r")
            local db = ocData.serialization.unserialize(f:read("*all"))
            f:close()
            return db
        else
            core.log(1, "OC", "No database, will generate")
            return core.newBase()
        end
    end,
    saveConfig = function()
        local f = ocData.io.open(core.rootDir..ocData.configDir, "w")
        f:write(ocData.serialization.serialize(config))
        f:close()
    end,
    saveBase = function()
        local f = ocData.io.open(core.rootDir..ocData.baseDir, "w")
        f:write(ocData.serialization.serialize(base))
        f:close()
    end,
    
    ensureParrentDirectory = function(dir)
        if ocData.fs.exists(core.rootDir..dir) and not ocData.fs.isDirectory(core.rootDir..dir) then
            error("Illegal location(already exists): "..core.rootDir..dir)
        elseif not ocData.fs.exists(core.rootDir..dir) then
            ocData.fs.makeDirectory(core.rootDir..dir)
            ocData.fs.remove(core.rootDir..dir)
        end
    end,
    
    fileExists = function(file, root)
        return ocData.filesystem.exists((root or core.rootDir)..file)
    end,
    
    copyFile = function(from, to, fromRoot)
        core.log(0, "OC", "COPY "..(fromRoot or core.rootDir)..from.." -> "..core.rootDir..to)
        ocBackend.ensureParrentDirectory(to)
        return ocData.filesystem.copy((fromRoot or core.rootDir)..from, core.rootDir..to)
    end,
    
    removeFile = function(file)
        core.log(0, "OC", "REMOVE "..core.rootDir..file)
        ocData.fs.remove(core.rootDir..file)
    end,
    
    ----NETWORK
    getFile = function(url, location)
        core.log(0, "OC", "Get "..url)
        ocBackend.ensureParrentDirectory(location)
        return ocData.wget("-q", url, core.rootDir..location)
    end,
    
    getText = function(url, post)
        core.log(0, "OC", "Get "..url)
        local sContent = ""
        local result, response = pcall(ocData.internet.request, url, post)
        if not result then
            return nil
        end
        pcall(function()
            for chunk in response do
                sContent = sContent..chunk
            end
        end)
        return sContent
    end,
    
    ----USER INTERACTION
    log = print,

    prompt = function(message)
        io.write(message)
        local p = ocData.term.read(nil,nil,nil,nil,"^[Yyn]$")
        if p:sub(1,1):upper() ~= "Y" then
            error("User stopped")
        end
    end
}

local mptFrontend
mptFrontend = {
    
    name = "MPT",
    findPackage = function(name)
        local data = backend.getText(config.frontend.mpt.api.."package/"..name)
        if data then
            local pack = load("return "..data)()
            return true, pack.dependencies, pack
        end
    end,
    
    getFilesIntoCache = function(data)
        for _, file in ipairs(data.files) do
            if not backend.fileExists(config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file) then
                backend.getFile(config.frontend.mpt.api.."file/"..data.name..file, config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file)
            end
        end
        return data.files
    end,
    
    installFiles = function(data)
        for _, file in ipairs(data.files) do
            backend.copyFile(config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file, file)
        end
        backend.removeFile(config.cacheDir.."mpt/"..data.name.."/".. data.checksum)
    end,

    removePackage = function(package)
        for _, file in ipairs(base.installed[package].data.files) do
            backend.removeFile(file)
        end
    end,

    checkUpdate = function()
        local toCheck = {}
        for pack, data in pairs(base.installed) do
            if data.frontend == mptFrontend.name then
                toCheck[pack] = base.installed[pack].data.checksum
            end
        end
        local updateResp = backend.getText(config.frontend.mpt.api.."update", toCheck)
        if updateResp then
            local updateList = load("return "..updateResp)() or {}
            local res = {}
            for _, entry in ipairs(updateList) do
                res[entry.package] = {checksum = entry.checksum}
            end
            return res
        end
    end,
    
    isOffline = false
}

local oppmFrontend = {
    name = "OPPM",
    findPackage = function(name)end,
    checkUpdate = function()
        --https://github.com/OpenPrograms/Magik6k-Programs.git/info/refs?service=git-upload-pack
        --https://github.com/schacon/igithub/blob/master/http-protocol.txt
    end,
    action = function()end,
    isOffline = false
}



local mirrorFrontend
mirrorFrontend = {
    name = "Mirror",
    start = function()
        if options.mirror then
            mirrorFrontend.base = backend.readBase(options.mirror)
        end
    end,
    findPackage = function(name)
        if mirrorFrontend.base and mirrorFrontend.base.installed[name] then
            return true, mirrorFrontend.base.installed[name].deps, mirrorFrontend.base.installed[name].data
        end
    end,
    getFilesIntoCache = function(data)
        for _, file in ipairs(data.files) do
            if not backend.fileExists(config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file) then
                --backend.getFile(config.frontend.mpt.api.."file/"..data.name..file, config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file)
                backend.copyFile(file, config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file, options.mirror)
            end
        end
        return data.files
    end,
    installFiles = function(data)
        for _, file in ipairs(data.files) do
            backend.copyFile(config.cacheDir.."mpt/"..data.name.."/".. data.checksum ..file, file)
        end
        backend.removeFile(config.cacheDir.."mpt/"..data.name.."/".. data.checksum)
    end,
    removePackage = function(package)
        for _, file in ipairs(base.installed[package].data.files) do
            backend.removeFile(file)
        end
    end,
    checkUpdate = function()end,
    action = function()end,
    isOffline = true
}

backend = ocBackend
frontends = {mptFrontend, oppmFrontend, mirrorFrontend}

core = {
    rootDir = "/",

    init = function()
        backend.init()
        base = backend.readBase()
        config = backend.readConfig()
        local usedFrontends = {}
        for _, frontend in ipairs(frontends) do
            if not options.offline or frontend.isOffline then
                usedFrontends[#usedFrontends + 1] = frontend
                if frontend.start then
                    frontend.start()
                end
            end
        end
        frontends = usedFrontends
    end,

    finalize = function()
        backend.saveBase(base)
        backend.saveConfig(config)
    end,

    newBase = function()
        return {installed = {}}
    end,
    
    newConfig = function()
        return {cacheDir="/var/lib/mpt/cache/", database="/var/lib/mpt/base.db",
            frontend={mpt={api="http://mpt.magik6k.net/api/"}}}
    end,

    log = function(level, from, ...)
        if level >= loglevel then -- 4:WTF, 3:ERROR, 2:NOTICE, 1:INFO, 0:DEBUG
            backend.log("[ "..tostring(from)..(("      "):sub(#tostring(from))), "] ", ...)
        end
    end,

    safeCall = pcall,

    ------------------------------------
    --- ACTUAL TASKS
    data = {
        install = false,
        upgrade = false,
        remove = false,
        
        --User requested packages
        userInstall = {},
        
        --List of requested packages as refs
        installList = {},
        
        --List of outdated packages
        oldPackages = {},
        
        upgradeList = {},
        
        removeList = {}
    },

    --PACKAGE:
    --          frontend  -> frontend reference
    --optional: deps      -> string list
    --optional: data      -> data for frontend
    
    upgrade = function()
        core.data.upgrade = true
        core.data.install = true
    end,

    --[[
        Adds package installation task queue
    ]]
    install = function(name)
        if not base.installed[name] then
            core.data.install = true
            core.data.userInstall[#core.data.userInstall+1] = name
        end
    end,

    remove = function(name)
        core.data.remove = true
        if not base.installed[name] then
            error("Package "..name.." is not installed!")
        end
        core.data.removeList[#core.data.removeList+1] = name
    end,

    checkPackage = function(name)
        for _, f in ipairs(frontends) do
            local exists, deps, data_ = f.findPackage(name)
            if exists then
                return {frontend = f, deps = deps, data = data_}
            end
        end
    end,

    --Get package indexes
    getPackages = function()
        for _, pack in pairs(core.data.userInstall)do
            local package = core.checkPackage(pack)
            if package then
                if not core.data.installList[pack] then
                    core.data.installList[pack] = package
                end
            else
                error("Package "..pack.." not found!")
            end
        end
    end,

    --checks if packages are up-to-date
    checkUpdate = function()
        for _, f in ipairs(frontends) do
            local updates = f.checkUpdate()
            if updates then
                for pack, data in pairs(updates) do
                    core.data.oldPackages[pack] = {frontend = f, data = data}
                end
            end
        end
    end,

    countDeps = function(package)
        local toCheck = package.deps or {}
        for _, check in pairs(toCheck) do
            if not core.data.installList[check] and not base.installed[check] then
                local dep = core.checkPackage(check)
                if dep then
                    core.data.installList[check] = dep
                    core.countDeps(dep)
                end
            end
        end
    end,
    
    promptUser = function(msg)
        if core.data.remove then
            backend.log(">> Will remove:")
            for _, packName in pairs(core.data.removeList) do
                backend.log(packName)
            end
        end
        if core.data.upgrade then
            backend.log(">> Will upgrade:")
            for packName in pairs(core.data.oldPackages) do
                backend.log(packName)
            end
        end
        if core.data.install then
            backend.log(">> Will install:")
            for packName in pairs(core.data.installList) do
                backend.log(packName)
            end
        end
        
        backend.prompt(msg)
    end,

    doWork = function()
        if core.data.install then
            core.log(1, "Install", "Checking requested packages")
            core.getPackages()
            core.log(1, "Install", "Checking dependencies")
            for _, package in pairs(core.data.installList)do
                core.countDeps(package)
            end
        end
        
        if core.data.upgrade then
            core.log(1, "Upgrade", "Looking for old packages")
            core.checkUpdate()
            core.log(1, "Upgrade", "Getting update details")
            for name, package in pairs(core.data.oldPackages)do
                local exists, deps, data = package.frontend.findPackage(name)
                if exists then
                    core.data.upgradeList[name] = {frontend = package.frontend, deps = deps, data = data}
                end
            end
            core.log(1, "Upgrade", "Checking dependencies")
            for _, package in pairs(core.data.upgradeList)do
                core.countDeps(package)
            end
        end
        
        core.promptUser("Do you want to continue?[Y/n] ")
        
        if core.data.remove then
            core.log(1, "Core", "Removing packages")
            for _, package in pairs(core.data.removeList)do
                core.log(0, "Core", "Remove "..package)
                core.frontendForName(base.installed[package].frontend).removePackage(package)
                base.installed[package] = nil
            end
        end
        
        if core.data.install or core.data.upgrade then
            core.log(1, "Core", "Downloading files")
            local oldloglevel = loglevel
            loglevel =  0
            local filelist = {}
            for _, package in pairs(core.data.installList)do
                local got = package.frontend.getFilesIntoCache(package.data)
                for _, file in ipairs(got)do
                    if not filelist[file] then
                        filelist[file] = true
                    else
                        error("File conflict detected!("..file..")")
                    end
                end
            end
            
            for _, package in pairs(core.data.upgradeList)do
                package.files = package.frontend.getFilesIntoCache(package.data)
            end
            loglevel = oldloglevel
            
            if core.data.upgrade then
                core.log(1, "Upgrade", "Removing outdated packages")
                for name, package in pairs(core.data.upgradeList)do
                    core.log(0, "Core", "Remove "..name)
                    core.frontendForName(base.installed[name].frontend).removePackage(name)
                    base.installed[name] = nil
                end
                
                for _, package in pairs(core.data.upgradeList)do
                    for _, file in ipairs(package.files)do
                        if not filelist[file] then
                            filelist[file] = true
                        else
                            error("File conflict detected!("..file..").. It is really bad now :(")
                            --...because files already got removed (:
                        end
                    end
                end
            end
            
            core.log(1, "Install", "Looking for file conflicts")
            for file in pairs(filelist)do
                if backend.fileExists(file) then
                    error("File conflict detected!("..file.."), If this package was upgraded, it's uninstalled now")
                end
            end
            
            core.log(1, "Core", "Installing packages")
            
            for name, package in pairs(core.data.upgradeList)do
                package.frontend.installFiles(package.data)
                base.installed[name] = {frontend = package.frontend.name, deps=package.deps, data=package.data}
            end
            
            for name, package in pairs(core.data.installList)do
                package.frontend.installFiles(package.data)
                base.installed[name] = {frontend = package.frontend.name, deps=package.deps, data=package.data}
            end
        end
    end,
    
    frontendForName = function(name)
        for _, f in ipairs(frontends) do
            if f.name == name then
                return f
            end
        end
    end
    
}

--------

core.log(1, "Main", "> Loading settings")
core.init()

local state, reason = pcall(backend.run)
if not state then
    io.stderr:write(reason .. "\n")
end

core.log(1, "Main", "> Saving settings")
core.finalize()





