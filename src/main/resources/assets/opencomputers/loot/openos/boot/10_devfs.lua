require("filesystem").mount(
setmetatable({
  isReadOnly = function()return false end,
  address = require("uuid").next()
},
{
  __index=function(tbl,key)return require("devfs")[key]end
}), "/dev")
