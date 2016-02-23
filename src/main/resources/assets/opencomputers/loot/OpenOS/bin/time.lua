local sh = require('sh')

local clock_before = os.clock()
local cmd_result = 0
if ... then
  sh.execute(nil, ...) 
  cmd_result = sh.getLastExitCode()
end

local clock_after = os.clock()
local clock_diff = clock_after - clock_before

-- format time
local minutes = clock_diff / 60
local seconds = clock_diff % 60

local seconds_txt = string.format('%f', seconds)
seconds_txt = seconds_txt:gsub('^(.*%....).*$','%1')

io.write(string.format('\nreal%5dm%ss\n', minutes, seconds_txt))

return cmd_result
