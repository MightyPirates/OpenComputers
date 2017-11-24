local comp = require("component")
local text = require("text")

local dcache = {}
local pcache = {}
local adapter_pwd = "/lib/core/devfs/adapters/"

local adapter_api = {}

function adapter_api.toArgsPack(input, pack)
  local split = text.split(input, {"%s"}, true)
  local req = pack[1]
  local num = #split
  if num < req then return nil, "insufficient args" end
  local result = {n=num}
  for index=1,num do
    local typename = pack[index+1]
    local token = split[index]
    if typename == "boolean" then
      if token ~= "true" and token ~= "false" then return nil, "bad boolean value" end
      token = token == "true"
    elseif typename == "number" then
      token = tonumber(token)
      if not token then return nil, "bad number value" end
    end
    result[index] = token
  end
  return result
end

function adapter_api.createWriter(callback, ...)
  local types = table.pack(...)
  return function(input)
    local args, why = adapter_api.toArgsPack(input, types)
    if not args then return why end
    return callback(table.unpack(args, 1, args.n))
  end
end

function adapter_api.create_toggle(read, write, switch)
  return
  {
    read = read and function() return tostring(read()) end,
    write = write and function(value)
      value = text.trim(tostring(value))
      local on = value == "1" or value == "true"
      local off = value == "0" or value == "false"
      if not on and not off then
        return nil, "bad value"
      end
      if switch then
        (off and switch or write)()
      else
        write(on)
      end
    end
  }
end

function adapter_api.make_link(list, addr, prefix, bOmitZero)
  prefix = prefix or ""
  local zero = bOmitZero and "" or "0"
  local id = 0
  local name
  repeat
    name = string.format("%s%s", prefix, id == 0 and zero or tostring(id))
    id = id + 1
  until not list[name]
  list[name] = {link=addr}
end

return
{
  components =
  {
    list = function()
      local dirs = {}
      local types = {}
      local labels = {}
      local ads = {}

      dirs["by-type"] = {list=function()return types end}
      dirs["by-label"] = {list=function()return labels end}
      dirs["by-address"] = {list=function()return ads end}

      -- first sort the addr, primaries first, then sorted by address lexigraphically
      local hw_addresses = {}
      for addr,type in comp.list() do
        local isPrim = comp.isPrimary(addr)
        table.insert(hw_addresses, select(isPrim and 1 or 2, 1, {type,addr}))
      end

      for _,pair in ipairs(hw_addresses) do
        local type, addr = table.unpack(pair)
        if not dcache[type] then
          local adapter_file = adapter_pwd .. type .. ".lua"
          local loader = loadfile(adapter_file, "bt", _G)
          dcache[type] = loader and loader(adapter_api)
        end
        local adapter = dcache[type]
        if adapter then
          local proxy = pcache[addr] or comp.proxy(addr)
          pcache[addr] = proxy
          ads[addr] =
          {
            list = function()
              local devfs_proxy = adapter(proxy)
              devfs_proxy.address = {proxy.address}
              devfs_proxy.slot = {proxy.slot}
              devfs_proxy.type = {proxy.type}
              devfs_proxy.device = {device=proxy}
              return devfs_proxy
            end
          }

          -- by type building
          local type_dir = types[type] or {list={}}
          adapter_api.make_link(type_dir.list, "../../by-address/"..addr)
          types[type] = type_dir

          -- by label building (labels are only supported in filesystems
          local label = require("devfs").getDeviceLabel(proxy)
          if label then
            adapter_api.make_link(labels, "../by-address/"..addr, label, true)
          end
        end
      end
      return dirs
    end
  },
}
