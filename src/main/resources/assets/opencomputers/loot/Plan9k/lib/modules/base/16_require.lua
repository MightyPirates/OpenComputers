
kernel.userspace.package = {}

kernel.userspace.package.loaded = {}
kernel.userspace.package.preload = {}
kernel.userspace.package.loading = {}
kernel.userspace.package.searchers = {}

local function preloadSearcher(module)
    return kernel.userspace.package.preload[module]
end

function kernel.userspace.package.searchpath(name, path, sep, rep)
  checkArg(1, name, "string")
  checkArg(2, path, "string")
  sep = sep or '.'
  rep = rep or '/'
  sep, rep = '%' .. sep, rep
  name = string.gsub(name, sep, rep)
  local fs = kernel.modules.vfs
  local errorFiles = {}
  for subPath in string.gmatch(path, "([^;]+)") do
    subPath = string.gsub(subPath, "?", name)
    if subPath:sub(1, 1) ~= "/" and os.getenv then
      subPath = fs.concat(kernel.userspace.os.getenv("PWD") or "/", subPath)
    end
    if fs.exists(subPath) then
      local file = kernel.modules.io.io.open(subPath, "r")
      if file then
        file:close()
        return subPath
      end
    end
    table.insert(errorFiles, "\tno file '" .. subPath .. "'")
  end
  return nil, table.concat(errorFiles, "\n")
end

local function pathSearcher(module)
    local filepath, reason = kernel.userspace.package.searchpath(module, kernel.userspace.os.getenv("LIBPATH"))
    if filepath then
        local loader
        loader, reason = kernel.userspace.loadfile(filepath, "bt", setmetatable({},{__index = kernel.userspace}))
        if loader then
            local state
            state, reason = pcall(loader)
            if state then
                return reason
            end
        end
    end
    return nil, reason
end

kernel.userspace.package.searchers[#kernel.userspace.package.searchers + 1] = preloadSearcher
kernel.userspace.package.searchers[#kernel.userspace.package.searchers + 1] = pathSearcher

--TODO: possibly wrap result into metatable
kernel.userspace.require = function(module)
    --kernel.io.println(module)
    if kernel.userspace.package.loaded[module] then
        return kernel.userspace.package.loaded[module]
    else
        if kernel.userspace.package.loading[module] then
            error("Already loading "..tostring(module))
        else
            local reason
            
            kernel.userspace.package.loading[module] = true
            for _, searcher in ipairs(kernel.userspace.package.searchers) do
                local success, mod, res = pcall(searcher, module)
                if success and mod then
                    kernel.userspace.package.loading[module] = nil
                    kernel.userspace.package.loaded[module] = mod
                    return mod
                elseif (not success) and mod then
                    reason = mod
                elseif res then
                    reason = res
                end
            end
            kernel.userspace.package.loading[module] = nil
            error(string.format("Could not load module '%s': %s", module, reason or "module returned nil"))
        end
    end
end

function start()
    kernel.userspace.package.preload.package = kernel.userspace.package --TODO TODO TODO: METATABLE THIZ!!!!!!!!
    kernel.userspace.package.preload.filesystem = setmetatable({}, {__index = kernel.modules.vfs})
    kernel.userspace.package.preload.buffer = setmetatable({}, {__index = kernel.modules.buffer})
    kernel.userspace.package.preload.bit32 = setmetatable({}, {__index = kernel.userspace.bit32})
    kernel.userspace.package.preload.component = setmetatable({}, {__index = kernel.userspace.component})
    kernel.userspace.package.preload.computer = setmetatable({}, {__index = kernel.userspace.computer})
    kernel.userspace.package.preload.io = setmetatable({}, {__index = kernel.modules.io.io})
    kernel.userspace.package.preload.unicode = setmetatable({}, {__index = kernel.userspace.unicode})
    
    --TODO: fix override.. maybe
    
    setmetatable(kernel.userspace.package.preload, {
        __index = function(t, k) 
            return kernel.modules.threading.currentThread.cgroups.module.preload[k]
        end,
        __newindex = function(t, k, v)
            kernel.modules.threading.currentThread.cgroups.module.preload[k] = v
        end
    })

    setmetatable(kernel.userspace.package.loaded, {
        __index = function(t, k) 
            return kernel.modules.threading.currentThread.cgroups.module.loaded[k]
        end,
        __newindex = function(t, k, v)
            kernel.modules.threading.currentThread.cgroups.module.loaded[k] = v
        end
    })

    setmetatable(kernel.userspace.package.loading, {
        __index = function(t, k) 
            return kernel.modules.threading.currentThread.cgroups.module.loading[k]
        end,
        __newindex = function(t, k, v)
            kernel.modules.threading.currentThread.cgroups.module.loading[k] = v
        end
    })

    setmetatable(kernel.userspace.package.searchers, {
        __index = function(t, k) 
            return kernel.modules.threading.currentThread.cgroups.module.searchers[k]
        end,
        __newindex = function(t, k, v)
            kernel.modules.threading.currentThread.cgroups.module.searchers[k] = v
        end
    })
end