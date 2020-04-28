local process = require("process")
local fs = require("filesystem")

--Initialize coroutine library--
local _coroutine = coroutine -- real coroutine backend

_G.coroutine = setmetatable(
  {
    resume = function(co, ...)
      local proc = process.info(co)
      -- proc is nil if the process closed, natural resume will likely complain the coroutine is dead
      -- but if proc is dead and an orphan coroutine is alive, it doesn't have any proc data like stack info
      -- if the user really wants to resume it, let them
      return (proc and proc.data.coroutine_handler.resume or _coroutine.resume)(co, ...)
    end
  },
  {
    __index = function(_, key)
      return assert(process.info(_coroutine.running()), "thread has no proc").data.coroutine_handler[key]
    end
  }
)

package.loaded.coroutine = _G.coroutine

local kernel_load = _G.load
local intercept_load
intercept_load = function(source, label, mode, env)
  local prev_load = env and env.load or _G.load
  local e = env and setmetatable({
    load = function(_source, _label, _mode, _env)
      return prev_load(_source, _label, _mode, _env or env)
    end}, {
      __index = env,
      __pairs = function(...) return pairs(env, ...) end,
      __newindex = function(_, key, value) env[key] = value end,
  })
  return kernel_load(source, label, mode, e or process.info().env)
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
    return select(2, coroutine.resume(thread, ...))
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
    handles={},
    io={}, --init will populate this
    coroutine_handler = _coroutine,
    signal = error
  },
  instances = setmetatable({}, {__mode="v"})
}

-- intercept fs open
local fs_open = fs.open
fs.open = function(...)
  local fs_open_result = table.pack(fs_open(...))
  if fs_open_result[1] then
    process.addHandle(fs_open_result[1])
  end
  return table.unpack(fs_open_result, 1, fs_open_result.n)
end

