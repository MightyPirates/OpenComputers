-- load complex, if we can (might be low on memory)

local ok, why = pcall(function(...)
  return loadfile("/lib/core/full_ls.lua", "bt", _G)(...)
end, ...)

if not ok then
  if type(why) == "table" then
    if why.code == 0 then
      return
    end
    why = why.reason
  end
  io.stderr:write(tostring(why) .. "\nFor low memory systems, try using `list` instead\n")
  return 1
end

return why
