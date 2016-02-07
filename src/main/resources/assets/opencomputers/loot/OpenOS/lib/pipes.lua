local tx = require("transforms")
local shell = require("shell")
local sh = require("sh")
local process = require("process")

local plib = {}

plib.internal = {}

local pipeStream = {}
local function bfd() return nil, "bad file descriptor" end
function pipeStream.new(pm)
  local stream = {pm=pm}
  local metatable = {__index = pipeStream}
  return setmetatable(stream, metatable)
end
function pipeStream:resume()
  local yield_args = table.pack(self.pm.pco.resume_all(table.unpack(self.pm.args)))
  if not yield_args[1] then
    self.pm.args = {false}
    self.pm.dead = true

    if not yield_args[1] and yield_args[2] then
      io.stderr:write(tostring(yield_args[2]) .. "\n")
    end
  end
  self.pm.args = {true}
  return table.unpack(yield_args)
end
function pipeStream:close()
  if self.pm.closed then -- already closed
    return
  end

  self.pm.closed = true

  -- if our pco stack is empty, we've already run fully
  if self.pm.pco.top() == nil then
    return
  end

  -- if a thread aborted we have set dead true
  if self.pm.dead then
    return
  end

  -- run prog until dead
  local co = self.pm.pco.previous_handler
  local pco_root = self.pm.threads[1]
  if co.status(pco_root) == "dead" then
    -- I would have liked the pco stack to unwind itself for dead coroutines
    -- maybe I haven't handled aborts corrects
    return
  end

  return self:resume(true)
end
function pipeStream:read(n)
  local pm = self.pm

  if pm.closed then
    return bfd()
  end

  if pm:buffer() == '' and not pm.dead then
    local result = table.pack(self:resume())
    if not result[1] then
      -- resume can fail if p1 crashes
      self:close()
      return nil, "pipe closed unexpectedly"
    elseif result.n > 1 and not result[2] then
      return result[2], result[3]
    end
  end

  local result = pm:buffer(n)
  if result == '' and pm.dead and n > 0 then
    return nil -- eof
  end

  return result
end
function pipeStream:seek(whence, offset)
  return bfd()
end
function pipeStream:write(v)
  local pm = self.pm
  if pm.closed or pm.dead then
    -- if prog is dead, ignore all writes
    if pm.pco.previous_handler.status(pm.threads[pm.self_id]) ~= "dead" then
      error("attempt to use a closed stream")
    end
    return bfd()
  end

  pm:buffer(pm:buffer() .. v)

  -- allow handler to push write event
  local result = table.pack(self:resume())
  if not result[1] then
    -- resume can fail if p1 crashes
    pm.dead = true
    self:close()
    return nil, "pipe closed unexpectedly"
  end

  return self
end

function plib.internal.redirectRead(pm)
  local reader = {pm=pm}
  function reader:read(n)
    local pm = self.pm
    local pco = pm.pco
    -- if we have any buffer, return it first

    if pm:buffer() == '' and not pm.closed and not pm.dead then
      pco.yield_all()
    end

    if pm.closed or pm.dead then
      return nil
    end

    return pm:buffer(n)
  end

  return reader
end

function plib.internal.create(fp)
  local _co = process.info().data.coroutine_handler

  local pco = setmetatable(
  {
    stack = {},
    args = {},
    next = nil,
    create = _co.create,
    previous_handler = _co
  }, {__index=_co})

  function pco.top()
    return pco.stack[#pco.stack]
  end
  function pco.yield(...)
    -- pop last
    pco.set_unwind(pco.running())
    return _co.yield(...)
  end
  function pco.index_of(thread)
    for i,t in ipairs(pco.stack) do
      if t == thread then
        return i
      end
    end
  end
  function pco.yield_all(...)
    local current = pco.running()
    local existing_index = pco.index_of(current)
    assert(existing_index, "cannot yield inactive stack")
    pco.next = current
    return _co.yield(...)
  end
  function pco.set_unwind(from)
    pco.next = nil
    if from then
      local index = pco.index_of(from)
      if index then
        pco.stack = tx.sub(pco.stack, 1, index-1)
        pco.next = pco.stack[index-1]
      end
    end
  end
  function pco.resume_all(...)
    local base = pco.stack[1]
    local top = pco.top()
    if type(base) ~= "thread" or _co.status(base) ~= "suspended" or 
       type(top) ~= "thread" or _co.status(top) ~= "suspended" then
      return false
    end

    local status, result = pcall(function(...)
      local _result = table.pack(pco.resume(top, ...))
      return _result
    end,...)

    if not status then
      return nil, result
    end

    return table.unpack(result)
  end
  function pco.resume(thread, ...)
    checkArg(1, thread, "thread")
    local status = pco.status(thread)
    if status ~= "suspended" then
      local msg = string.format("cannot resume %s coroutine", 
        status == "dead" and "dead" or "non-suspended")
      return false, msg
    end

    local current_index = pco.index_of(pco.running())
    local existing_index = pco.index_of(thread)

    if not existing_index then
      assert(current_index, "pco coroutines cannot resume threads outside the stack")
      pco.stack = tx.concat(tx.sub(pco.stack, 1, current_index), {thread})
    end

    if current_index then
      -- current should be waiting for yield
      pco.next = thread
      return true, _co.yield(...) -- pass args to resume next
    else
      -- the stack is not running
      pco.next = nil
      local yield_args = table.pack(_co.resume(thread, ...))
      if #pco.stack > 0 then
        -- thread may have crashed (crash unwinds as well)
        -- or we don't have next lined up (unwind)
        if not pco.next or not yield_args[1] then
          -- unwind from current index, not top
          pco.set_unwind(thread)
        end

          -- if next is current thread, yield_all is active
          -- in such a case, yield out first, then resume where we left off
        if pco.next and pco.next ~= thread then
          local next = pco.next
          pco.next = nil
          return pco.resume(next, table.unpack(yield_args,2,yield_args.n))
        end
      end

      return table.unpack(yield_args)
    end
  end
  function pco.status(thread)
    checkArg(1, thread, "thread")

    local current_index = pco.index_of(pco.running())
    local existing_index = pco.index_of(thread)

    if current_index and existing_index and existing_index < current_index then
      local current = pco.stack[current_index]
      if current and _co.status(current) == "running" then
        return "normal"
      end
    end

    return _co.status(thread)
  end
  function pco.wrap(f)
    local thread = coroutine.create(f)
    return function(...)
      local result_pack = table.pack(pco.resume(thread, ...))
      local result, reason = result_pack[1], result_pack[2]
      assert(result, reason)
      return select(2, table.unpack(result_pack))
    end
  end

  if fp then
    pco.stack = {process.load(fp,nil,nil--[[init]],"pco root")}
    process.info(pco.stack[1]).data.coroutine_handler = pco
  end

  return pco
end

local pipeManager = {}
function pipeManager.reader(pm,...)
  while pm.pco.status(pm.threads[pm.prog_id]) ~= "dead" do
    pm.pco.yield_all()

    -- kick back to main thread, true to kick back one further
    if pm.closed then break end

    -- if we are a reader pipe, we leave the buffer alone and yield to previous
    if pm.pco.status(pm.threads[pm.prog_id]) ~= "dead" then
      pm.pco.yield()
    end
  end
  pm.dead = true
end

function pipeManager:buffer(value)
  -- if value but no stream, buffer for buffer

  local s = self and self.pipe and self.pipe.stream
  if not s then
    if type(value) == "string" or self.prewrite then
      self.prewrite = self.prewrite or {}
      s = self.prewrite -- s.buffer will be self.prewrite.buffer
    else
      return ''
    end
  elseif self.prewrite then -- we stored, previously, a prewrite buffer
    s.buffer = self.prewrite.buffer .. s.buffer
    self.prewrite = nil
  end

  if type(value) == "string" then
    s.buffer = value
    return value
  elseif type(value) ~= "number" then
    return s.buffer -- don't truncate
  end

  local result = string.sub(s.buffer, 1, value)
  s.buffer = string.sub(s.buffer, value + 1)
  return result
end

function pipeManager.new(prog, mode, env)
  mode = mode or "r"
  if mode ~= "r" and mode ~= "w" then
    return nil, "bad argument #2: invalid mode " .. tostring(mode) .. " must be r or w"
  end

  local shellPath = os.getenv("SHELL") or "/bin/sh"
  local shellPath, reason = shell.resolve(shellPath, "lua")
  if not shellPath then
    return nil, reason
  end

  local pm = setmetatable(
    {dead=false,closed=false,args={},prog=prog,mode=mode,env=env},
    {__index=pipeManager}
  )
  pm.prog_id = pm.mode == "r" and 1 or 2
  pm.self_id = pm.mode == "r" and 2 or 1
  pm.handler = pm.mode == "r" and 
    function()return pipeManager.reader(pm)end or
    function()pm.dead=true end

  pm.commands = {}
  pm.commands[pm.prog_id] = {shellPath, sh.internal.buildCommandRedirects({})}
  pm.commands[pm.self_id] = {pm.handler, sh.internal.buildCommandRedirects({})}

  pm.root = function()
    local reason
    pm.threads, reason, pm.inputs, pm.outputs = 
      sh.internal.buildPipeStream(pm.commands, pm.env)

    if not pm.threads then
      pm.dead = true
      return false, reason -- 2nd return is reason, not pipes, on error :)
    end
    pm.pipe = reason[1] -- an array of pipes of length 1

    local startup_args = {}
    -- if we are the writer, we need args to resume prog
    if pm.mode == "w" then
      pm.pipe.stream.args = {pm.env,pm.prog,n=2}
      startup_args = {true,n=1}
      -- also, if we are the writer, we need to intercept the reader
      pm.pipe.stream.redirect.read = plib.internal.redirectRead(pm)
    else
      startup_args = {true,pm.env,pm.prog,n=3}
    end

    return sh.internal.executePipeStream(
      pm.threads,
      {pm.pipe},
      pm.inputs,
      pm.outputs,
      startup_args)
  end

  return pm
end

function plib.popen(prog, mode, env)
  checkArg(1, prog, "string")
  checkArg(2, mode, "string", "nil")
  checkArg(3, env, "table", "nil")

  local pm, reason = pipeManager.new(prog, mode, env)

  if not pm then
    return false, reason
  end

  pm.pco=plib.internal.create(pm.root)
  
  local pfd = require("buffer").new(mode, pipeStream.new(pm))
  pfd:setvbuf("no", nil) -- 2nd are to read chunk size

  -- popen processes start on create (which is LAME :P)
  pfd.stream:resume()

  return pfd
end

return plib
