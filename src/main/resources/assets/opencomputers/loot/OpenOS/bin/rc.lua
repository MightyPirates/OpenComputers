local rc = require('rc')

local args = {...}

local res, reason = rc.runCommand(table.unpack(args))

if reason then
  print(reason)
end
