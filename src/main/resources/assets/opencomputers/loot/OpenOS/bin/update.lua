local component = require("component")
local computer = require("computer")
local args, options = shell.parse(...)
local from = "--from="

if not options.from then
  for addr in component.list("filesystem") do
    if addr ~= computer.getBootAddress() and component.proxy(addr).getLabel() == "openos" then
      from = from .. addr
      break
    end
  end
  
  if from == "--from=" then
    print("Could not find an OpenOS boot device to update from.")
    return
  end
end

return os.execute("install " .. from .. " --update --noboot --nolabelset " .. table.concat(table.pack(...), " "))
