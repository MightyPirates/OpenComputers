local component = require("component")
local filesystem = require("filesystem")
local term = require("term")
local unicode = require("unicode")
local shell = require("shell")

local args, options = shell.parse(...)

-------------------------------------------

local function getfile(fs, path)
    local buffer = ""

    local file, err = fs.open(path)
    if not file then
        return nil, err
    end
    while true do
        local read = fs.read(file, math.huge)
        if not read then
            break
        end
        buffer = buffer .. read
    end
    fs.close(file)

    return buffer
end

local function savefile(fs, path, data)
    local file, err = fs.open(path, "w")
    if not file then
        return nil, err
    end
    fs.write(file, data)
    fs.close(file)
end

local function install(fs, fs2, path)
    local files = fs.list(path)
    for i = 1, #files do
        local file = filesystem.concat(path, files[i])
        if fs.isDirectory(file) then
            fs2.makeDirectory(file)
            install(fs, fs2, file)
        else
            if unicode.sub(filesystem.name(file), 1, 1) ~= "." or options.a then
                savefile(fs2, file, getfile(fs, file))
            end
        end
    end
end

local function input()
    local data = term.read()
    if not data then
        print("canceled.")
        os.exit()
    end
    return data:sub(1, #data - 1)
end

-------------------------------------------

local filesystems = {}
for address in component.list("filesystem") do
    local proxy = component.proxy(address)
    print(tostring(#filesystems + 1)..")."..address.."|"..(proxy.getLabel() or ""))
    filesystems[#filesystems + 1] = proxy
end

-------------------------------------------

local function getFs(str)
    while true do
        term.write("input number "..str.." drive: ")
        local number = tonumber(input())
        if number then
            if number >= 1 and number <= #filesystems then
                return filesystems[number]
            else
                print("invalid number")
            end
        else
            print("invalid input")
        end
    end
end

local from = getFs("FROM")
local to = getFs("TO")
if from.address == to.address then
    print("drive equals.")
    return
end

term.write("check data! input [Y/n]")
local data = input()
if data == "Y" or data == "y" or data == "" then
    install(from, to, "/")
else
    print("canceled.")
end