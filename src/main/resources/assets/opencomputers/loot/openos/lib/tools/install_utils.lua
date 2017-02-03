local cmd, options = ...

local function select_prompt(devs, prompt)
  table.sort(devs, function(a, b) return a.path<b.path end)

  local choice = devs[1]
  if #devs > 1 then
    print(prompt)

    for i = 1, #devs do
      local src = devs[i]
      local label = src.dev.getLabel()
      if label then
        label = label .. " (" .. src.dev.address:sub(1, 8) .. "...)"
      else
        label = src.dev.address
      end
      io.write(i .. ") " .. label .. " at " .. src.path .. '\n')
    end

    io.write("Please enter a number between 1 and " .. #devs .. '\n')
    io.write("Enter 'q' to cancel the installation: ")
    choice = nil
    while not choice do
      result = io.read() or "q"
      if result == "q" then
        os.exit()
      end
      local number = tonumber(result)
      if number and number > 0 and number <= #devs then
        choice = devs[number]
      else
        io.write("Invalid input, please try again: ")
      end
    end
  end

  return choice
end

if cmd == 'select' then
  if #options.sources == 0 then
    if options.source_label then
      io.stderr:write("Nothing to install labeled: " .. options.source_label .. '\n')
    elseif options.from then
      io.stderr:write("Nothing to install from: " .. options.from .. '\n')
    else
      io.stderr:write("Nothing to install\n")
    end
    os.exit(1)
  end

  if #options.targets == 0 then
    if options.to then
      io.stderr:write("No such target to install to: " .. options.to .. '\n')
    else
      io.stderr:write("No writable disks found, aborting\n")
    end
    os.exit(1)
  end

  local source = select_prompt(options.sources, "What do you want to install?")
  if #options.sources > 1 and #options.targets > 1 then
    print()
  end
  local target = select_prompt(options.targets, "Where do you want to install to?")

  return source, target

elseif cmd == 'install' then
  local installer_path = options.source_root .. "/.install"
  local installer, reason = loadfile(installer_path, "bt", setmetatable({install=
  {
    from=options.source_root,
    to=options.target_root,
    fromDir=options.source_dir,
    root=options.target_dir,
    update=options.update,
    label=options.label,
    setlabel=options.setlabel,
    setboot=options.setboot,
    reboot=options.reboot,
  }}, {__index=_G}))
  if not installer then
    io.stderr:write("installer failed to load: " .. tostring(reason) .. '\n')
    os.exit(1)
  else
    return installer()
  end
end