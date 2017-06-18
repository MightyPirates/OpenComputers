local process = require("process")

--Initialize coroutine library--
local _coroutine = coroutine -- real coroutine backend

_G.coroutine = setmetatable({}, {__index = function(_, key)
  return assert(process.info(_coroutine.running()), "thread has no proc").data.coroutine_handler[key]
end})

package.loaded.coroutine = _G.coroutine

local kernel_load = _G.load
local intercept_load
intercept_load = function(source, label, mode, env)
  if env then
    local loader = setmetatable(
    {
      env = env,
      load = intercept_load,
    },
    {__call = function(tbl, _source, _label, _mode, _env)
      return tbl.load(_source, _label, _mode, _env or tbl.env)
    end})
    if env.load and (type(env.load) ~= "table" or env.load.load ~= intercept_load) then
      loader.load = env.load
    end
    env.load = loader
  end
  return kernel_load(source, label, mode, env or process.info().env)
end
_G.load = intercept_load

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

local init_thread = _coroutine.running()
process.list[init_thread] = {
  path = "/init.lua",
  command = "init",
  env = _ENV,
  data =
  {
    vars={},
    io={}, --init will populate this
    coroutine_handler = _coroutine
  },
  instances = setmetatable({}, {__mode="v"})
}
