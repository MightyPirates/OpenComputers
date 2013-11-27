local dirs, options = shell.parse(...)
if #dirs == 0 then
  table.insert(dirs, ".")
end

for i = 1, #dirs do
  local path = shell.resolve(dirs[i])
  if #dirs > 1 then
    if i > 1 then print() end
    print("/" .. path .. ":")
  end
  local list, reason = fs.list(path)
  if not list then
    print(reason)
  else
    local function setColor(c)
      if component.gpu.getForeground() ~= c then
        component.gpu.setForeground(c)
      end
    end
    local lsd = {}
    local lsf = {}
    for f in list do
      if f:sub(-1) == "/" then
        table.insert(lsd, f)
      else
        table.insert(lsf, f)
      end
    end
    table.sort(lsd)
    table.sort(lsf)
    setColor(0x99CCFF)
    for _, d in ipairs(lsd) do
      if options.a or d:sub(1, 1) ~= "." then
        io.write(d .. "\t")
        if options.l then
          print()
        end
      end
    end
    for _, f in ipairs(lsf) do
      if f:sub(-4) == ".lua" then
        setColor(0x00FF00)
      else
        setColor(0xFFFFFF)
      end
      if options.a or f:sub(1, 1) ~= "." then
        io.write(f .. "\t")
        if options.l then
          setColor(0xFFFFFF)
          print(fs.size(fs.concat(path, f)))
        end
      end
    end
    setColor(0xFFFFFF)
    if not options.l then
      print()
    end
  end
end
