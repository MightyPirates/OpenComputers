--[[
  This script sets up the environment to properly allow persisting the state
  after this point, regardless of what the following code does.
]]

-- Keep a backup of globals that we use for direct access.
local g = {
  assert = assert,
  error = error,
  next = next,
  pcall = pcall,
  type = type,
  unpack = unpack,
  coroutine = {
    create = coroutine.create,
    resume = coroutine.resume,
    status = coroutine.status,
    yield = coroutine.yield
  },
  debug = {
    traceback = debug.traceback
  }
}
_G.debug = nil

--[[
  Replace functions known to call back to Lua with ones writtin in Lua. This
  is necessary for yielding to work in Lua 5.1, where it's not possible to
  yield across C stack frames. Note that even though this works in 5.2, it's
  probably nigh impossible to serialize such yielded coroutines, exactly
  because of the C stack frames.
]]

-- Wrap all native functions with a wrapper that generates a traceback.
local function wrapper(f, name)
  return function(...)
    local result = {g.pcall(f, ...)}
    if result[1] then
      return g.unpack(result, 2)
    else
      g.error(g.debug.traceback(result[2], 2), 2)
    end
  end
end
local function wrap(t)
  local walked = {}
  local towrap = {}
  local function walk(t)
    for k, v in pairs(t) do
      if not walked[v] then
        walked[v] = true
        if type(v) == "function" then
          table.insert(towrap, {t, k, v})
        elseif type(v) == "table" then
          walk(v)
        end
      end
    end
  end
  walk(t)
  for _, v in ipairs(towrap) do
    local t, k, v = unpack(v)
    t[k] = wrapper(v, k)
  end
end
wrap(_G)

function _G.xpcall(...)
  local args = {...}
  g.assert(#args > 1, "bad argument #2 to 'xpcall' (value expected)")
  local f = args[1]
  local msgh = args[2]
  local result
  if g.type(f) == "function" then
    local co = g.coroutine.create(f)
    result = {g.coroutine.resume(co, unpack(args, 3))}
    while g.coroutine.status(co) ~= "dead" do
      result = {g.coroutine.resume(co, g.coroutine.yield(g.unpack(result, 2)))}
    end
  else
    result = {false, "attempt to call a " .. g.type(f) .. " value"}
  end
  if result[1] then
    return g.unpack(result)
  end
  if g.type(msgh) == "function" then
    local ok, message = g.pcall(msgh, g.unpack(result, 2))
    if ok then
      return false, message
    end
  end
  return false, "error in error handling"
end

g.xpcall = xpcall
local function passthrough(msg) return msg end
function _G.pcall(f, ...)
  return g.xpcall(f, passthrough, ...)
end

function _G.ipairs(...)
  local args = {...}
  g.assert (#args > 0, "bad argument #1 to 'ipairs' (table expected, got no value)")
  local t = args[1]
  g.assert(g.type(t) == "table", "bad argument #1 to 'ipairs' (table expected, got" .. g.type(t) .. ")")
  return function(t, idx)
    idx = idx + 1
    local value = t[idx]
    if value then
      return idx, value
    end
  end, t, 0
end

function _G.pairs(...)
  local args = {...}
  g.assert (#args > 0, "bad argument #1 to 'pairs' (table expected, got no value)")
  local t = args[1]
  g.assert(g.type(t) == "table", "bad argument #1 to 'pairs' (table expected, got" .. g.type(t) .. ")")
  return g.next, t, nil
end

function _G.coroutine.wrap(...)
  local args = {...}
  g.assert (#args > 0, "bad argument #1 to 'wrap' (function expected, got no value)")
  local f = args[1]
  g.assert(g.type(f) == "function", "bad argument #1 to 'wrap' (function expected, got" .. g.type(f) .. ")")
  local co = g.coroutine.create(f)
  return function(...)
    local result = {g.coroutine.resume(co, ...)}
    if result[1] then
      return g.unpack(result, 2)
    else
      g.error(result[2], 2)
    end
  end
end

--[[ Build Pluto's permanent value tables. ]]
local perms, uperms = {[_ENV] = "_ENV"}, {["_ENV"] = _ENV}

-- Flattens nested tables to concatenate field names with points. This is done
-- to ensure we don't have any duplicates and to get the perm "names".
local function store(t)
  if not t then return end
  local done = {}
  local function flattenAndStore(k, v)
    if type(v) == "table" then
      if not done[v] then
        done[v] = true
        local prefix = k .. "."
        for k, v in pairs(v) do
          flattenAndStore(prefix .. k, v)
        end
      end
    elseif type(v) == "function" then
      assert(uperms[k] == nil, "duplicate permanent value named " .. k)
      -- If we have aliases its enough to store the value once.
      if not perms[v] then
        perms[v] = k
        uperms[k] = v
      end
    end
  end
  for k, v in pairs(t) do
    flattenAndStore(k, v)
  end
end
store(_G)
store(...)

return perms, uperms