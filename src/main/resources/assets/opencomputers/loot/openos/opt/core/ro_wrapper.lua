local lib = {}

function lib.wrap(proxy)
  checkArg(1, proxy, "table")
  if proxy.isReadOnly() then
    return proxy
  end

  local function roerr() return nil, "filesystem is readonly" end
  return setmetatable({
    rename = roerr,
    open = function(path, mode)
      checkArg(1, path, "string")
      checkArg(2, mode, "string")
      if mode:match("[wa]") then
        return roerr()
      end
      return proxy.open(path, mode)
    end,
    isReadOnly = function()
      return true
    end,
    write = roerr,
    setLabel = roerr,
    makeDirectory = roerr,
    remove = roerr,
  }, {__index=proxy})
end

return lib
