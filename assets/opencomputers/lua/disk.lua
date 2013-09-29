--[[
  The Disk API, provided by disk components.
]]

driver.disk = {}

-- We track mounted disks in this table.
local fstab = {}

function driver.disk.mount()
end

function driver.disk.umount()
end

function driver.disk.listdir(dirname)
end

function driver.disk.remove(path)
end

function driver.disk.rename(path)
end

function driver.disk.tmpname()
end

function driver.disk.open(filename, mode)
end

function driver.disk.read()
end

function driver.disk.write(value)
end

function driver.disk.flush(file)
end

function driver.disk.close(file)
end

function driver.disk.type(file)
end

-- Aliases for vanilla Lua.
os.remove = driver.disk.remove
os.rename = driver.disk.rename
os.tmpname = driver.disk.tmpname

io = {}
io.flush = function() end -- does nothing
-- TODO io.lines = function(filename) end
io.open = driver.disk.open
-- TODO io.popen = function(prog, mode) end
io.read = driver.disk.read
-- TODO io.tmpfile = function() end
io.type = driver.disk.type
