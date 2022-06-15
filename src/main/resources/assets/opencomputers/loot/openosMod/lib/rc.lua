-- Keeps track of loaded scripts to retain local values between invocation
-- of their command callbacks.
local rc = {}
rc.loaded = {}

return rc

