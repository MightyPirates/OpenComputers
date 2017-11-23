local buffer = require("buffer")
local tty_stream = require("tty").stream

local core_stdin = buffer.new("r", tty_stream)
local core_stdout = buffer.new("w", tty_stream)
local core_stderr = buffer.new("w", setmetatable(
{
  write = function(_, str)
    return tty_stream:write("\27[31m"..str.."\27[37m")
  end
}, {__index=tty_stream}))

core_stdout:setvbuf("no")
core_stderr:setvbuf("no")
core_stdin.tty = true
core_stdout.tty = true
core_stderr.tty = true

core_stdin.close = tty_stream.close
core_stdout.close = tty_stream.close
core_stderr.close = tty_stream.close

local io_mt = getmetatable(io) or {}
io_mt.__index = function(_, k)
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
