--[[
  The Disk API, provided by disk components.
]]

mount = function() end
umount = function() end

listdir = function(dirname) end
remove = _G.os.remove
rename = _G.os.rename
tmpname = function() end

open = function(filename, mode) end
read = function() end
write = function(value) end
flush = function(file) end
close = function(file) end
type = function(file) end