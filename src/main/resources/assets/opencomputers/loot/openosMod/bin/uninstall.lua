local fs = require("filesystem")
local shell = require("shell")
local component = require("component")
local su = require("superUtiles")

--------------------------------------------

print("список программ которые можно удалить утилитой uninstall")

local programms = {}
local drives = {}
local names = {}
for address in component.list("filesystem") do
    local proxy = component.proxy(address)
    for _, file in ipairs(proxy.list("/free/uninstallers") or {}) do
        local full_path = fs.concat(su.getMountPoint(proxy.address), "/free/uninstallers", file)
        local lfs = fs.get(full_path)

        local drive = (lfs.getLabel() or "noLabel") .. ":" .. lfs.address:sub(1, 5)
        print(tostring(#programms + 1) .. ".", "programm: " .. file, su.getFullInfoParts(address))
        table.insert(programms, full_path)
        table.insert(drives, drive)
        table.insert(names, file)
    end
end

if #programms == 0 then
    print("у вас нечего удалять")
    return
end

::tonew::
io.write("введите номер программы: ")
local read = io.read()
if not read then return end
local num = tonumber(read)
if not num or num < 1 or num > #programms then
    print("ошибка ввода, ввидите номер программы или нажмите ctrl + c чтобы покинуть unintaller")
    goto tonew
end

io.write("вы уверены что хотите удалить " .. names[num] .. " с диска " .. drives[num] .. " [Y/y] ")
local ok = io.read()
if ok ~= "y" and ok ~= "Y" and ok ~= "" then ok = false end
if not ok then return end

os.execute(programms[num])
print("удаления завершено")