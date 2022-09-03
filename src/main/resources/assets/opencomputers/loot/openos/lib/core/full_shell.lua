local shell = require("shell")
local process = require("process")

function shell.aliases()
  return pairs(process.info().data.aliases)
end

function shell.execute(command, env, ...)
  local sh, reason = shell.getShell()
  if not sh then
    return false, reason
  end
  local proc = process.load(sh, nil, nil, command)
  local result = table.pack(process.internal.continue(proc, env, command, ...))
  if result.n == 0 then return true end
  return table.unpack(result, 1, result.n)
end

function shell.getPath()
  return os.getenv("PATH")
end

function shell.setPath(value)
  os.setenv("PATH", value)
end
