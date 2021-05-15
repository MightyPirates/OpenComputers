local process = require("process")
local unicode = require("unicode")
local event = require("event")
local event_mt = getmetatable(event.handlers)

-- WARNING this code does not use official kernel API and is likely to change

local data = {}
local widths = {}
local sorted = {}
local moved_indexes = {}

local elbow = unicode.char(0x2514)

local function thread_id(t,p)
  if t then
    return tostring(t):gsub("^thread: 0x", "")
  end
  -- find the parent thread
  for k,v in pairs(process.list) do
    if v == p then
      return thread_id(k)
    end
  end
  return "-"
end

local cols =
{
  {"PID", thread_id},
  {"EVENTS", function(_,p)
    local handlers = {}
    if event_mt.threaded then
      handlers = rawget(p.data, "handlers") or {}
    elseif not p.parent then
      handlers = event.handlers
    end
    local count = 0
    for _ in pairs(handlers) do
      count = count + 1
    end
    return count == 0 and "-" or tostring(count)
  end},
  {"THREADS", function(_,p)
    -- threads are handles with mt.close == thread.waitForAll
    local count = 0
    for _,h in ipairs(p.data.handles) do
      local mt = getmetatable(h)
      if mt and mt.__status then
        count = count + 1
      end
    end
    return count == 0 and "-" or tostring(count)
  end},
  {"PARENT", function(_,p)
    for _,process_info in pairs(process.list) do
      for i,handle in ipairs(process_info.data.handles) do
        local mt = getmetatable(handle)
        if mt and mt.__status then
          if mt.process == p then
            return thread_id(nil, process_info)
          end
        end
      end
    end
    return thread_id(nil, p.parent)
  end},
  {"HANDLES", function(_, p)
    local count = #p.data.handles
    return count == 0 and "-" or tostring(count)
  end},
  {"CMD", function(_,p) return p.command end},
}

local function add_field(key, value)
  if not data[key] then data[key] = {} end
  table.insert(data[key], value)
  widths[key] = math.max(widths[key] or 0, #value)
end

for _,key in ipairs(cols) do
  add_field(key[1], key[1])
end

for thread_handle, process_info in pairs(process.list) do
  for _,key in ipairs(cols) do
    add_field(key[1], key[2](thread_handle, process_info))
  end
end

local parent_index
for index,set in ipairs(cols) do
  if set[1] == "PARENT" then
    parent_index = index
    break
  end
end
assert(parent_index, "did not find a parent column")

local function move_to_sorted(index)
  if moved_indexes[index] then
    return false
  end
  local entry = {}
  for k,v in pairs(data) do
    entry[k] = v[index]
  end
  sorted[#sorted + 1] = entry
  moved_indexes[index] = true
  return true
end

local function make_elbow(depth)
  return (" "):rep(depth - 1) .. (depth > 0 and elbow or "")
end

-- remove COLUMN labels to simplify sort
move_to_sorted(1)

local function update_family(parent, depth)
  depth = depth or 0
  parent = parent or "-"
  for index in ipairs(data.PID) do
    local this_parent = data[cols[parent_index][1]][index]
    if this_parent == parent then
      local dash_cmd = make_elbow(depth) .. data.CMD[index]
      data.CMD[index] = dash_cmd
      widths.CMD = math.max(widths.CMD or 0, #dash_cmd)
      if move_to_sorted(index) then
        update_family(data.PID[index], depth + 1)
      end
    end
  end
end

update_family()
table.remove(cols, parent_index) -- don't show parent id

for _,set in ipairs(sorted) do
  local split = ""
  for _,key in ipairs(cols) do
    local label = key[1]
    local format = split .. "%-" .. tostring(widths[label]) .. "s"
    io.write(string.format(format, set[label]))
    split = "   "
  end
  print()
end

