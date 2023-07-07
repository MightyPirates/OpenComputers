local hookInterval = 10000
local function calcHookInterval()
	local bogomipsDivider = 0.05
	local bogomipsDeadline = computer.realTime() + bogomipsDivider
	local ipsCount = 0
	local bogomipsBusy = true
	local function calcBogoMips()
		ipsCount = ipsCount + hookInterval
		if computer.realTime() > bogomipsDeadline then
			bogomipsBusy = false
		end
	end
	-- The following is a bit of nonsensical-seeming code attempting
	-- to cover Lua's VM sufficiently for the IPS calculation.
	local bogomipsTmpA = {{["b"]=3, ["d"]=9}}
	local function c(k)
		if k <= 2 then
			bogomipsTmpA[1].d = k / 2.0
		end
	end
	debug.sethook(calcBogoMips, "", hookInterval)
	while bogomipsBusy do
		local st = ""
		for k=2,4 do
			st = st .. "a" .. k
			c(k)
			if k >= 3 then
				bogomipsTmpA[1].b = bogomipsTmpA[1].b * (k ^ k)
			end
		end
	end
	debug.sethook()
	return ipsCount / bogomipsDivider
end

local ipsCount = calcHookInterval()
-- Since our IPS might still be too generous (hookInterval needs to run at most
-- every 0.05 seconds), we divide it further by 10 relative to that.
hookInterval = (ipsCount * 0.005)
if hookInterval < 1000 then hookInterval = 1000 end

local deadline = math.huge
local hitDeadline = false
local tooLongWithoutYielding = setmetatable({},  { __tostring = function() return "too long without yielding" end})
local function checkDeadline()
  if computer.realTime() > deadline then
    debug.sethook(coroutine.running(), checkDeadline, "", 1)
    if not hitDeadline then
      deadline = deadline + 0.5
    end
    hitDeadline = true
    error(tooLongWithoutYielding)
  end
end
local function pcallTimeoutCheck(...)
  local ok, timeout = ...
  if rawequal(timeout, tooLongWithoutYielding) then
    return ok, tostring(tooLongWithoutYielding)
  end
  return ...
end

-------------------------------------------------------------------------------

local isLuaOver54 = _VERSION:match("5.4")
local isLuaOver53 = isLuaOver54 or _VERSION:match("5.3")

local function checkArg(n, have, ...)
  have = type(have)
  local function check(want, ...)
    if not want then
      return false
    else
      return have == want or check(...)
    end
  end
  if not check(...) then
    local msg = string.format("bad argument #%d (%s expected, got %s)",
                              n, table.concat({...}, " or "), have)
    error(msg, 3)
  end
end

-------------------------------------------------------------------------------

--[[ This is pretty much a straight port of Lua's pattern matching code from
     the standard PUC-Rio C implementation. We want to have this in plain Lua
     for the sandbox, so that timeouts also apply while matching stuff, which
     can take a looong time for certain "evil" patterns.
     It passes the pattern matching unit tests from Lua 5.2's test suite, so
     that should be good enough. ]]
do
  local CAP_UNFINISHED = -1
  local CAP_POSITION = -2
  local L_ESC = '%'
  local SPECIALS = "^$*+?.([%-"
  local SHORT_STRING = 500 -- use native implementations for short strings

  local string_find, string_lower, string_match, string_gmatch, string_gsub =
        string.find, string.lower, string.match, string.gmatch, string.gsub

  local match -- forward declaration

  local strptr
  local strptr_mt = {__index={
    step = function(self, count)
      self.pos = self.pos + (count or 1)
      return self
    end,
    head = function(self, len)
      return string.sub(self.data, self.pos, self.pos + (len or self:len()) - 1)
    end,
    len = function(self)
      return #self.data - (self.pos - 1)
    end,
    char = function(self, offset)
      local pos = self.pos + (offset or 0)
      if pos == #self.data + 1 then
        return "\0"
      end
      return string.sub(self.data, pos, pos)
    end,
    copy = function(self, offset)
      return strptr(self.data, self.pos + (offset or 0))
    end
    },
    __add = function(a, b)
      if type(b) == "table" then
        return a.pos + b.pos
      else
        return a:copy(b)
      end
    end,
    __sub = function(a, b)
      if type(b) == "table" then
        return a.pos - b.pos
      else
        return a:copy(-b)
      end
    end,
    __eq = function(a, b)
      return a.data == b.data and a.pos == b.pos
    end,
    __lt = function(a, b)
      assert(a.data == b.data)
      return a.pos < b.pos
    end,
    __le = function(a, b)
      assert(a.data == b.data)
      return a.pos <= b.pos
    end
  }
  function strptr(s, pos)
    return setmetatable({
      data = s,
      pos = pos or 1
    }, strptr_mt)
  end

  local function islower(b) return b >= 'a' and b <= 'z' end
  local function isupper(b) return b >= 'A' and b <= 'Z' end
  local function isalpha(b) return islower(b) or isupper(b) end
  local function iscntrl(b) return b <= '\007' or (b >= '\010' and b <= '\017') or (b >= '\020' and b <= '\027') or (b >= '\030' and b <= '\037' and b ~= ' ') or b == '\177' end
  local function isdigit(b) return b >= '0' and b <= '9' end
  local function ispunct(b) return (b >= '{' and b <= '~') or (b == '`') or (b >= '[' and b <= '_') or (b == '@') or (b >= ':' and b <= '?') or (b >= '(' and b <= '/') or (b >= '!' and b <= '\'') end
  local function isspace(b) return b == '\t' or b == '\n' or b == '\v' or b == '\f' or b == '\r' or b == ' ' end
  local function isalnum(b) return isalpha(b) or isdigit(b) end
  local function isxdigit(b) return isdigit(b) or (b >= 'a' and b <= 'f') or (b >= 'A' and b <= 'F') end
  local function isgraph(b) return not iscntrl(b) and not isspace(b) end

  -- translate a relative string position: negative means back from end
  local function posrelat(pos, len)
    if pos >= 0 then return pos
    elseif -pos > len then return 0
    else return len + pos + 1
    end
  end

  local function check_capture(ms, l)
    l = l - '1'
    if l < 0 or l >= ms.level or ms.capture[l].len == CAP_UNFINISHED then
      error("invalid capture index %" .. (l + 1))
    end
    return l
  end

  local function capture_to_close(ms)
    local level = ms.level
    while level > 0 do
      level = level - 1
      if ms.capture[level].len == CAP_UNFINISHED then
        return level
      end
    end
    return error("invalid pattern capture")
  end

  local function classend(ms, p)
    local p0 = p:char() p = p:copy(1)
    if p0 == L_ESC then
      if p == ms.p_end then
        error("malformed pattern (ends with %)")
      end
      return p:step(1)
    elseif p0 == '[' then
      if p:char() == '^' then
        p:step()
      end
      repeat  -- look for a `]'
        if p == ms.p_end then
          error("malformed pattern (missing ])")
        end
        p:step()
        if p:char(-1) == L_ESC then
          if p < ms.p_end then
            p:step()  -- skip escapes (e.g. `%]')
          end
        end
      until p:char() == ']'
      return p:step()
    else
      return p
    end
  end

  local function match_class(c, cl)
    local res
    local cll = string_lower(cl)
    if cll == 'a' then res = isalpha(c)
    elseif cll == 'c' then res = iscntrl(c)
    elseif cll == 'd' then res = isdigit(c)
    elseif cll == 'g' then res = isgraph(c)
    elseif cll == 'l' then res = islower(c)
    elseif cll == 'p' then res = ispunct(c)
    elseif cll == 's' then res = isspace(c)
    elseif cll == 'u' then res = isupper(c)
    elseif cll == 'w' then res = isalnum(c)
    elseif cll == 'x' then res = isxdigit(c)
    elseif cll == 'z' then res = c == '\0'  -- deprecated option
    else return cl == c
    end
    if islower(cl) then return res
    else return not res
    end
  end

  local function matchbracketclass(c, p, ec)
    local sig = true
    p = p:copy(1)
    if p:char() == '^' then
      sig = false
      p:step()  -- skip the `^'
    end
    while p < ec do
      if p:char() == L_ESC then
        p:step()
        if match_class(c, p:char()) then
          return sig
        end
      elseif p:char(1) == '-' and p + 2 < ec then
        p:step(2)
        if p:char(-2) <= c and c <= p:char() then
          return sig
        end
      elseif p:char() == c then
        return sig
      end
      p:step()
    end
    return not sig
  end

  local function singlematch(ms, s, p, ep)
    if s >= ms.src_end then
      return false
    end
    local p0 = p:char()
    if p0 == '.' then return true -- matches any char
    elseif p0 == L_ESC then return match_class(s:char(), p:char(1))
    elseif p0 == '[' then return matchbracketclass(s:char(), p, ep:copy(-1))
    else return p:char() == s:char()
    end
  end

  local function matchbalance(ms, s, p)
    if p >= ms.p_end - 1 then
      error("malformed pattern (missing arguments to %b)")
    end
    if s:char() ~= p:char() then return nil end
    local b = p:char()
    local e = p:char(1)
    local cont = 1
    s = s:copy()
    while s:step() < ms.src_end do
      if s:char() == e then
        cont = cont - 1
        if cont == 0 then return s:step() end
      elseif s:char() == b then
        cont = cont + 1
      end
    end
    return nil  -- string ends out of balance
  end

  local function max_expand(ms, s, p, ep)
    local i = 0  -- counts maximum expand for item
    while singlematch(ms, s:copy(i), p, ep) do
      i = i + 1
    end
    -- keeps trying to match with the maximum repetitions
    while i >= 0 do
      local res = match(ms, s:copy(i), ep:copy(1))
      if res then return res end
      i = i - 1  -- else didn't match; reduce 1 repetition to try again
    end
    return nil
  end

  local function min_expand(ms, s, p, ep)
    s = s:copy()
    while true do
      local res = match(ms, s, ep:copy(1))
      if res ~= nil then
        return res
      elseif singlematch(ms, s, p, ep) then
        s:step()  -- try with one more repetition
      else return nil
      end
    end
  end

  local function start_capture(ms, s, p, what)
    local level = ms.level
    ms.capture[level] = ms.capture[level] or {}
    ms.capture[level].init = s:copy()
    ms.capture[level].len = what
    ms.level = level + 1
    local res = match(ms, s, p)
    if res == nil then  -- match failed?
      ms.level = ms.level - 1  -- undo capture
    end
    return res
  end

  local function end_capture(ms, s, p)
    local l = capture_to_close(ms)
    ms.capture[l].len = s - ms.capture[l].init  -- close capture
    local res = match(ms, s, p)
    if res == nil then  -- match failed?
      ms.capture[l].len = CAP_UNFINISHED  -- undo capture
    end
    return res
  end

  local function match_capture(ms, s, l)
    l = check_capture(ms, l)
    local len = ms.capture[l].len
    if ms.src_end - s >= len and
       ms.capture[l].init:head(len) == s:head(len)
    then
      return s:copy(len)
    else return nil
    end
  end

  function match(ms, s, p)
    s = s:copy()
    p = p:copy()
    ::init:: -- using goto's to optimize tail recursion
    if p ~= ms.p_end then
      local p0 = p:char()
      if p0 == '(' then  -- start capture
        if p:char(1) == ')' then  -- position capture?
          s = start_capture(ms, s, p:copy(2), CAP_POSITION)
        else
          s = start_capture(ms, s, p:copy(1), CAP_UNFINISHED)
        end
        goto brk
      elseif p0 == ')' then  -- end capture
        s = end_capture(ms, s, p:copy(1))
        goto brk
      elseif p0 == '$' then
        if p + 1 ~= ms.p_end then  -- is the `$' the last char in pattern?
          goto dflt  -- no; go to default
        end
        s = (s == ms.src_end) and s or nil  -- check end of string
        goto brk
      elseif p0 == L_ESC then  -- escaped sequences not in the format class[*+?-]?
        local p1 = p:char(1)
        if p1 == 'b' then  -- balanced string?
          s = matchbalance(ms, s, p:copy(2))
          if s ~= nil then
            p:step(4)
            goto init  -- return match(ms, s, p + 4)
          end
          -- else fail (s == nil)
        elseif p1 == 'f' then  -- frontier?
          p:step(2)
          if p:char() ~= '[' then
            error("missing [ after %f in pattern")
          end
          local ep = classend(ms, p)  -- points to what is next
          local previous = (s == ms.src_init) and '\0' or s:char(-1)
          if not matchbracketclass(previous, p, ep:copy(-1)) and
             matchbracketclass(s:char(), p, ep:copy(-1))
          then
            p = ep
            goto init  -- return match(ms, s, ep)
          end
          s = nil  -- match failed
        elseif isdigit(p:char(1)) then  -- capture results (%0-%9)?
          s = match_capture(ms, s, p:char(1))
          if s ~= nil then
            p:step(2)
            goto init  -- return match(ms, s, p + 2)
          end
        else
          goto dflt
        end
        goto brk
      end
      ::dflt:: do
        local ep = classend(ms, p)  -- points to what is next
        local ep0 = ep:char()
        if not singlematch(ms, s, p, ep) then
          if ep0 == '*' or ep0 == '?' or ep0 == '-' then  -- accept empty?
            p = ep:copy(1)
            goto init  -- return match(ms, s, ep + 1)
          else  -- '+' or no suffix
            s = nil  -- fail
          end
        else  -- matched once
          if ep0 == '?' then  -- optional
            local res = match(ms, s:copy(1), ep:copy(1))
            if res ~= nil then
              s = res
            else
              p = ep:copy(1)
              goto init  -- else return match(ms, s, ep + 1)
            end
          elseif ep0 == '+' then  -- 1 or more repetitions
            s = max_expand(ms, s:copy(1), p, ep)  -- 1 match already done
          elseif ep0 == '*' then  -- 0 or more repetitions
            s = max_expand(ms, s, p, ep)
          elseif ep0 == '-' then  -- 0 or more repetitions (minimum)
            s = min_expand(ms, s, p, ep)
          else
            s:step()
            p = ep
            goto init  -- else return match(ms, s+1, ep);
          end
        end
      end
      ::brk::
    end
    return s
  end

  local function push_onecapture(ms, i, s, e)
    if i >= ms.level then
      if i == 0 then  -- ms->level == 0, too
        return s:head(e - s)  -- add whole match
      else
        error("invalid capture index")
      end
    else
      local l = ms.capture[i].len;
      if l == CAP_UNFINISHED then error("unfinished capture") end
      if l == CAP_POSITION then
        return ms.capture[i].init - ms.src_init + 1
      else
        return ms.capture[i].init:head(l)
      end
    end
  end

  local function push_captures(ms, s, e)
    local nlevels = (ms.level == 0 and s) and 1 or ms.level
    local captures = {}
    for i = 0, nlevels - 1 do
      table.insert(captures, push_onecapture(ms, i, s, e))
    end
    return table.unpack(captures)
  end

  -- check whether pattern has no special characters
  local function nospecials(p)
    for i = 1, #p do
      for j = 1, #SPECIALS do
        if p:sub(i, i) == SPECIALS:sub(j, j) then
          return false
        end
      end
    end
    return true
  end

  local function str_find_aux(str, pattern, init, plain, find)
    checkArg(1, str, "string")
    checkArg(2, pattern, "string")
    checkArg(3, init, "number", "nil")

    if #str < SHORT_STRING then
      return (find and string_find or string_match)(str, pattern, init, plain)
    end

    local s = strptr(str)
    local p = strptr(pattern)
    local init = posrelat(init or 1, #str)
    if init < 1 then init = 1
    elseif init > #str + 1 then  -- start after string's end?
      return nil  -- cannot find anything
    end
    -- explicit request or no special characters?
    if find and (plain or nospecials(pattern)) then
      -- do a plain search
      local s2 = string_find(str, pattern, init, true)
      if s2 then
        return s2-s.pos + 1, s2 - s.pos + p:len()
      end
    else
      local s1 = s:copy(init - 1)
      local anchor = p:char() == '^'
      if anchor then p:step() end
      local ms = {
        src_init = s,
        src_end = s:copy(s:len()),
        p_end = p:copy(p:len()),
        capture = {}
      }
      repeat
        ms.level = 0
        local res = match(ms, s1, p)
        if res ~= nil then
          if find then
            return s1.pos - s.pos + 1, res.pos - s.pos, push_captures(ms, nil, nil)
          else
            return push_captures(ms, s1, res)
          end
        end
      until s1:step() > ms.src_end or anchor
    end
    return nil  -- not found
  end

  local function str_find(s, pattern, init, plain)
    return str_find_aux(s, pattern, init, plain, true)
  end

  local function str_match(s, pattern, init)
    return str_find_aux(s, pattern, init, false, false)
  end

  local function str_gmatch(s, pattern, init)
    checkArg(1, s, "string")
    checkArg(2, pattern, "string")

    if #s < SHORT_STRING then
      return string_gmatch(s, pattern, init)
    end

    local start = 0
    if isLuaOver54 then
      checkArg(3, init, "number", "nil")
      if init ~= nil then
        start = posrelat(init, #s)
        if start < 1 then start = 0
        elseif start > #s + 1 then start = #s + 1
        else start = start - 1 end
      end
    end

    local s = strptr(s)
    local p = strptr(pattern)
    return function()
      ms = {
        src_init = s,
        src_end = s:copy(s:len()),
        p_end = p:copy(p:len()),
        capture = {}
      }
      for offset = start, ms.src_end.pos - 1 do
        local src = s:copy(offset)
        ms.level = 0
        local e = match(ms, src, p)
        if e ~= nil then
          local newstart = e - s
          if e == src then newstart = newstart + 1 end -- empty match? go at least one position
          start = newstart
          return push_captures(ms, src, e)
        end
      end
      return nil  -- not found
    end
  end

  local function add_s(ms, b, s, e, r)
    local news = tostring(r)
    local i = 1
    while i <= #news do
      if news:sub(i, i) ~= L_ESC then
        b = b .. news:sub(i, i)
      else
        i = i + 1  -- skip ESC
        if not isdigit(news:sub(i, i)) then
          b = b .. news:sub(i, i)
        elseif news:sub(i, i) == '0' then
          b = b .. s:head(e - s)
        else
          b = b .. push_onecapture(ms, news:sub(i, i) - '1', s, e)  -- add capture to accumulated result
        end
      end
      i = i + 1
    end
    return b
  end

  local function add_value(ms, b, s, e, r, tr)
    local res
    if tr == "function" then
      res = r(push_captures(ms, s, e))
    elseif tr == "table" then
      res = r[push_onecapture(ms, 0, s, e)]
    else  -- LUA_TNUMBER or LUA_TSTRING
      return add_s(ms, b, s, e, r)
    end
    if not res then  -- nil or false?
      res = s:head(e - s)  -- keep original text
    elseif type(res) ~= "string" and type(res) ~= "number" then
      error("invalid replacement value (a "..type(res)..")")
    end
    return b .. res  -- add result to accumulator
  end

  local function str_gsub(s, pattern, repl, n)
    checkArg(1, s, "string")
    checkArg(2, pattern, "string", "number")
    checkArg(3, repl, "number", "string", "function", "table")
    checkArg(4, n, "number", "nil")

    if #s < SHORT_STRING then
      return string_gsub(s, pattern, repl, n)
    end

    pattern = tostring(pattern)
    local src = strptr(s);
    local p = strptr(pattern)
    local tr = type(repl)
    local max_s = n or (#s + 1)
    local anchor = p:char() == '^'
    if anchor then
      p:step()  -- skip anchor character
    end
    n = 0
    local b = ""
    local ms = {
      src_init = src:copy(),
      src_end = src:copy(src:len()),
      p_end = p:copy(p:len()),
      capture = {}
    }
    while n < max_s do
      ms.level = 0
      local e = match(ms, src, p)
      if e then
        n = n + 1
        b = add_value(ms, b, src, e, repl, tr)
      end
      if e and e > src then  -- non empty match?
        src = e  -- skip it
      elseif src < ms.src_end then
        b = b .. src:char()
        src:step()
      else break
      end
      if anchor then break end
    end
    b = b .. src:head()
    return b, n  -- number of substitutions
  end

  string.find = str_find
  string.match = str_match
  string.gmatch = str_gmatch
  string.gsub = str_gsub
end

-------------------------------------------------------------------------------

local function spcall(...)
  local result = table.pack(pcall(...))
  if not result[1] then
    error(tostring(result[2]), 0)
  else
    return table.unpack(result, 2, result.n)
  end
end

local sgcco

local function sgcf(self, gc)
  while true do
    self, gc = coroutine.yield(pcall(gc, self))
  end
end

local function sgc(self)
  local oldDeadline, oldHitDeadline = deadline, hitDeadline
  local mt = debug.getmetatable(self)
  mt = rawget(mt, "mt")
  local gc = rawget(mt, "__gc")
  if type(gc) ~= "function" then
    return
  end
  if not sgcco then
    sgcco = coroutine.create(sgcf)
  end
  debug.sethook(sgcco, checkDeadline, "", hookInterval)
  deadline, hitDeadline = math.min(oldDeadline, computer.realTime() + 0.5), true
  local _, result, reason = coroutine.resume(sgcco, self, gc)
  debug.sethook(sgcco)
  if coroutine.status(sgcco) == "dead" then
    sgcco = nil
  end
  deadline, hitDeadline = oldDeadline, oldHitDeadline
  if not result then
    error(reason, 0)
  end
end

--[[ This is the global environment we make available to userland programs. ]]
-- You'll notice that we do a lot of wrapping of native functions and adding
-- parameter checks in those wrappers. This is to avoid errors from the host
-- side that would push error objects - which are userdata and cannot be
-- persisted.
local sandbox, libprocess
sandbox = {
  assert = assert,
  dofile = nil, -- in boot/*_base.lua
  error = error,
  _G = nil, -- see below
  getmetatable = function(t)
    if type(t) == "string" then -- don't allow messing with the string mt
      return nil
    end
    local result = getmetatable(t)
    -- check if we have a wrapped __gc using mt
    if type(result) == "table" and system.allowGC() and rawget(result, "__gc") == sgc then
      result = rawget(result, "mt")
    end
    return result
  end,
  ipairs = ipairs,
  load = function(ld, source, mode, env)
    if not system.allowBytecode() then
      mode = "t"
    end
    return load(ld, source, mode, env or sandbox)
  end,
  loadfile = nil, -- in boot/*_base.lua
  next = next,
  pairs = pairs,
  pcall = function(...)
    return pcallTimeoutCheck(pcall(...))
  end,
  print = nil, -- in boot/*_base.lua
  rawequal = rawequal,
  rawget = rawget,
  rawlen = rawlen,
  rawset = rawset,
  select = select,
  setmetatable = function(t, mt)
    if type(mt) ~= "table" then
      return setmetatable(t, mt)
    end
    if rawget(mt, "__gc") ~= nil then -- If __gc is set to ANYTHING not `nil`, we're gonna have issues
      -- Garbage collector callbacks apparently can't be sandboxed after
      -- all, because hooks are disabled while they're running. So we just
      -- disable them altogether by default.
      if system.allowGC() then
        -- For all user __gc functions we enforce a much tighter deadline.
        -- This is because these functions may be called from the main
        -- thread under certain circumstanced (such as when saving the world),
        -- which can lead to noticeable lag if the __gc function behaves badly.
        local sbmt = {} -- sandboxed metatable. only for __gc stuff, so it's
                        -- kinda ok to have a shallow copy instead... meh.
        for k, v in next, mt do
          sbmt[k] = v
        end
        sbmt.__gc = sgc
        sbmt.mt = mt
        mt = sbmt
      else
        -- Don't allow marking for finalization, but use the raw metatable.
        local gc = rawget(mt, "__gc")
        rawset(mt, "__gc", nil) -- remove __gc
        local ret = table.pack(pcall(setmetatable, t, mt))
        rawset(mt, "__gc", gc) -- restore __gc
        if not ret[1] then error(ret[2], 0) end
        return table.unpack(ret, 2, ret.n)
      end
    end
    return setmetatable(t, mt)
  end,
  tonumber = tonumber,
  tostring = tostring,
  type = type,
  _VERSION = _VERSION:match("Luaj") and "Luaj" or _VERSION:match("5.4") and "Lua 5.4" or _VERSION:match("5.3") and "Lua 5.3" or "Lua 5.2",
  xpcall = function(f, msgh, ...)
    local handled = false
    checkArg(2, msgh, "function")
    local result = table.pack(xpcall(f, function(...)
      if rawequal((...), tooLongWithoutYielding) then
        return tooLongWithoutYielding
      elseif handled then
        return ...
      else
        handled = true
        return msgh(...)
      end
    end, ...))
    if rawequal(result[2], tooLongWithoutYielding) then
      result = table.pack(result[1], select(2, pcallTimeoutCheck(pcall(msgh, tostring(tooLongWithoutYielding)))))
    end
    return table.unpack(result, 1, result.n)
  end,

  coroutine = {
    create = coroutine.create,
    resume = function(co, ...) -- custom resume part for bubbling sysyields
      checkArg(1, co, "thread")
      local args = table.pack(...)
      while true do -- for consecutive sysyields
        debug.sethook(co, checkDeadline, "", hookInterval)
        local result = table.pack(
          coroutine.resume(co, table.unpack(args, 1, args.n)))
        debug.sethook(co) -- avoid gc issues
        checkDeadline()
        if result[1] then -- success: (true, sysval?, ...?)
          if coroutine.status(co) == "dead" then -- return: (true, ...)
            return true, table.unpack(result, 2, result.n)
          elseif result[2] ~= nil then -- yield: (true, sysval)
            args = table.pack(coroutine.yield(result[2]))
          else -- yield: (true, nil, ...)
            return true, table.unpack(result, 3, result.n)
          end
        else -- error: result = (false, string)
          return false, result[2]
        end
      end
    end,
    running = coroutine.running,
    status = coroutine.status,
    wrap = function(f) -- for bubbling coroutine.resume
      local co = coroutine.create(f)
      return function(...)
        local result = table.pack(sandbox.coroutine.resume(co, ...))
        if result[1] then
          return table.unpack(result, 2, result.n)
        else
          error(result[2], 0)
        end
      end
    end,
    yield = function(...) -- custom yield part for bubbling sysyields
      return coroutine.yield(nil, ...)
    end,
    -- Lua 5.3.
    isyieldable = coroutine.isyieldable
  },

  string = {
    byte = string.byte,
    char = string.char,
    dump = string.dump,
    find = string.find,
    format = string.format,
    gmatch = string.gmatch,
    gsub = string.gsub,
    len = string.len,
    lower = string.lower,
    match = string.match,
    rep = string.rep,
    reverse = string.reverse,
    sub = string.sub,
    upper = string.upper,
    -- Lua 5.3.
    pack = string.pack,
    unpack = string.unpack,
    packsize = string.packsize
  },

  table = {
    concat = table.concat,
    insert = table.insert,
    pack = table.pack,
    remove = table.remove,
    sort = table.sort,
    unpack = table.unpack,
    -- Lua 5.3.
    move = table.move
  },

  math = {
    abs = math.abs,
    acos = math.acos,
    asin = math.asin,
    atan = math.atan,
    atan2 = math.atan2 or math.atan, -- Deprecated in Lua 5.3
    ceil = math.ceil,
    cos = math.cos,
    cosh = math.cosh, -- Deprecated in Lua 5.3
    deg = math.deg,
    exp = math.exp,
    floor = math.floor,
    fmod = math.fmod,
    frexp = math.frexp, -- Deprecated in Lua 5.3
    huge = math.huge,
    ldexp = math.ldexp or function(a, e) -- Deprecated in Lua 5.3
        return a*(2.0^e)
    end,
    log = math.log,
    max = math.max,
    min = math.min,
    modf = math.modf,
    pi = math.pi,
    pow = math.pow or function(a, b) -- Deprecated in Lua 5.3
      return a^b
    end,
    rad = math.rad,
    random = function(...)
      return spcall(math.random, ...)
    end,
    randomseed = function(seed)
      -- math.floor(seed) emulates pre-OC 1.8.0 behaviour
      spcall(math.randomseed, math.floor(seed))
    end,
    sin = math.sin,
    sinh = math.sinh, -- Deprecated in Lua 5.3
    sqrt = math.sqrt,
    tan = math.tan,
    tanh = math.tanh, -- Deprecated in Lua 5.3
    -- Lua 5.3.
    maxinteger = math.maxinteger,
    mininteger = math.mininteger,
    tointeger = math.tointeger,
    type = math.type,
    ult = math.ult
  },

  -- Deprecated in Lua 5.3.
  bit32 = bit32 and {
    arshift = bit32.arshift,
    band = bit32.band,
    bnot = bit32.bnot,
    bor = bit32.bor,
    btest = bit32.btest,
    bxor = bit32.bxor,
    extract = bit32.extract,
    replace = bit32.replace,
    lrotate = bit32.lrotate,
    lshift = bit32.lshift,
    rrotate = bit32.rrotate,
    rshift = bit32.rshift
  },

  io = nil, -- in lib/io.lua

  os = {
    clock = os.clock,
    date = function(format, time)
      return spcall(os.date, format, time)
    end,
    difftime = function(t2, t1)
      return t2 - t1
    end,
    execute = nil, -- in boot/*_os.lua
    exit = nil, -- in boot/*_os.lua
    remove = nil, -- in boot/*_os.lua
    rename = nil, -- in boot/*_os.lua
    time = function(table)
      checkArg(1, table, "table", "nil")
      return os.time(table)
    end,
    tmpname = nil, -- in boot/*_os.lua
  },

  debug = {
    getinfo = function(...)
      local result = debug.getinfo(...)
      if result then
        -- Only make primitive information available in the sandbox.
        return {
          source = result.source,
          short_src = result.short_src,
          linedefined = result.linedefined,
          lastlinedefined = result.lastlinedefined,
          what = result.what,
          currentline = result.currentline,
          nups = result.nups,
          nparams = result.nparams,
          isvararg = result.isvararg,
          name = result.name,
          namewhat = result.namewhat,
          istailcall = result.istailcall
        }
      end
    end,
    traceback = debug.traceback,
    -- using () to wrap the return of debug methods because in Lua doing this
    -- causes only the first return value to be selected
    -- e.g. (1, 2) is only (1), the 2 is not returned
    -- this is critically important here because the 2nd return value from these
    -- debug methods is the value itself, which opens a door to exploit the sandbox
    getlocal = function(...) return (debug.getlocal(...)) end,
    getupvalue = function(...) return (debug.getupvalue(...)) end,
  },

  -- Lua 5.3.
  utf8 = utf8 and {
    char = utf8.char,
    charpattern = utf8.charpattern,
    codes = utf8.codes,
    codepoint = utf8.codepoint,
    len = utf8.len,
    offset = utf8.offset
  },

  checkArg = checkArg
}
sandbox._G = sandbox

-------------------------------------------------------------------------------
-- Start of non-standard stuff.

-- JNLua derps when the metatable of userdata is changed, so we have to
-- wrap and isolate it, to make sure it can't be touched by user code.
-- These functions provide the logic for wrapping and unwrapping (when
-- pushed to user code and when pushed back to the host, respectively).
local wrapUserdata, wrapSingleUserdata, unwrapUserdata, wrappedUserdataMeta

wrappedUserdataMeta = {
  -- Weak keys, clean up once a proxy is no longer referenced anywhere.
  __mode="k",
  -- We need custom persist logic here to avoid ERIS trying to save the
  -- userdata referenced in this table directly. It will be repopulated
  -- in the load methods of the persisted userdata wrappers (see below).
  [persistKey and persistKey() or "LuaJ"] = function()
    return function()
      -- When using special persistence we have to manually reassign the
      -- metatable of the persisted value.
      return setmetatable({}, wrappedUserdataMeta)
    end
  end
}
local wrappedUserdata = setmetatable({}, wrappedUserdataMeta)

local function processResult(result)
  result = wrapUserdata(result) -- needed for metamethods.
  if not result[1] then -- error that should be re-thrown.
    error(result[2], 0)
  else -- success or already processed error.
    return table.unpack(result, 2, result.n)
  end
end

local function invoke(target, direct, ...)
  local result
  if direct then
    local args = table.pack(...) -- for unwrapping
    args = unwrapUserdata(args)
    result = table.pack(target.invoke(table.unpack(args, 1, args.n)))
    args = nil -- clear upvalue, avoids trying to persist it
    if result.n == 0 then -- limit for direct calls reached
      result = nil
    end
    -- no need to wrap here, will be wrapped in processResult
  end
  if not result then
    local args = table.pack(...) -- for access in closure
    result = select(1, coroutine.yield(function()
      args = unwrapUserdata(args)
      local result = table.pack(target.invoke(table.unpack(args, 1, args.n)))
      args = nil -- clear upvalue, avoids trying to persist it
      result = wrapUserdata(result)
      return result
    end))
  end
  return processResult(result)
end

local function udinvoke(f, data, ...)
  local args = table.pack(...)
  args = unwrapUserdata(args)
  local result = table.pack(f(data, table.unpack(args)))
  args = nil -- clear upvalue, avoids trying to persist it
  return processResult(result)
end

-- Metatable for additional functionality on userdata.
local userdataWrapper = {
  __index = function(self, ...)
    return udinvoke(userdata.apply, wrappedUserdata[self], ...)
  end,
  __newindex = function(self, ...)
    return udinvoke(userdata.unapply, wrappedUserdata[self], ...)
  end,
  __call = function(self, ...)
    return udinvoke(userdata.call, wrappedUserdata[self], ...)
  end,
  __gc = function(self)
    local data = wrappedUserdata[self]
    wrappedUserdata[self] = nil
    userdata.dispose(data)
  end,
  -- This is the persistence protocol for userdata. Userdata is considered
  -- to be 'owned' by Lua, and is saved to an NBT tag. We also get the name
  -- of the actual class when saving, so we can create a new instance via
  -- reflection when loading again (and then immediately wrap it again).
  -- Collect wrapped callback methods.
  [persistKey and persistKey() or "LuaJ"] = function(self)
    local className, nbt = userdata.save(wrappedUserdata[self])
    -- The returned closure is what actually gets persisted, including the
    -- upvalues, that being the classname and a byte array representing the
    -- nbt data of the userdata value.
    return function()
      return wrapSingleUserdata(userdata.load(className, nbt))
    end
  end,
  -- Do not allow changing the metatable to avoid the gc callback being
  -- unset, leading to potential resource leakage on the host side.
  __metatable = "userdata",
  __tostring = function(self)
    local data = wrappedUserdata[self]
    return tostring(select(2, pcall(tostring, data)))
  end
}

local userdataCallback = {
  __call = function(self, ...)
    local methods = spcall(userdata.methods, wrappedUserdata[self.proxy])
    for name, direct in pairs(methods) do
      if name == self.name then
        return invoke(userdata, direct, self.proxy, name, ...)
      end
    end
    error("no such method", 1)
  end,
  __tostring = function(self)
    return userdata.doc(wrappedUserdata[self.proxy], self.name) or "function"
  end
}

function wrapSingleUserdata(data)
  -- Reuse proxies for lower memory consumption and more logical behavior
  -- without the need of metamethods like __eq, as well as proper reference
  -- behavior after saving and loading again.
  for k, v in pairs(wrappedUserdata) do
    -- We need a custom 'equals' check for userdata because metamethods on
    -- userdata introduced by JNLua tend to crash the game for some reason.
    if v == data then
      return k
    end
  end
  local proxy = {type = "userdata"}
  local methods = spcall(userdata.methods, data)
  for method in pairs(methods) do
    proxy[method] = setmetatable({name=method, proxy=proxy}, userdataCallback)
  end
  wrappedUserdata[proxy] = data
  return setmetatable(proxy, userdataWrapper)
end

function wrapUserdata(values)
  local processed = {}
  local function wrapRecursively(value)
    if type(value) == "table" then
      if not processed[value] then
        processed[value] = true
        for k, v in pairs(value) do
          value[k] = wrapRecursively(v)
        end
      end
    elseif type(value) == "userdata" then
      return wrapSingleUserdata(value)
    end
    return value
  end
  return wrapRecursively(values)
end

function unwrapUserdata(values)
  local processed = {}
  local function unwrapRecursively(value)
    if wrappedUserdata[value] then
      return wrappedUserdata[value]
    end
    if type(value) == "table" then
      if not processed[value] then
        processed[value] = true
        for k, v in pairs(value) do
          value[k] = unwrapRecursively(v)
        end
      end
    end
    return value
  end
  return unwrapRecursively(values)
end

-------------------------------------------------------------------------------

local libcomponent

-- Caching proxy objects for lower memory use.
local proxyCache = setmetatable({}, {__mode="v"})

-- Short-term caching of callback directness for improved performance.
local directCache = setmetatable({}, {__mode="k"})
local function isDirect(address, method)
  local cacheKey = address..":"..method
  local cachedValue = directCache[cacheKey]
  if cachedValue ~= nil then
    return cachedValue
  end
  local methods, reason = spcall(component.methods, address)
  if not methods then
    return false
  end
  for name, info in pairs(methods) do
    if name == method then
      directCache[cacheKey] = info.direct
      return info.direct
    end
  end
  error("no such method", 1)
end

local componentProxy = {
  __index = function(self, key)
    if self.fields[key] and self.fields[key].getter then
      return libcomponent.invoke(self.address, key)
    else
      rawget(self, key)
    end
  end,
  __newindex = function(self, key, value)
    if self.fields[key] and self.fields[key].setter then
      return libcomponent.invoke(self.address, key, value)
    elseif self.fields[key] and self.fields[key].getter then
      error("field is read-only")
    else
      rawset(self, key, value)
    end
  end,
  __pairs = function(self)
    local keyProxy, keyField, value
    return function()
      if not keyField then
        repeat
          keyProxy, value = next(self, keyProxy)
        until not keyProxy or keyProxy ~= "fields"
      end
      if not keyProxy then
        keyField, value = next(self.fields, keyField)
      end
      return keyProxy or keyField, value
    end
  end
}

local componentCallback = {
  __call = function(self, ...)
    return libcomponent.invoke(self.address, self.name, ...)
  end,
  __tostring = function(self)
    return libcomponent.doc(self.address, self.name) or "function"
  end
}

libcomponent = {
  doc = function(address, method)
    checkArg(1, address, "string")
    checkArg(2, method, "string")
    local result, reason = spcall(component.doc, address, method)
    if not result and reason then
      error(reason, 2)
    end
    return result
  end,
  invoke = function(address, method, ...)
    checkArg(1, address, "string")
    checkArg(2, method, "string")
    return invoke(component, isDirect(address, method), address, method, ...)
  end,
  list = function(filter, exact)
    checkArg(1, filter, "string", "nil")
    local list = spcall(component.list, filter, not not exact)
    local key = nil
    return setmetatable(list, {__call=function()
      key = next(list, key)
      if key then
        return key, list[key]
      end
    end})
  end,
  methods = function(address)
    local result, reason = spcall(component.methods, address)
    -- Transform to pre 1.4 format to avoid breaking scripts.
    if type(result) == "table" then
      for k, v in pairs(result) do
        if not v.getter and not v.setter then
          result[k] = v.direct
        else
          result[k] = nil
        end
      end
      return result
    end
    return result, reason
  end,
  fields = function(address)
    local result, reason = spcall(component.methods, address)
    if type(result) == "table" then
      for k, v in pairs(result) do
        if not v.getter and not v.setter then
          result[k] = nil
        end
      end
      return result
    end
    return result, reason
  end,
  proxy = function(address)
    local type, reason = spcall(component.type, address)
    if not type then
      return nil, reason
    end
    local slot, reason = spcall(component.slot, address)
    if not slot then
      return nil, reason
    end
    if proxyCache[address] then
      return proxyCache[address]
    end
    local proxy = {address = address, type = type, slot = slot, fields = {}}
    local methods, reason = spcall(component.methods, address)
    if not methods then
      return nil, reason
    end
    for method, info in pairs(methods) do
      if not info.getter and not info.setter then
        proxy[method] = setmetatable({address=address,name=method}, componentCallback)
      else
        proxy.fields[method] = info
      end
    end
    setmetatable(proxy, componentProxy)
    proxyCache[address] = proxy
    return proxy
  end,
  type = function(address)
    return spcall(component.type, address)
  end,
  slot = function(address)
    return spcall(component.slot, address)
  end
}
sandbox.component = libcomponent

local libcomputer = {
  isRobot = computer.isRobot,
  address = computer.address,
  tmpAddress = computer.tmpAddress,
  freeMemory = computer.freeMemory,
  totalMemory = computer.totalMemory,
  uptime = computer.uptime,
  energy = computer.energy,
  maxEnergy = computer.maxEnergy,

  getBootAddress = computer.getBootAddress,
  setBootAddress = function(...)
    return spcall(computer.setBootAddress, ...)
  end,

  users = computer.users,
  addUser = function(...)
    return spcall(computer.addUser, ...)
  end,
  removeUser = function(...)
    return spcall(computer.removeUser, ...)
  end,

  shutdown = function(reboot)
    coroutine.yield(not not reboot)
  end,
  pushSignal = function(...)
    return spcall(computer.pushSignal, ...)
  end,
  pullSignal = function(timeout)
    local deadline = computer.uptime() +
      (type(timeout) == "number" and timeout or math.huge)
    repeat
      local signal = table.pack(coroutine.yield(deadline - computer.uptime()))
      if signal.n > 0 then
        return table.unpack(signal, 1, signal.n)
      end
    until computer.uptime() >= deadline
  end,

  beep = function(...)
    return libcomponent.invoke(computer.address(), "beep", ...)
  end,
  getDeviceInfo = function()
    return libcomponent.invoke(computer.address(), "getDeviceInfo")
  end,
  getProgramLocations = function()
    return libcomponent.invoke(computer.address(), "getProgramLocations")
  end,

  getArchitectures = function(...)
    return spcall(computer.getArchitectures, ...)
  end,
  getArchitecture = function(...)
    return spcall(computer.getArchitecture, ...)
  end,
  setArchitecture = function(...)
    local result, reason = spcall(computer.setArchitecture, ...)
    if not result then
      if reason then
        return result, reason
      end
    else
      coroutine.yield(true) -- reboot
    end
  end
}
sandbox.computer = libcomputer

local libunicode = {
  char = function(...)
    return spcall(unicode.char, ...)
  end,
  len = function(s)
    return spcall(unicode.len, s)
  end,
  lower = function(s)
    return spcall(unicode.lower, s)
  end,
  reverse = function(s)
    return spcall(unicode.reverse, s)
  end,
  sub = function(s, i, j)
    if j then
      return spcall(unicode.sub, s, i, j)
    end
    return spcall(unicode.sub, s, i)
  end,
  upper = function(s)
    return spcall(unicode.upper, s)
  end,
  isWide = function(s)
    return spcall(unicode.isWide, s)
  end,
  charWidth = function(s)
    return spcall(unicode.charWidth, s)
  end,
  wlen = function(s)
    return spcall(unicode.wlen, s)
  end,
  wtrunc = function(s, n)
    return spcall(unicode.wtrunc, s, n)
  end
}
sandbox.unicode = libunicode

-------------------------------------------------------------------------------

local function bootstrap()
  local eeprom = libcomponent.list("eeprom")()
  if eeprom then
    local code = libcomponent.invoke(eeprom, "get")
    if code and #code > 0 then
      local bios, reason = load(code, "=bios", "t", sandbox)
      if bios then
        return coroutine.create(bios), {n=0}
      end
      error("failed loading bios: " .. reason, 0)
    end
  end
  error("no bios found; install a configured EEPROM", 0)
end

-------------------------------------------------------------------------------

local function main()
  -- Yield once to get a memory baseline.
  coroutine.yield()

  -- After memory footprint to avoid init.lua bumping the baseline.
  local co, args = bootstrap()
  local forceGC = 10

  while true do
    deadline = computer.realTime() + system.timeout()
    hitDeadline = false

    -- NOTE: since this is run in an executor thread and we enforce timeouts
    -- in user-defined garbage collector callbacks this should be safe.
    if persistKey then -- otherwise we're in LuaJ
      forceGC = forceGC - 1
      if forceGC < 1 then
        collectgarbage("collect")
        forceGC = 10
      end
    end

    debug.sethook(co, checkDeadline, "", hookInterval)
    local result = table.pack(coroutine.resume(co, table.unpack(args, 1, args.n)))
    args = nil -- clear upvalue, avoids trying to persist it
    if not result[1] then
      error(tostring(result[2]), 0)
    elseif coroutine.status(co) == "dead" then
      error("computer halted", 0)
    else
      args = table.pack(coroutine.yield(result[2])) -- system yielded value
      args = wrapUserdata(args)
    end
  end
end

-- JNLua converts the coroutine to a string immediately, so we can't get the
-- traceback later. Because of that we have to do the error handling here.
return pcallTimeoutCheck(pcall(main))
