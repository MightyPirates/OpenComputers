-- Aliases for vanilla Lua.
os.remove = driver.fs.remove
os.rename = driver.fs.rename
-- TODO os.tmpname = function() end

local function unavailable()
  return nil, "bad file descriptor"
end

io = {}

io.stdin = {handle="stdin"}

function io.stdin:close()
  return nil, "cannot close standard file"
end

io.stdin.flush = unavailable

function io.stdin:lines(...)
  return function()
    local result = {self:read(...)}
    if not result[1] and result[2] then
      error(result[2])
    end
    return table.unpack(result)
  end
end

function io.stdin:read(...)
end

io.stdin.seek = unavailable
io.stdin.setvbuf = unavailable
io.stdin.write = unavailable

io.stdout = {handle="stdout"}

io.stdout.close = io.stdin.close

function io.stdout:flush()
  return self -- no-op
end

io.stdout.lines = unavailable
io.stdout.read = unavailable
io.stdout.seek = unavailable
io.stdout.setvbuf = unavailable

function io.stdout:write()
  return nil, "bad file descriptor"
end

io.stderr = io.stdout

io.lines = function(filename)
  local f = io.open(filename)
  return function()
    if f then
      local result, reason = f:read("*l")
      if result then
        return result
      else
        f:close()
        return nil, reason
      end
  end
end

-- TODO io.popen = function(prog, mode) end

-- TODO io.tmpfile = function() end

io.type = driver.fs.type

-------------------------------------------------------------------------------

local input, output = io.stdin, io.stdout

function io.close(file)
  (file or output):close()
end

function io.flush()
  output:flush()
end

function io.input(file)
  if file then
    if type(file) == "string" then
      local result, reason = io.open(file)
      if not result then
        error(reason)
      end
      input = result
    elseif io.type(file) then
      input = file
    else
      error("bad argument #1 (string or file expected, got " .. type(file) .. ")")
    end
  end
  return input
end

function io.open(...)
  local result, reason = driver.fs.open(...)
  if result then
    current = result
  end
  return result, reason
end

function io.output(file)
  if file then
    if type(file) == "string" then
      local result, reason = io.open(file, "w")
      if not result then
        error(reason)
      end
      output = result
    elseif io.type(file) then
      output = file
    else
      error("bad argument #1 (string or file expected, got " .. type(file) .. ")")
    end
  end
  return output
end

function io.read(...)
  if current then
    return current:read(...)
  end
end

function io.write(...)
  if current then
    return current:write(...)
  end
end

-------------------------------------------------------------------------------

event.listen("component_added", function(_, address)
  if component.type(address) == "filesystem" and address ~= os.romAddress() then
    local name = address:sub(1, 3)
    repeat
      name = address:sub(1, name:len() + 1)
    until not driver.fs.exists("/mnt/" .. name)
    driver.fs.mount(address, "/mnt/" .. name)
    local autorun = "/mnt/" .. name .. "/autorun"
    if driver.fs.exists(autorun .. ".lua") then
      dofile(autorun .. ".lua")
    elseif driver.fs.exists(autorun) then
      dofile(autorun)
    end
  end
end)

event.listen("component_removed", function(_, address)
  if component.type(address) == "filesystem" then
    driver.fs.umount(address)
  end
end)
