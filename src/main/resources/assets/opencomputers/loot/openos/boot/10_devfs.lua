require("filesystem").mount(
setmetatable({
  address = "f5501a9b-9c23-1e7a-4afe-4b65eed9b88a"
},
{
  __index=function(tbl,key)
    local pass
    local passthrough = function() return pass end
    if key == "getLabel" then
      pass = "devfs"
    elseif key == "spaceTotal" or key == "spaceUsed" then
      pass = 0
    elseif key == "isReadOnly" then
      pass = false
    else
      return require("devfs")[key]
    end
    return passthrough
  end
}), "/dev")
