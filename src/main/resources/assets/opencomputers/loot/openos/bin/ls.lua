-- load complex, if we can (might be low on memory)

local ok, why = pcall(function(...)
  local full_ls_path = package.searchpath("tools/full_ls", package.path)
  return loadfile(full_ls_path, "bt", _G)(...)
end, ...)

if not ok then 
  io.stderr:write((why or "") .. "\nFor low memory systems, try using `list` instead\n")
  return 1
end

return why
