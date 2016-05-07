require("filesystem").mount(
setmetatable({
  isReadOnly = function()return true end
},
{
  __index=function(tbl,key)return require("devfs")[key]end
}), "/dev")
