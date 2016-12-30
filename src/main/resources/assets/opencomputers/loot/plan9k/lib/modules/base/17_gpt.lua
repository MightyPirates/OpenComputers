local block = kernel.modules.block
local util = kernel.modules.util

local lba = 512

local function scan(device, duuid, dname)
    local h = {}
    device.open(h)
    device.seek(h, "set", 1 * lba)
    local head = device.read(h, 92)
    local sig, rev, hsize, crc, clb, blb, fusablelba, lusablelba,
          uuid, arraystart, partitions, entrysz, partcrc = string.unpack("\60c8c4I4I4xxxxI8I8I8I8c16I8I4I4I4", head)
    if sig ~= "EFI PART" or rev ~= "\0\0\1\0" then
        return
    end
    for i = 1, partitions do
        device.seek(h, "set", arraystart * lba + (i - 1) * entrysz)
        local entry = device.read(h, entrysz)
        local ptype, puuid, pstart, pend, attr, name = string.unpack("\60c16c16I8I8I8c72", entry)
        if ptype ~= ("\0"):rep(16) then
            local size = (pend - pstart + 1) * 512
            block.register(util.binUUID(puuid), dname .. "p" .. (i - 1), kernel.modules.partition.buildDevice(device, pstart * lba, size))
        end
    end
    if device.close then
        device.close(h)
    end
end

kernel.modules.block.scanners.gpt = scan