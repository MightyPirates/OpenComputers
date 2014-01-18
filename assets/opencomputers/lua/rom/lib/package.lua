package = {}

package.path = "./?.lua;/lib/?.lua;/usr/lib/?.lua;/home/lib/?.lua"

local loading = {}

local loaded = {}
package.loaded = setmetatable({}, {__index = loaded, __newindex = function(t, k, v) loaded[k] = v end})

local preload = {}
package.preload = setmetatable({}, {__index = preload, __newindex = function(t, k, v) preload[k] = v end})

package.searchers = {}

function package.searchpath(name, path, sep, rep)
  assert(name, "You must specify a name!")
  assert(path, "You must specify a path!")
  sep = sep or '.'
  rep = rep or '/'
  sep, rep = '%'..sep, '%'..rep
  name = string.gsub(name, sep, rep)
  local errorFiles = {}
  for sPath in string.gmatch(path, "([^;]+)") do
    sPath = string.gsub(sPath, "?", name)
    sPath = shell.resolve(sPath)
    if fs.exists(sPath) then
      local file = io.open(sPath, "r")
      if file then
        file:close()
        return sPath
      else
        table.insert(errorFiles, "Tried to open: "..sPath)
      end
    end
  end
  return nil, table.concat(errorFiles, " ")
end

local function preloadSearcher(module)
  return preload[module]
end

local function pathSearcher(module)
  local sPath, err = package.searchpath(module, package.path)
  if sPath then
    return loadfile(sPath, "bt", _G), sPath
  else
    return err
  end
end

table.insert(package.searchers, preloadSearcher)
table.insert(package.searchers, pathSearcher)

function require(module)
  if loaded[module] then
    return loaded[module]
  elseif not loading[module] then
    loading[module] = true
    local loader, value, errorMsg = nil, nil, {"Could not load "..module}
    for i=1, #package.searchers do
      local f, extra = package.searchers[i](module)
      if f and type(f) ~= "string" then
        loader = f
        value = extra
        break
      else
        table.insert(errorMsg, f)
      end
    end
    if loader then
      local ret = loader(module, value)
      if ret then
        loaded[module] = ret
      elseif not loaded[module] then
        loaded[module] = true
      end
      loading[module] = false
      return loaded[module]
    else
      loading[module] = false
      error(table.concat(errorMsg, '\n'))
    end
  else
    error "Already loading: "..module
  end
end
