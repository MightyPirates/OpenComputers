local package = {}

package.path = "/lib/?.lua;/usr/lib/?.lua;/home/lib/?.lua;./?.lua;/lib/?/init.lua;/usr/lib/?/init.lua;/home/lib/?/init.lua;./?/init.lua"

local loading = {}

local loaded = {
  ["_G"] = _G,
  ["bit32"] = bit32,
  ["coroutine"] = coroutine,
  ["math"] = math,
  ["os"] = os,
  ["package"] = package,
  ["string"] = string,
  ["table"] = table
}
package.loaded = loaded

local preload = {}
package.preload = preload

local delayed = {}
package.delayed = delayed

package.searchers = {}

function package.searchpath(name, path, sep, rep)
  checkArg(1, name, "string")
  checkArg(2, path, "string")
  sep = sep or '.'
  rep = rep or '/'
  sep, rep = '%' .. sep, rep
  name = string.gsub(name, sep, rep)
  local fs = require("filesystem")
  local errorFiles = {}
  for subPath in string.gmatch(path, "([^;]+)") do
    subPath = string.gsub(subPath, "?", name)
    if subPath:sub(1, 1) ~= "/" and os.getenv then
      subPath = fs.concat(os.getenv("PWD") or "/", subPath)
    end
    if fs.exists(subPath) then
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

local delay_data = {}
local delay_tools = setmetatable({},{__mode="v"})

function delay_data.__index(tbl,key)
  local lookup = delay_data.lookup or loadfile(package.searchpath("tools/delayLookup", package.path), "bt", _G)
  delay_data.lookup = lookup
  return lookup(delay_data, tbl, key)
end
delay_data.__pairs = delay_data.__index -- nil key acts like pairs

local function delaySearcher(module)
  if not delayed[module] then
    return "\tno field package.delayed['" .. module .. "']"
  end
  local filepath, reason = package.searchpath(module, package.path)
  if not filepath then
    return reason
  end
  local parser = delay_tools.parser or loadfile(package.searchpath("tools/delayParse", package.path), "bt", _G)
  delay_tools.parser = parser
  local loader, reason = parser(filepath,delay_data)
  return loader, reason
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
table.insert(package.searchers, delaySearcher)
table.insert(package.searchers, pathSearcher)

function require(module)
  checkArg(1, module, "string")
  if loaded[module] ~= nil then
    return loaded[module]
  elseif not loading[module] then
    loading[module] = true
    local loader, value, errorMsg = nil, nil, {"module '" .. module .. "' not found:"}
    for i = 1, #package.searchers do
      -- the pcall is mostly for out of memory errors
      local ok, f, extra = pcall(package.searchers[i], module)
      if not ok then
        table.insert(errorMsg, "\t" .. f)
      elseif f and type(f) ~= "string" then
        loader = f
        value = extra
        break
      elseif f then
        table.insert(errorMsg, f)
      end
    end
    if loader then
      local success, result = pcall(loader, module, value)
      loading[module] = false
      if not success then
        error(result, 2)
      end
      if result then
        loaded[module] = result
      elseif not loaded[module] then
        loaded[module] = true
      end
      return loaded[module]
    else
      loading[module] = false
      error(table.concat(errorMsg, "\n"), 2)
    end
  else
    error("already loading: " .. module .. debug.traceback(), 2)
  end
end

-------------------------------------------------------------------------------

return package
