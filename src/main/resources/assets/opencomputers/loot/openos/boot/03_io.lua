local buffer = require("buffer")
local tty = require("tty")

local core_stdin = buffer.new("r", tty)
local core_stdout = buffer.new("w", tty)
local core_stderr = buffer.new("w", setmetatable(
{
  write = function(_, str)
    return tty:write("\27[31m"..str.."\27[37m")
  end
  }, {__index=tty}))

core_stdout:setvbuf("no")
core_stderr:setvbuf("no")
core_stdin.tty = true
core_stdout.tty = true
core_stderr.tty = true

core_stdin.close = tty.close
core_stdout.close = tty.close
core_stderr.close = tty.close

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
