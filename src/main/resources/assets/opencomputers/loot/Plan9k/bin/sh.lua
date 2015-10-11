local kernel = require("pipes")
local term = require("term")
local fs = require("filesystem")

local builtin = {}

local function tokenize(cmd)
    local res = {}
    
    local currentWord = ""
    
    for char in string.gmatch(cmd, ".") do
        if char:match("[%w%._%-/~=:]") then
            currentWord = currentWord .. char
        elseif char:match("[|><&]+") then
            if #currentWord > 0 then
                res[#res + 1] = currentWord
                currentWord = ""
            end
            res[#res + 1] = char
        elseif char:match("%s") and #currentWord > 0 then
            res[#res + 1] = currentWord
            currentWord = ""
        end
    end
    if #currentWord > 0 then
        res[#res + 1] = currentWord
    end
    --print("Tokenized: ", table.unpack(res))
    return res
end

local function parse(tokens)
    -- {{arg = {"cat", "file.txt"}, stdin = "-", stdout = 1}, {arg = {"wc", "-c"}, stdin = 1, stdout = "out.txt"}}, 1
    
    local res = {}
    local pipes = 0
    
    local nextin = "-"
    local nextout = "-"
    local arg = {}
    
    local skip = false
    
    for k, token in ipairs(tokens) do
        if not skip then
            if token:match("[%w%._%-/~]+") and not notarg then
                arg[#arg + 1] = token
            elseif token == "|" then
                pipes = pipes + 1
                res[#res + 1] = {arg = arg, stdin = nextin, stdout = pipes}
                nextin = pipes
                nextout = "-"
                arg = {}
            elseif token == "<" and type(nextin) ~= "number" then
                if not tokens[k + 1]:match("[%w%._%-/~]+") then error("Syntax error") end
                nextin = tokens[k + 1]
                skip = true
            elseif token == ">" then
                if not tokens[k + 1]:match("[%w%._%-/~]+") then error("Syntax error") end
                nextout = tokens[k + 1]
                skip = true
            elseif token == ">>" then
                if not tokens[k + 1]:match("[%w%._%-/~]+") then error("Syntax error") end
                nextout = tokens[k + 1]
                skip = true
                print("APPEND MODE IS NOT FULLY IMPLEMENTED")
            elseif token == "&" then
                res[#res + 1] = {arg = arg, stdin = nextin, stdout = nextout, nowait = true}
                nextout = "-"
                nextin = "-"
                arg = {}
            end
        else
            skip = false
        end
    end

    if #arg > 0 then
        res[#res + 1] = {arg = arg, stdin = nextin, stdout = nextout}
    end

    return res, pipes
end

local function resolveProgram(name)
    if builtin[name] then
        return builtin[name]
    end
    for dir in string.gmatch(os.getenv("PATH"), "[^:$]+") do
        if dir:sub(1,1) ~= "/" then
            local dir = fs.concat(os.getenv("PWD") or "/", dir)
        end
        local file = fs.concat(dir, name .. ".lua")
        if fs.exists(file) then
            return file
        end
        file = fs.concat(dir, name)
        if fs.exists(file) then
            return file
        end
    end
end

local function execute(cmd)
    local tokens = tokenize(cmd)
    local programs, npipes = parse(tokens)
    
    local pipes = {}
    
    for i=1, npipes do
        pipes[i] = {io.pipe()}
    end
    
    local processes = {}
    
    for n, program in pairs(programs) do
        local prog = program.arg[1]
        program.arg[1] = resolveProgram(prog)
        if not program.arg[1] then
            print("\x1b[31mProgram '" .. tostring(prog) .. "' was not found\x1b[39m")
            return
        end
    end
    
    local res, message = pcall(function()
        for n, program in pairs(programs) do
            local sin = type(program.stdin) == "number" and pipes[program.stdin][1] or program.stdin == "-" and io.input() or io.open(program.stdin, "r")
            local sout = type(program.stdout) == "number" and pipes[program.stdout][2] or program.stdout == "-" and io.output() or io.open(program.stdout, "w")

            processes[n] = {
                pid = os.spawnp(program.arg[1], sin, sout, io.stderr, table.unpack(program.arg, 2)),
                stdin = sin,
                stdout = sout
            }
        end
    end)

    for n, process in pairs(processes) do
        if not programs[n].nowait then
            kernel.joinThread(process.pid)
        end
        
        if io.output() ~= process.stdout then pcall(function() process.stdout:close() end) end
        if io.input() ~= process.stdin then pcall(function() process.stdin:close() end) end
    end
    
    if not res then
        io.write("\x1b[31m")
        print(message)
        io.write("\x1b[39m")
    end
end

local function expand(value)
    local result = value:gsub("%$(%w+)", os.getenv):gsub("%$%b{}",
        function(match) return os.getenv(expand(match:sub(3, -2))) or match end)
    return result
end

local run = true

builtin.cd = function(dir)
    if not dir then
       os.setenv("PWD", os.getenv("HOME") or "/")
    else
        if dir:sub(1,1) ~= "/" then
            dir = fs.concat(os.getenv("PWD"), dir)
        end
        if not fs.isDirectory(dir) then
            io.stderr:write("cd: " .. tostring(dir) .. ": Not a directory\n")
            return 1
        end
        os.setenv("PWD", dir)
    end
end

builtin.exit = function()
    run = false
end

local term = require("term")
local history = {}

if fs.exists("~/.history") then
    for line in io.lines("~/.history") do
        if #line > 0 then
            table.insert(history, 1, line)
        end
    end
    table.insert(history, 1, "")
end

local function log(cmd)
    local hisfile = io.open("~/.history", "a")
    if #cmd > 0 then
        hisfile:write(cmd .. "\n")
    end
    hisfile:close()
end

while run do
    if term.getCursor() > 1 then
        io.write("\n")
    end
    io.write("\x1b49m\x1b39m")
    io.write(expand(os.getenv("PS1")))
    local cmd = term.read(history)--io.read("*l")
    --print("--IN: ", cmd)
    execute(cmd)
    --print("--OUT: ", cmd)
    log(cmd)
end
