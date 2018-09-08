-- Run all enabled rc scripts.
-- /boot/*rc was moved before /boot/*filesystem because
-- 1. rc now loads in via the init signal
-- 2. rc used to load directly
-- Thus, for rc to load prior, it needs to register prior
require("event").listen("init", function()
  dofile(require("shell").resolve("rc", "lua"))
  return false
end)

