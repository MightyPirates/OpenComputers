local devices = {}
local dfs = kernel.modules.devfs.data
scanners = {}

function register(uuid, name, device, scan)
    if devices[uuid] then
        return
    end
    
    if dfs[name] then
       return
    end
    
    devices[uuid] = {name = name}
    dfs[name] = device
    
    if scan then
        for k, v in pairs(scanners) do
            v(device, uuid, name)
        end
    end
end

function unregister(uuid)
    if not devices[uuid] then
        return
    end
    dfs[devices[uuid].name] = nil
end
