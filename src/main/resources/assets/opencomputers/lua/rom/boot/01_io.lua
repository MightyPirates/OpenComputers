local buffer = require("buffer")
local term = require("term")

local stdinStream = {handle="stdin"}
local stdoutStream = {handle="stdout"}
local stdinHistory = {}

local function badFileDescriptor()
  return nil, "bad file descriptor"
end

function stdinStream:close()
  return nil, "cannot close standard file"
end
stdoutStream.close = stdinStream.close

function stdinStream:read(n)
  local result = term.read(stdinHistory)
  while #stdinHistory > 10 do
    table.remove(stdinHistory, 1)
  end
  return result
end

function stdoutStream:write(str)
  term.write(str, true)
  return self
end

stdinStream.seek = badFileDescriptor
stdinStream.write = badFileDescriptor
stdoutStream.read = badFileDescriptor
stdoutStream.seek = badFileDescriptor

io.stdin = buffer.new("r", stdinStream)
io.stdout = buffer.new("w", stdoutStream)
io.stderr = io.stdout

io.stdout:setvbuf("no")

io.input(io.stdin)
io.output(io.stdout)