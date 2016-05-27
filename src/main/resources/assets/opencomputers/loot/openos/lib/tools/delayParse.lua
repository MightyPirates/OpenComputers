local filepath,delay_data = ...
local file, reason = io.open(filepath, "r")
if not file then
  return reason
end

local methods = {}
local delay_start_pattern = "^%s*function%s*%-%-%[%[@delayloaded%-start@%]%]%s*(.*)$"
local delay_end_pattern = "^%s*end%s*%-%-%[%[@delayloaded%-end@%]%]%s*$"
local n,buffer,lib_name,current_method,open = 0,{}

while true do
  local line = file:read("*L")
  if current_method then
    local closed = not line or line:match(delay_end_pattern)
    if closed then
      local path,method_name,args = open:match("^(.-)([^%.]+)(%(.*)$")
      current_method = current_method-#args
      methods[path] = methods[path] or {}
      methods[path][method_name] = {current_method,n+#line-current_method}
      current_method=nil
    end
  elseif line then
    open = line:match(delay_start_pattern)
    if open then
      lib_name,open = open:match("^([^%.]+)%.(.*)$")
      current_method = n+#line
    else
      buffer[#buffer+1] = line
    end
  else
    file:close()
    break
  end
  n = n + #line
end

if not next(methods) or current_method or not lib_name then
  return "no methods found or unclosed marker for delayed load"
end

buffer = table.concat(buffer)
local loader, reason = load(buffer, "="..filepath, "t", _G)
local library, local_env = loader()
if library then
  local_env = local_env or {}
  local_env[lib_name] = library

  local env = setmetatable(local_env, {__index=_G})
  
  for path,pack in pairs(methods) do
    local target = library
    for name in path:gmatch("[^%.]+") do target = target[name] end
    delay_data[target] =
    {
      methods = pack,
      cache = {},
      env = env,
      path = filepath
    }
    setmetatable(target, delay_data)
  end

  return function()return library end, filepath
end

return reason
