local process = require("process")

--Initialize coroutine library--
local _coroutine = coroutine -- real coroutine backend

_G.coroutine = {}
package.loaded.coroutine = _G.coroutine

for key,value in pairs(_coroutine) do
  if type(value) == "function" and value ~= "running" and value ~= "create" then
    _G.coroutine[key] = function(...)
      local thread = _coroutine.running()
      local info = process.info(thread)
      -- note the gc thread does not have a process info
      assert(info,"process not found for " .. tostring(thread))
      local data = info.data
      local co = data.coroutine_handler
      local handler = co[key]
      return handler(...)
    end
  else
    _G.coroutine[key] = value
  end
end

local kernel_create = _coroutine.create
local function install(path, name)
  _coroutine.create = function(f,standAlone)
    local co = kernel_create(f)
    if not standAlone then
      table.insert(process.findProcess().instances, co)
    end
    return co
  end
  local load = load
  _G.load = function(ld, source, mode, env)
    env = env or select(2, process.running())
    return load(ld, source, mode, env)
  end
  local thread = _coroutine.running()
  process.list[thread] = {
    path = path,
    command = name,
    env = _ENV,
    data =
    {
      vars={},
      io={}, --init will populate this
      coroutine_handler=setmetatable({}, {__index=_coroutine})
    },
    instances = setmetatable({}, {__mode="v"})
  }
end

install("/init.lua", "init")

