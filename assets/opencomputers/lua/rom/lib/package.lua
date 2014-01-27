local package = {}

package.path = "./?.lua;/lib/?.lua;/usr/lib/?.lua;/home/lib/?.lua"

local loading = {}

local loaded = {
  ["_G"] = _G,
  ["bit32"] = bit32,
  ["coroutine"] = coroutine,
  ["io"] = io,
  ["math"] = math,
  ["os"] = os,
  ["package"] = package,
  ["string"] = string,
  ["table"] = table
}
package.loaded = loaded

local preload = {}
package.preload = preload

package.searchers = {}

function package.searchpath(name, path, sep, rep)
  checkArg(1, name, "string")
  checkArg(2, path, "string")
  sep = sep or '.'
  rep = rep or '/'
  sep, rep = '%' .. sep, '%' .. rep
  name = string.gsub(name, sep, rep)
  local errorFiles = {}
  for subPath in string.gmatch(path, "([^;]+)") do
    subPath = string.gsub(subPath, "?", name)
    if loaded.shell then
      subPath = require("shell").resolve(subPath)
    end
    if require("filesystem").exists(subPath) then
      local file = io.open(subPath, "r")
      if file then
        file:close()
        return subPath
      end
    end
    table.insert(errorFiles, "\tno file '" .. subPath .. "'")
  end
  return nil, table.concat(errorFiles, "\n")
end

local function preloadSearcher(module)
  if preload[module] ~= nil then
    return preload[module]
  else
    return "\tno field package.preload['" .. module .. "']"
  end
end

local function pathSearcher(module)
  local filepath, reason = package.searchpath(module, package.path)
  if filepath then
    local loader, reason = loadfile(filepath, "bt", _G)
    if loader then
      return loader, filepath
    else
      return reason
    end
  else
    return reason
  end
end

table.insert(package.searchers, preloadSearcher)
table.insert(package.searchers, pathSearcher)

function require(module)
  checkArg(1, module, "string")
  if loaded[module] then
    return loaded[module]
  elseif not loading[module] then
    loading[module] = true
    local loader, value, errorMsg = nil, nil, {"module '" .. module .. "' not found:"}
    for i=1, #package.searchers do
      local f, extra = package.searchers[i](module)
      if f and type(f) ~= "string" then
        loader = f
        value = extra
        break
      elseif f then
        table.insert(errorMsg, f)
      end
    end
    if loader then
      local result = loader(module, value)
      if result then
        loaded[module] = result
      elseif not loaded[module] then
        loaded[module] = true
      end
      loading[module] = false
      return loaded[module]
    else
      loading[module] = false
      error(table.concat(errorMsg, "\n"))
    end
  else
    error("already loading: " .. module)
  end
end

-------------------------------------------------------------------------------

return package
