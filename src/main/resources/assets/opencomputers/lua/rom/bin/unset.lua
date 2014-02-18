local args = {...}

if #args < 1 then
  print "You have to specify which arguments to unset!"
else
  for _, k in ipairs(args) do
    os.setenv(k, nil)
  end
end  