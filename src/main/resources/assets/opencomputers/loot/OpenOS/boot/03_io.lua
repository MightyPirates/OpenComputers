local buffer = require("buffer")
local term = require("term")

local io_open = io.open
function io.open(path, mode)
  return io_open(require("shell").resolve(path), mode)
end

local stdinStream = {handle="stdin"}
local stdoutStream = {handle="stdout"}
local stderrStream = {handle="stderr"}
local stdinHistory = {}

local function badFileDescriptor()
  return nil, "bad file descriptor"
end

function stdinStream:close()
  return nil, "cannot close standard file"
end
stdoutStream.close = stdinStream.close
stderrStream.close = stdinStream.close

function stdinStream:read(n, dobreak)
  local result = term.read(stdinHistory, dobreak)
  while #stdinHistory > 10 do
    table.remove(stdinHistory, 1)
  end
  return result
end

function stdoutStream:write(str)
  term.drawText(str, self.wrap ~= false)
  return self
end

function stderrStream:write(str)
  local component = require("component")
  if component.isAvailable("gpu") and component.gpu.getDepth() and component.gpu.getDepth() > 1 then
    local foreground = component.gpu.setForeground(0xFF0000)
    term.write(str, true)
    component.gpu.setForeground(foreground)
  else
    term.write(str, true)
  end
  return self
end

stdinStream.seek = badFileDescriptor
stdinStream.write = badFileDescriptor
stdoutStream.read = badFileDescriptor
stdoutStream.seek = badFileDescriptor
stderrStream.read = badFileDescriptor
stderrStream.seek = badFileDescriptor

local core_stdin = buffer.new("r", stdinStream)
local core_stdout = buffer.new("w", stdoutStream)
local core_stderr = buffer.new("w", stderrStream)

core_stdout:setvbuf("no")
core_stderr:setvbuf("no")
core_stdin.tty = true
core_stdout.tty = true
core_stderr.tty = true

core_stdin.close = stdinStream.close
core_stdout.close = stdinStream.close
core_stderr.close = stdinStream.close

local fd_map =
{
  -- key name => method name
  stdin = 'input',
  stdout = 'output',
  stderr = 'error'
}

local io_mt = getmetatable(io) or {}
io_mt.__index = function(t, k)
  if fd_map[k] then
    return io[fd_map[k]]()
  end
end
io_mt.__newindex = function(t, k, v)
  if fd_map[k] then
    io[fd_map[k]](v)
  else
    rawset(io, k, v)
  end
end

setmetatable(io, io_mt)

io.stdin = core_stdin
io.stdout = core_stdout
io.stderr = core_stderr
