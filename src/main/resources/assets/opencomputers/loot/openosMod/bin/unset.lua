local args = {...}

if #args < 1 then
  io.write("Usage: unset <varname>[ <varname2> [...]]\n")
else
  for _, k in ipairs(args) do
    os.setenv(k, nil)
  end
end
