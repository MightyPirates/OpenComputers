local cmd, arg, options, devices = ...

local function select_prompt(devs, prompt)
  table.sort(devs, function(a, b) return a.path<b.path end)
  local num_devs = #devs

  if num_devs < 2 then
    return devs[1]
  end

  io.write(prompt,'\n')

  for i = 1, num_devs do
    local src = devs[i]
    local dev = src.dev
    local selection_label = (src.prop or {}).label or dev.getLabel()
    if selection_label then
      selection_label = string.format("%s (%s...)", selection_label, dev.address:sub(1, 8))
    else
      selection_label = dev.address
    end
    io.write(string.format("%d) %s at %s [r%s]\n", i, selection_label, src.path, dev.isReadOnly() and 'o' or 'w'))
  end

  io.write("Please enter a number between 1 and " .. num_devs .. '\n')
  io.write("Enter 'q' to cancel the installation: ")
  for _=1,5 do
    local result = io.read() or "q"
    if result == "q" then
      os.exit()
    end
    local number = tonumber(result)
    if number and number > 0 and number <= num_devs then
      return devs[number]
    else
      io.write("Invalid input, please try again: ")
      os.sleep(0)
    end
  end
  print("\ntoo many bad inputs, aborting")
  os.exit(1)
end

if cmd == "select" then
  if arg == "sources" then
    if #devices == 0 then
      if options.label then
        io.stderr:write("Nothing to install labeled: " .. options.label .. '\n')
      elseif options.from then
        io.stderr:write("Nothing to install from: " .. options.from .. '\n')
      else
        io.stderr:write("Nothing to install\n")
      end
      os.exit(1)
    end
    return select_prompt(devices, "What do you want to install?")
  elseif arg == "targets" then
    if #devices == 0 then
      if options.to then
        io.stderr:write("No such target to install to: " .. options.to .. '\n')
      else
        io.stderr:write("No writable disks found, aborting\n")
      end
      os.exit(1)
    end
    return select_prompt(devices, "Where do you want to install to?")
  end
end