-- Run all enabled rc scripts.
local results = require('rc').allRunCommand('start')

for name, result in pairs(results) do
  local ok, reason = table.unpack(result)
  if not ok then
    io.stderr:write(reason .. "\n")
  end
end
