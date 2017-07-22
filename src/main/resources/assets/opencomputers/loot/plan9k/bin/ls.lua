local fs = require("filesystem")
local shell = require("shell")
local text = require('text')
local term = require('term')

local dirs, options = shell.parse(...)
if #dirs == 0 then
  table.insert(dirs, ".")
end

local function formatOutput()
  return true
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
    local lsd = {}
    local lsf = {}
    local m = 1
    for f in list do
      m = math.max(m, f:len() + 2)
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
    io.write("\x1b[36m")
    --setColor(0x66CCFF)

    local col = 1
    local columns = math.huge
    if formatOutput() then
      --columns = math.max(1, math.floor((term.getResolution() - 1) / m))
      columns = math.max(1, math.floor(79 / m))
    end

    for _, d in ipairs(lsd) do
      if options.a or d:sub(1, 1) ~= "." then
        if options.l or not formatOutput() or col % columns == 0 then
          io.write(d .. "\n")
        else
          io.write(text.padRight(d, m))
        end
        col = col + 1
      end
    end

    for _, f in ipairs(lsf) do
      if fs.isLink(fs.concat(path, f)) then
        io.write("\x1b[33m")
        --setColor(0xFFAA00)
      elseif f:sub(-4) == ".lua" then
        io.write("\x1b[32m")
        --setColor(0x00FF00)
      else
        io.write("\x1b[39m")
        --setColor(0xFFFFFF)
      end
      if options.a or f:sub(1, 1) ~= "." then
        if not formatOutput() then
          io.write(f)
          if options.l then
            io.write(" " .. fs.size(fs.concat(path, f)))
          end
          io.write("\n")
        else
          io.write(text.padRight(f, m))
          if options.l then
            io.write("\x1b[39m")
            --setColor(0xFFFFFF)
            io.write(fs.size(fs.concat(path, f)), "\n")
          elseif col % columns == 0 then
            io.write("\n")
          end
        end
        col = col + 1
      end
    end

    io.write("\x1b[39m")
    --setColor(0xFFFFFF)
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

