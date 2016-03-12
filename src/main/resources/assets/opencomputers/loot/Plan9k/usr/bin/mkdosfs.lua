local fs = require "filesystem"
local args = {...}
 
local dev = args[1] or error("Must specify a file")
 
local device = fs.open(dev, "wb")
 
local sectors = math.floor(device:seek("end", 0)/512)
print(sectors .. " sectors")
device:seek("set", 0)
 
local fat12 = sectors/4 < 4085
local fatSize = math.ceil(sectors*(fat12 and 1.5 or 2)/2048)
 
local startseq = "\xEB\x3C\x90ocdosfs\0"
local bytesPerSector = "\0\2"
local sectorsPerCluster = "\4"
local reservedSectors = "\1\0"
local fatnum = "\2"
local dirent = "\0\2"
local sectors = string.char(sectors%256) .. string.char(math.floor(sectors/256))
local descriptor = "\xF8"
local sectorsPerFat = string.char(fatSize%256) .. string.char(math.floor(fatSize/256))
local sectorsPerTrack = "\x20\0"
local heads = "\x40\0"
local hiddenSect = "\0\0\0\0"
local largeSectorAmount = "\0\0\0\0"
 
local drivenum = "\x80"
local ntflags = "\0"
local signature = "\x29"
local id = "\0\0\0\0"
local label = "NO NAME    "
local sysid = fat12 and "FAT12   " or "FAT16   "
local bootcode = ("\0"):rep(448)
local bootsig = "\x55\xAA"
 
local bootRecord = startseq .. bytesPerSector .. sectorsPerCluster .. reservedSectors .. fatnum .. dirent .. sectors .. descriptor
    .. sectorsPerFat .. sectorsPerTrack .. heads .. hiddenSect .. largeSectorAmount
    .. drivenum .. ntflags .. signature .. id .. label .. sysid .. bootcode .. bootsig
 
print("Boot record size: " .. #bootRecord)
 
device:write(bootRecord)
 
print("Cleaning FAT tables")
os.sleep(0)
for i = 1, 2 do
    device:write("\xF8" .. ("\xFF"):rep(fat12 and 2 or 3) .. ("\0"):rep(fat12 and 509 or 508))
    for i = 1, fatSize-1 do
        device:write(("\0"):rep(512))
    end
end
 
print("Cleaning Root Directory")
os.sleep(0)
 
for i = 1, 512 do
    device:write(("\0"):rep(32))
end
