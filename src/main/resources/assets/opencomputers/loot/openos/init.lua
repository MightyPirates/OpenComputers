do
  local loadfile = load([[return function(file)
    local handle, reason = invoke(addr, "open", file)
    if not handle then
      error(reason)
    end
    local buffer = ""
    repeat
      local data, reason = invoke(addr, "read", handle, math.huge)
      if not data and reason then
        error(reason)
      end
      buffer = buffer .. (data or "")
    until not data
    invoke(addr, "close", handle)
    return load(buffer, "=" .. file, "bt", _G)
  end]], "=loadfile", "bt", {load=load,math=math,addr=computer.getBootAddress(), invoke=component.invoke})()
  loadfile("/lib/tools/boot.lua")(loadfile)
end

while true do
  local result, reason = xpcall(require("shell").getShell(), function(msg)
    return tostring(msg).."\n"..debug.traceback()
  end)
  if not result then
    io.stderr:write((reason ~= nil and tostring(reason) or "unknown error") .. "\n")
    io.write("Press any key to continue.\n")
    os.sleep(0.5)
    require("event").pull("key")
  end
end
