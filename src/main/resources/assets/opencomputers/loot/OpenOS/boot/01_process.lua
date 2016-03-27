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

local init_thread = _coroutine.running()
local init_load = _G.load

_G.load = function(ld, source, mode, env)
  env = env or select(2, process.running())
  return init_load(ld, source, mode, env)
end

local kernel_create = _coroutine.create
_coroutine.create = function(f,standAlone)
  local co = kernel_create(f)
  if not standAlone then
    table.insert(process.findProcess().instances, co)
  end
  return co
end

_coroutine.wrap = function(f)
  local thread = coroutine.create(f)
  return function(...)
    local result_pack = table.pack(coroutine.resume(thread, ...))
    local result, reason = result_pack[1], result_pack[2]
    assert(result, reason)
    return select(2, table.unpack(result_pack))
  end
end

process.list[init_thread] = {
  path = "/init.lua",
  command = "init",
  env = _ENV,
  data =
  {
    vars={},
    io={}, --init will populate this
    coroutine_handler=setmetatable({}, {__index=_coroutine})
  },
  instances = setmetatable({}, {__mode="v"})
}
