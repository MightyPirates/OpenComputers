--[[ Set up the global environment we make available to userspace programs. ]]
_G._G = {
  -- Top level values. The selection of kept methods rougly follows the list
  -- as available on the Lua wiki here: http://lua-users.org/wiki/SandBoxes
  -- Some entries have been kept although they are marked as unsafe on the
  -- wiki, due to how we set up our environment: we clear the globals table,
  -- so it does not matter if user-space functions gain access to the global
  -- environment. We pretty much give all user-space code full control to
  -- mess up the VM on the Lua side, we just want to make sure they can never
  -- reach out to the Java side in an unintended way.
  ["assert"] = _G.assert,
  ["error"] = _G.error,

  ["call"] = _G.call,
  ["pcall"] = _G.pcall,
  ["xpcall"] = _G.xpcall,

  ["ipairs"] = _G.ipairs,
  ["next"] = _G.next,
  ["pairs"] = _G.pairs,

  ["rawequal"] = _G.rawequal,
  ["rawget"] = _G.rawget,
  ["rawset"] = _G.rawset,

  ["select"] = _G.select,
  ["unpack"] = _G.unpack,
  ["type"] = _G.type,
  ["tonumber"] = _G.tonumber,
  ["tostring"] = _G.tostring,

  -- Loadstring is OK because it's OK that the loaded chunk is in the global
  -- environment as mentioned in the comment above.
  ["loadstring"] = _G.loadstring,

  -- We don't care what users do with metatables. The only raised concern was
  -- about breaking an environment, and we don't care about that.
  ["getmetatable"] = _G.getmetatable,
  ["setmetatable"] = _G.setmetatable,

  -- Same goes for environment setters themselves. We do use local environments
  -- for loaded scripts, but that's more for convenience than for control.
  ["getfenv"] = _G.getfenv,
  ["setfenv"] = _G.setfenv,

  -- Custom print that actually writes to the screen buffer.
  --["print"] = _G.print,

  ["coroutine"] = {
    ["create"] = _G.coroutine.create,
    ["resume"] = _G.coroutine.resume,
    ["running"] = _G.coroutine.running,
    ["status"] = _G.coroutine.status,
    ["wrap"] = _G.coroutine.wrap,
    ["yield"] = _G.coroutine.yield
  },

  ["string"] = {
    ["byte]"] = _G.string.byte,
    ["char"] = _G.string.char,
    ["dump"] = _G.string.dump,
    ["find"] = _G.string.find,
    ["format"] = _G.string.format,
    ["gmatch"] = _G.string.gmatch,
    ["gsub"] = _G.string.gsub,
    ["len"] = _G.string.len,
    ["lower"] = _G.string.lower,
    ["match"] = _G.string.match,
    ["rep"] = _G.string.rep,
    ["reverse"] = _G.string.reverse,
    ["sub"] = _G.string.sub,
    ["upper"] = _G.string.upper
  },

  ["table"] = {
    ["concat"] = _G.table.concat,
    ["insert"] = _G.table.insert,
    ["maxn"] = _G.table.maxn,
    ["remove"] = _G.table.remove,
    ["sort"] = _G.table.sort
  },

  ["math"] = {
    ["abs"] = _G.math.abs,
    ["acos"] = _G.math.acos,
    ["asin"] = _G.math.asin,
    ["atan"] = _G.math.atan,
    ["atan2"] = _G.math.atan2,
    ["ceil"] = _G.math.ceil,
    ["cos"] = _G.math.cos,
    ["cosh"] = _G.math.cosh,
    ["deg"] = _G.math.deg,
    ["exp"] = _G.math.exp,
    ["floor"] = _G.math.floor,
    ["fmod"] = _G.math.fmod,
    ["frexp"] = _G.math.frexp,
    ["huge"] = _G.math.huge,
    ["ldexp"] = _G.math.ldexp,
    ["log"] = _G.math.log,
    ["log10"] = _G.math.log10,
    ["max"] = _G.math.max,
    ["min"] = _G.math.min,
    ["modf"] = _G.math.modf,
    ["pi"] = _G.math.pi,
    ["pow"] = _G.math.pow,
    ["rad"] = _G.math.rad,
    -- TODO Check if different Java LuaState's interfere via this. If so we
    --      may have to create a custom random instance to replace the built
    --      in random functionality of Lua.
    ["random"] = _G.math.random,
    ["randomseed"] = _G.math.randomseed,
    ["sin"] = _G.math.sin,
    ["sinh"] = _G.math.sinh,
    ["sqrt"] = _G.math.sqrt,
    ["tan"] = _G.math.tan,
    ["tanh"] = _G.math.tanh
  },

  ["os"] = {
    ["clock"] = _G.os.clock,
    ["date"] = _G.os.date,
    ["difftime"] = _G.os.difftime,
    ["time"] = _G.os.time
  }
}