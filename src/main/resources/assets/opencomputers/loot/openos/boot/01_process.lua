local process = require("process")

--Initialize coroutine library--
local _coroutine = coroutine -- real coroutine backend

_G.coroutine = setmetatable(
  {
    resume = function(co, ...)
      local proc = process.info(co)
      -- proc is nil if the process closed, natural resume will likely complain the coroutine is dead
      -- but if proc is dead and an aborted coroutine is alive, it doesn't have any proc data like stack info
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
  if env then
    local prev_load = env.load or intercept_load
    local next_load = function(_source, _label, _mode, _env)
      return prev_load(_source, _label, _mode, _env or env)
    end
    if rawget(env, "load") then -- overwrite load 
      env.load = next_load
    else -- else it must be an __index load, or it didn't have one
      local env_mt = getmetatable(env) or {}
      local env_mt_index = env_mt.__index
      env_mt.__index = function(tbl, key)
        if key == "load" then
          return next_load
        elseif type(env_mt_index) == "table" then
          return env_mt_index[key]
        elseif env_mt_index then
          return env_mt_index(tbl, key)
        end
        return nil
      end
      setmetatable(env, env_mt)
    end
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
    io={}, --init will populate this
    coroutine_handler = _coroutine
  },
  instances = setmetatable({}, {__mode="v"})
}
