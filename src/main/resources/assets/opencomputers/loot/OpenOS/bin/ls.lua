local component = require("component")
local fs = require("filesystem")
local shell = require("shell")

local dirs, options = shell.parse(...)
if #dirs == 0 then
  table.insert(dirs, ".")
end

io.output():setvbuf("line")
for i = 1, #dirs do
  local path = shell.resolve(dirs[i])
  if #dirs > 1 then
    if i > 1 then
      io.write("\n")
    end
    io.write(path, ":\n")
  end
  local list, reason = fs.list(path)
  if not list then
    io.write(reason .. "\n")
  else
    local function setColor(c)
      if component.gpu.getForeground() ~= c then
        io.stdout:flush()
        component.gpu.setForeground(c)
      end
    end
    local lsd = {}
    local lsf = {}
    for f in list do
      if f:sub(-1) == "/" then
        if options.p then
          table.insert(lsd, f)
        else
          table.insert(lsd, f:sub(1, -2))
        end
      else
        table.insert(lsf, f)
      end
    end
    table.sort(lsd)
    table.sort(lsf)
    setColor(0x66CCFF)
    for _, d in ipairs(lsd) do
      if options.a or d:sub(1, 1) ~= "." then
        io.write(d .. "\t")
        if options.l or io.output() ~= io.stdout then
          io.write("\n")
        end
      end
    end
    for _, f in ipairs(lsf) do
      if fs.isLink(fs.concat(path, f)) then
        setColor(0xFFAA00)
      elseif f:sub(-4) == ".lua" then
        setColor(0x00FF00)
      else
        setColor(0xFFFFFF)
      end
      if options.a or f:sub(1, 1) ~= "." then
        io.write(f .. "\t")
        if options.l then
          setColor(0xFFFFFF)
          io.write(fs.size(fs.concat(path, f)), "\n")
        elseif io.output() ~= io.stdout then
          io.write("\n")
        end
      end
    end
    setColor(0xFFFFFF)
    if options.M then
      io.write("\n" .. tostring(#lsf) .. " File(s)")
      io.write("\n" .. tostring(#lsd) .. " Dir(s)")
    end
    if not options.l then
      io.write("\n")
    end
  end
end
io.output():setvbuf("no")
io.output():flush()