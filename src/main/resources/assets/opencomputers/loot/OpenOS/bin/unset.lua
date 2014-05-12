local args = {...}

if #args < 1 then
  print("Usage: unset <varname>[ <varname2> [...]]")
else
  for _, k in ipairs(args) do
    os.setenv(k, nil)
  end
end