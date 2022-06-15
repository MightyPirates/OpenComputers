local fs = require("filesystem")
local shell = require("shell")
local su = require("superUtiles")

local args, options = shell.parse(...)

if #args == 0 then
    print("Usage:")
    print("programs list - выводит список программ зарегистрированые в ос")
    print("programs allList - выводит список всех программ которые можно открыть в данный момент, повторяюшиеся имена игнорируються")
    print("programs userList - работает аналогичьно allList однако игнорирует все программы чье имя содержиться в /bin даже если сама программа не лежит в bin")
    print("programs command name command(uninstall/run/lalala/blablabla/any)")
    return
end

--------------------------------------------

if args[1] == "list" then
    for file in fs.list("/free/programs/menagers") do
        print(file)
    end
elseif args[1] == "allList" or args[1] == "userList" then
    local paths = su.split(shell.getPath(), ":")
    local names = {}
    for i, programmPath in ipairs(paths) do
        for file in fs.list(fs.xconcat(shell.getWorkingDirectory(), programmPath)) do
            local full_path = fs.xconcat(shell.getWorkingDirectory(), programmPath, file)
            local name = fs.name(file)

            if not su.inTable(names, name) then
                if not fs.isDirectory(full_path) or fs.exists(fs.concat(full_path, "main.lua")) then
                    if fs.isDirectory(full_path) then
                        full_path = fs.concat(full_path, "main.lua")
                    end
                    if su.endAt(full_path, ".") == "lua" then --если разширения файла lua
                        if args[1] ~= "userList" or not fs.exists(fs.concat("/bin", name)) then --под исключения попадают все файлы котопые присутствуют в системной папки bin даже если лежат в bin на другом диске
                            table.insert(names, name)
                            print(su.startAt(name, "."))
                        end
                    end
                end
            end
        end
    end
elseif args[1] == "command" then
    local programmPath = fs.concat("/free/programs/menagers", args[2])
    if fs.exists(programmPath) then
        require("shell").execute(programmPath, nil, args[3])
    else
        print("no this programm")
    end
end