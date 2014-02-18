local args = {...}

if #args < 1 then
  for k,v in pairs(os.getenv()) do
    print(k..'='..v)
  end
else
  local count = 1
  for _, expr in ipairs(args) do
    local k, v = string.match(expr, "(.-)=(.*)")
    if v then
      os.setenv(k, v)
    else
      os.setenv(tostring(count), k)
      count = count + 1
    end
  end
end