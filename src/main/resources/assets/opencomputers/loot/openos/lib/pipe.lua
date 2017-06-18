local process = require("process")
local shell = require("shell")
local buffer = require("buffer")
local command_result_as_code = require("sh").internal.command_result_as_code

local pipe = {}

-- root can be a coroutine or a function
function pipe.createCoroutineStack(root, env, name)
  checkArg(1, root, "thread", "function")

  if type(root) == "function" then
    root = assert(process.load(root, env, nil, name or "pipe"), "failed to load proc data for given function")
  end

  local proc = assert(process.info(root), "failed to load proc data for given coroutine")
  local _co = proc.data.coroutine_handler

  local pco = setmetatable({root=root}, {__index=_co})
  proc.data.coroutine_handler = pco

  function pco.yield(...)
    return _co.yield(nil, ...)
  end
  function pco.yield_all(...)
    return _co.yield(true, ...)
  end
  function pco.resume(co, ...)
    checkArg(1, co, "thread")
    local args = table.pack(...)
    while true do -- for consecutive sysyields
      local result = table.pack(_co.resume(co, table.unpack(args, 1, args.n)))
      if result[1] then -- success: (true, sysval?, ...?)
        if _co.status(co) == "dead" or pco.root == co then -- return: (true, ...)
          return true, table.unpack(result, 2, result.n)
        elseif result[2] ~= nil then -- yield: (true, sysval)
          args = table.pack(_co.yield(table.unpack(result, 2, result.n)))
        else -- yield: (true, nil, ...)
          return true, table.unpack(result, 3, result.n)
        end
      else -- error: result = (false, string)
        return false, result[2]
      end
    end
  end
  return pco
end

local pipe_stream = 
{
  close = function(self)
    self.closed = true
    if coroutine.status(self.next) == "suspended" then
      coroutine.resume(self.next)
    end
    self.redirect = {}
  end,
  seek = function()
    return nil, "bad file descriptor"
  end,
  write = function(self, value)
    if not self.redirect[1] and self.closed then
      -- if next is dead, ignore all writes
      if coroutine.status(self.next) ~= "dead" then
        io.stderr:write("attempt to use a closed stream\n")
        os.exit(1)
      end
    elseif self.redirect[1] then
      return self.redirect[1]:write(value)
    elseif not self.closed then
      self.buffer = self.buffer .. value
      local result = table.pack(coroutine.resume(self.next))
      if coroutine.status(self.next) == "dead" then
        self:close()
        -- always cause os.exit when the pipe closes
        -- this is very important
        -- e.g. cat very_large_file | head
        -- when head is done, cat should stop
        result[1] = nil
      end
      -- the next pipe
      if not result[1] then
        os.exit(command_result_as_code(result[2]))
      end
      return self
    end
    os.exit(0) -- abort the current process: SIGPIPE
  end,
  read = function(self, n)
    if self.closed then
      return nil -- eof
    end
    if self.redirect[0] then
      -- popen could be using this code path
      -- if that is the case, it is important to leave stream.buffer alone
      return self.redirect[0]:read(n)
    elseif self.buffer == "" then
      coroutine.yield()
    end
    local result = string.sub(self.buffer, 1, n)
    self.buffer = string.sub(self.buffer, n + 1)
    return result
  end
}

-- prog1 | prog2 | ... | progn
function pipe.buildPipeChain(progs)
  local chain = {}
  local prev_piped_stream
  for i=1,#progs do
    local prog = progs[i]
    local thread = type(prog) == "thread" and prog or pipe.createCoroutineStack(prog).root
    chain[i] = thread
    local data = process.info(thread).data
    local pio = data.io

    local piped_stream
    if i < #progs then
      local handle = setmetatable({redirect = {rawget(pio, 1)},buffer = ""}, {__index = pipe_stream})
      piped_stream = buffer.new("rw", handle)
      piped_stream:setvbuf("no", 1024)
      -- buffer close flushes the buffer, but we have no buffer
      -- also, when the buffer is closed, reads and writes don't pass through
      -- simply put, we don't want buffer:close
      piped_stream.close = function(self) self.stream:close() end
      pio[1] = piped_stream
      table.insert(data.handles, piped_stream)
    end

    if prev_piped_stream then
      prev_piped_stream.stream.redirect[0] = rawget(pio, 0)
      prev_piped_stream.stream.next = thread
      pio[0] = prev_piped_stream
    end

    prev_piped_stream = piped_stream
  end

  return chain
end

local chain_stream =
{
  read = function(self, value)
    -- handler is currently on yield_all [else we wouldn't have control here]
    local stack_ok, read_ok, ret = self.pco.resume(self.pco.root, value)
    return select(stack_ok and read_ok and 2 or 1, nil, ret)
  end,
  write = function(self, ...)
    return self:read(table.concat({...}))
  end,
  close = function(self)
    self.io_stream:close()
  end,
}

function pipe.popen(prog, mode, env)
  mode = mode or "r"
  if mode ~= "r" and mode ~= "w" then
    return nil, "bad argument #2: invalid mode " .. tostring(mode) .. " must be r or w"
  end

  local r = mode == "r"
  local key = r and "read" or "write"

  -- to simplify the code - shell.execute is run within a function to pass (prog, env)
  -- if cmd_proc where to come second (mode=="w") then the pipe_proc would have to pass
  -- the starting args. which is possible, just more complicated
  local cmd_proc = process.load(function() return shell.execute(prog, env) end, nil, nil, prog)

  -- the chain stream is the popen controller
  local stream = setmetatable({}, { __index = chain_stream })

  -- the stream needs its own process for io
  local pipe_proc = process.load(function()
    local n = r and 0 or ""
    while true do
      n = stream.pco.yield_all(stream.io_stream[key](stream.io_stream, n))
    end
  end, nil, nil, "pipe_handler")

  local pipe_index = r and 2 or 1
  local cmd_index = r and 1 or 2
  local chain = {[cmd_index]=cmd_proc, [pipe_index]=pipe_proc}

  -- upgrade coroutine stack
  local cmd_stack = pipe.createCoroutineStack(chain[1])

  -- the processes need to share the coroutine handler to yield the cmd_stack
  process.info(chain[2]).data.coroutine_handler = cmd_stack

  -- link the cmd and pipe proc io
  pipe.buildPipeChain(chain)

  -- store handle to io_stream from easy access later
  stream.io_stream = process.info(cmd_stack.root).data.io[1].stream
  stream.pco = cmd_stack

  -- popen commands start out running, like threads
  cmd_stack.resume(cmd_stack.root)

  local buffered_stream = buffer.new(mode, stream)
  buffered_stream:setvbuf("no", 1024)
  return buffered_stream
end

return pipe
