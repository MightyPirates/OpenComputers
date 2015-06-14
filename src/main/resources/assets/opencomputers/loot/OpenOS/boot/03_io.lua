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
  term.write(str, true)
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

io.stdin = buffer.new("r", stdinStream)
io.stdout = buffer.new("w", stdoutStream)
io.stderr = buffer.new("w", stderrStream)

io.stdout:setvbuf("no")
io.stderr:setvbuf("no")

io.stdin.close = stdinStream.close
io.stdout.close = stdinStream.close
io.stderr.close = stdinStream.close

io.input(io.stdin)
io.output(io.stdout)
