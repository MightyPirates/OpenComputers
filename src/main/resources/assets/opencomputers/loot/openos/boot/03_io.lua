local buffer = require("buffer")
local tty = require("tty")

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

function stdinStream:read()
  return tty.read(stdinHistory)
end

function stdoutStream:write(str)
  tty.drawText(str, self.nowrap)
  return self
end

function stderrStream:write(str)
  local gpu = tty.gpu()
  local set_depth = gpu and gpu.getDepth() and gpu.getDepth() > 1

  if set_depth then
    set_depth = gpu.setForeground(0xFF0000)
  end
    
  tty.drawText(str)

  if set_depth then
    gpu.setForeground(set_depth)
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

local io_mt = getmetatable(io) or {}
io_mt.__index = function(t, k)
  return
    k == 'stdin' and io.input() or
    k == 'stdout' and io.output() or
    k == 'stderr' and io.error() or
    nil
end

setmetatable(io, io_mt)

io.input(core_stdin)
io.output(core_stdout)
io.error(core_stderr)
