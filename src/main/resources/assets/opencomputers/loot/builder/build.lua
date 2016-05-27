local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local process = require("process")
local robot = require("robot")
local shell = require("shell")

local args = {...}
if #args < 1 then
  print("Please enter a file name with the schematic to build.")
  return
end

local file, reason = io.open(shell.resolve(args[1], "plan"))
if not file then
  io.stderr:write(reason .. "\n")
  return
end

print("Analyzing schematic...")

local counts = {}
local width, height, depth = 0, 1, 0
do
  local lx, lz = 0, 0
  while true do
    local line = file:read()
    if line == nil then
      width = math.max(width, lx)
      depth = math.max(depth, lz)
      file:seek("set")
      break
    end
    line = line:gsub("\r", "")
    if line:sub(1, 1) == "#" then
      height = height + 1
      width = math.max(width, lx)
      depth = math.max(depth, lz)
      lx, lz = 0, 0
    else
      lz = lz + 1
      lx = math.max(lx, #line)
      for i = 1, #line do
        if line:sub(i, i) == "1" then
          counts[lz] = (counts[lz] or 0) + 1
        end
      end
    end
  end
end

print("Model bounds: " .. width .. "x" .. height .. "x" .. depth)

local SLOT_DIRT = 1
local SLOT_STONE = 2
local y, z = 0, 0
local offset, count = 0, depth

if #args > 1 then
  offset = tonumber(args[2]) or 0
  count = tonumber(args[3]) or (depth - offset)
end

local cost = count * height -- the "wall"
for z = offset + 1, offset + count do
  cost = cost + (counts[z] or 0)
end

do
  local path = fs.path(process.running())
  local stateFile = io.open(fs.concat(path, ".state"))
  if stateFile then
    print("State file found, loading...")
    file:seek("set", tonumber(stateFile:read()))
    y = tonumber(stateFile:read())
    z = tonumber(stateFile:read())
    offset = tonumber(stateFile:read())
    count = tonumber(stateFile:read())
    cost = tonumber(stateFile:read())
    stateFile:close()
    print("Resuming work on slabs " .. (offset + 1) .. " through " .. (offset + count) .. ".")
    print("Continuing at height " .. y .. ", depth " .. z .. ".")
    print("There's a total number of " .. cost .. " blocks left to place.")
  else
    robot.select(SLOT_DIRT)
    print("I'll be working on slabs " .. (offset + 1) .. " through " .. (offset + count) .. ".")
    print("That'll take a total of " .. cost .. " blocks. Put them in a chest in front of me.")
    print("Also put a stack of dirt into my first inventory slot (the selected one).")
  end
end

print("Press any key when ready.")
os.sleep(0.5)
event.pull("key")
print("Starting in 3 seconds...")
os.sleep(3)

local function save()
  local path = fs.path(process.running())
  local stateFile, reason = io.open(fs.concat(path, ".state"), "w")
  if not stateFile then
    io.stderr:write("Failed saving state: " .. tostring(reason))
    return
  end
  stateFile:write(tostring(file:seek("cur")) .. "\n")
  stateFile:write(tostring(y) .. "\n")
  stateFile:write(tostring(z) .. "\n")
  stateFile:write(tostring(offset) .. "\n")
  stateFile:write(tostring(count) .. "\n")
  stateFile:write(tostring(cost) .. "\n")
  stateFile:close()
  print("State saved.")
end

function restock()
  local slot = SLOT_STONE
  robot.select(slot)
  while cost > 0 and slot <= 16 do
    -- fill up this slot, stop if we have all we need.
    while true do
      local count = robot.count(slot)
      local space = robot.space(slot)
      local wantCount = math.min(cost, space) -- avoid overflow
      if wantCount <= 0 then
        break
      end
      robot.suck(wantCount)
      local suckCount = robot.count(slot) - count
      cost = cost - suckCount
      if suckCount == 0 then
        -- not enough materials in chest
        os.sleep(5)
      end
    end
    slot = slot + 1
  end
  local energy = computer.energy()
  if energy < computer.maxEnergy() * 0.5 then
    repeat
      os.sleep(1)
      if computer.energy() < energy and computer.energy() < computer.maxEnergy() * 0.05 then
        print("Emergency shutdown - out of energy!")
        save()
        print("Shutting down in 3 seconds...")
        os.sleep(3)
        computer.shutdown()
      end
    until computer.energy() > computer.maxEnergy() * 0.95
  end
end

local function selectStone()
  local didPrint = false
  while true do
    for slot = SLOT_STONE, 16 do
      if robot.count(slot) > 0 then
        robot.select(slot)
        return
      end
    end
    if not didPrint then
      print("I need more materials!")
      didPrint = true
    end
    os.sleep(5)
  end
end

local function gotoWork()
  -- PRE: looks at chest
  robot.turnLeft()
  repeat until robot.back()
  robot.turnRight()
  for _ = 1, z do
    repeat until robot.back()
  end
  for _ = 1, y do
    repeat until robot.up()
  end
  robot.turnLeft()
  repeat until robot.back()
  repeat until robot.back()
  -- POST: stands at job pos, facing -x
end

local function moveToChest(force)
  -- PRE: stands at job pos, facing -x
  local stones = 0
  for slot = SLOT_STONE, 16 do
    stones = stones + robot.count(slot)
  end
  if force or (stones < width and cost > 0) or (computer.energy() > 0 and computer.energy() < computer.maxEnergy() * 0.1) then
    repeat until robot.forward()
    repeat until robot.forward()
    robot.turnRight()
    for _ = 1, y do
      repeat until robot.down()
    end
    for _ = 1, z do
      repeat until robot.forward()
    end
    robot.turnLeft()
    repeat until robot.forward()
    robot.turnRight()
    restock()
    return true
  end
end

restock()
gotoWork()
for _ = 1, offset do
  file:read()
end
while true do
  local row = file:read()
  if not row then
    moveToChest(true)
    return
  end
  if string.sub(row, 1, 1) == "#" then
    -- skip, handled in z == depth below
    for _ = 1, offset do
      file:read()
    end
  else
    -- PRE: at job pos, facing -x
    selectStone()
    repeat until robot.place()
    local placed = 0
    for x = 1, width do
      if not string.find(row, "1", x, true) then
        break
      end
      repeat until robot.back()
      if string.sub(row, x, x) == "0" then
        robot.select(SLOT_DIRT)
      else
        selectStone()
      end
      repeat until robot.place()
      placed = placed + 1
    end
    repeat until robot.up()
    robot.select(SLOT_DIRT)
    for _ = 1, placed do
      repeat until robot.forward()
      robot.swingDown()
    end
    -- POST: above job pos, facing -x
    if z + 1 < count then
      robot.turnLeft()
      repeat until robot.forward()
      robot.turnRight()
      repeat until robot.down()
      z = z + 1
      if moveToChest() then
        gotoWork()
      end
    else
      for _ = 1, depth - offset - count do
        file:read()
      end
      y = y + 1
      moveToChest(true)
      z = 0
      gotoWork()
    end
  end
end

file:close()
fs.remove(fs.concat(fs.path(process.running()), ".state"))
print("Done!")