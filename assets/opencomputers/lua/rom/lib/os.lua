os.execute = function(command)
  if not command then
    return type(shell) == "table"
  end
  checkArg(1, command, "string")
  local head, tail = nil, ""
  repeat
    local oldHead = head
    head = command:match("^%S+")
    tail = command:usub(head:ulen() + 1) .. tail
    if head == oldHead then -- say no to infinite recursion, live longer
      command = nil
    else
      command = shell.alias(head)
    end
  until command == nil
  local args = {}
  for part in tail:gmatch("%S+") do
    table.insert(args, part)
  end
  return shell.execute(head, table.unpack(args))
end

os.remove = driver.filesystem.remove

os.rename = driver.filesystem.rename

function os.tmpname()
  if driver.filesystem.exists("tmp") then
    for i = 1, 10 do
      local name = "tmp/" .. math.random(1, 0x7FFFFFFF)
      if not driver.filesystem.exists(name) then
        return name
      end
    end
  end
end
