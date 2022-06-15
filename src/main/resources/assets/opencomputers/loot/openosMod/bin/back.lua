local thread = require("thread")
local shell = require("shell")
local su = require("superUtiles")

local args = shell.parse(...)

------------------------------------------

if not _G.programm_loaded then
    _G.programm_loaded = {}
end

local function printUsage()
    print("back load path name ...")
    print("back list")
    print("back stop name")
    print("back start name")
    print("back kill name")
end

------------------------------------------

--сорян за говно код

if args[1] == "load" then
    if not args[2] then print("path is empty") return end
    if not args[3] then print("name is empty") return end
    if programm_loaded[args[3]] then print("this name exists") return end

    local programmArgs = {}
    for i = 4, #args do
        programmArgs[#programmArgs + 1] = args[i]
    end

    local path = shell.resolve(args[2], "lua")
    local data = assert(su.getFile(path))
    data = su.modProgramm(data)
    local func = assert(load(data, "=" .. args[2], nil, su.createEnv()))
    os.setenv("_", path)
    local th = thread.create(func, table.unpack(programmArgs))
    th:detach()
    programm_loaded[args[3]] = th
elseif args[1] == "list" then
    for name in pairs(programm_loaded) do
        print(name)
    end
elseif args[1] == "stop" then
    if not args[2] then print("name is empty") return end

    local th = programm_loaded[args[2]]
    if not th then print("this programm is not loaded") return end
    print(th:suspend())
elseif args[1] == "start" then
    if not args[2] then print("name is empty") return end

    local th = programm_loaded[args[2]]
    if not th then print("this programm is not loaded") return end
    print(th:resume())
elseif args[1] == "kill" then
    if not args[2] then print("name is empty") return end

    local th = programm_loaded[args[2]]
    if not th then print("this programm is not loaded") return end
    print(th:kill())
    programm_loaded[args[2]] = nil
else
    printUsage()
end