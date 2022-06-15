local shell = require("shell")
local args, options = shell.parse(...)

local ec, error_prefix = 0, "alias:"

if options.help then
  print(string.format("Usage: alias: [name[=value] ... ]"))
  return
end

local function validAliasName(k)
  return k:match("[/%$`=|&;%(%)<> \t]") == nil
end

local function setAlias(k, v)
  if not validAliasName(k) then
    io.stderr:write(string.format("%s `%s': invalid alias name\n", error_prefix, k))
  else
    shell.setAlias(k, v)
  end
end

local function printAlias(k)
  local v = shell.getAlias(k)
  if not v then
    io.stderr:write(string.format("%s %s: not found\n", error_prefix, k))
    ec = 1
  else
    io.write(string.format("alias %s='%s'\n", k, v))
  end
end

local function splitPair(arg)
  local matchBegin, matchEnd = arg:find("=")
  if matchBegin == nil or matchBegin == 1 then
    return arg
  else
    return arg:sub(1, matchBegin - 1), arg:sub(matchEnd + 1)
  end
end

local function handlePair(k, v)
  if v then
    return setAlias(k, v)
  else
    return printAlias(k)
  end
end

if not next(args) then -- no args
  -- print all aliases
  for k,v in shell.aliases() do
    print(string.format("alias %s='%s'", k, v))
  end
else
  for _,v in ipairs(args) do
    checkArg(1,v,"string")
    handlePair(splitPair(v))
  end
end

return ec
