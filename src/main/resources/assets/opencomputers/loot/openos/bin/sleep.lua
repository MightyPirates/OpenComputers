local shell = require("shell")
local tty = require("tty")
local args, options = shell.parse(...)

if options.help then
  print([[Usage: sleep NUMBER[SUFFIX]...
Pause for NUMBER seconds.  SUFFIX may be 's' for seconds (the default),
'm' for minutes, 'h' for hours or 'd' for days.  Unlike most implementations
that require NUMBER be an integer, here NUMBER may be an arbitrary floating
point number.  Given two or more arguments, pause for the amount of time
specified by the sum of their values.]])
end

local function help(bad_arg)
  print("sleep: invalid option -- '"..tostring(bad_arg).."'")
  print("Try 'sleep --help' for more information.")
end

local function time_type_multiplier(time_type)
  if not time_type or #time_type == 0 or time_type == 's' then
    return 1
  elseif time_type == 'm' then
    return 60
  elseif time_type == 'h' then
    return 60 * 60
  elseif time_type == 'd' then
    return 60 * 60 * 24
  end

  -- weird error, my bad
  assert(false,'bug parsing parameter:'..tostring(time_type))
end

options.help = nil
if next(options) then
  help(next(options))
  return 1
end

local total_time = 0

for _,v in ipairs(args) do
  local interval, time_type = v:match('^([%d%.]+)([smhd]?)$')
  interval = tonumber(interval)

  if not interval or interval < 0 then
    help(v)
    return 1
  end

  total_time = total_time + time_type_multiplier(time_type) * interval
end

local ins = io.stdin.stream
local pull = ins.pull
local start = 1
if not pull then
  pull = require("event").pull
  start = 2
end
pull(select(start, ins, total_time, "interrupted"))
