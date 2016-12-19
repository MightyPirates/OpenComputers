local kernel = require("pipes")
local term = require("term")
local text = require("text")
local shell = require("shell")
local fs = require("filesystem")

local arg, opt = shell.parse(...)

local alias = {}
local builtin = {}

-------------------
-- Interpreter
-- TODO: Move to lib

local function parse(tokens)
    -- {{arg = {"cat", "file.txt"}, stdin = "-", stdout = 1}, {arg = {"wc", "-c"}, stdin = 1, stdout = "out.txt"}}, 1
    
    local res = {}
    local pipes = 0
    
    local nextin = "-"
    local nextout = "-"
    local nexterr = nil
    local arg = {}
    
    local skip = false
    
    for k, token in ipairs(tokens) do
        if not skip then
            if token:match("[%w%._%-/~]+") and not notarg then
                local tlen = #token
                if (token:sub(1,1)=="\"" or token:sub(1,1)=="'") and (token:sub(tlen,tlen)=="\"" or token:sub(tlen,tlen)=="'") then
                    token = token:sub(2, tlen - 1)
                end
                if #arg == 0 and alias[token] then
                    for _, v in pairs(alias[token]) do
                        arg[#arg + 1] = v
                    end
                else
                    arg[#arg + 1] = token
                end
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
            elseif token == "2>" then
                if not tokens[k + 1]:match("[%w%._%-/~]+") then error("Syntax error") end
                nexterr = tokens[k + 1]
                skip = true
            elseif token == ">>" then
                if not tokens[k + 1]:match("[%w%._%-/~]+") then error("Syntax error") end
                nextout = tokens[k + 1]
                skip = true
                print("APPEND MODE IS NOT IMPLEMENTED")
            elseif token == "&" then
                res[#res + 1] = {arg = arg, stdin = nextin, stdout = nextout, stderr = nexterr, nowait = true}
                nextout = "-"
                nextin = "-"
                nexterr = nil
                arg = {}
            end
        else
            skip = false
        end
    end

    if #arg > 0 then
        res[#res + 1] = {arg = arg, stdin = nextin, stdout = nextout, stderr = nexterr}
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
    local tokens = text.tokenize(cmd)
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
            local serr = program.stderr and (program.stderr == "-" and io.output() or io.open(program.stderr, "w")) or io.stderr

            processes[n] = {
                pid = os.spawnp(program.arg[1], sin, sout, serr, table.unpack(program.arg, 2)),
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

local function script(file)
    for line in io.lines(file) do
        if line:sub(1,1) ~= "#" then
            if opt.x then
                print("> " .. line)
            end
            execute(text.trim(line))
        end
    end
end

-------------------
-- Builtins

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

builtin.alias = function(what, ...)
    if not what then
        print("Usage: alias <alias> [command] [arguments...]")
        return
    end
    alias[what] = {...}
end

builtin.unalias = function(what)
    if not what then
        print("Usage: unalias <alias>")
        return
    end
    alias[what] = nil
end

-------------------
-- Logging

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
    local hisfile, err = io.open("~/.history", "a")
        if hisfile then
        if #cmd > 0 then
            hisfile:write(cmd .. "\n")
        end
        hisfile:close()
    else
        io.stderr:write("Error writing to histfile: " .. tostring(err))
    end
end

-------------------
-- Tab completion

local function escapeMagic(text)
    return text:gsub('[%(%)%.%%%+%-%*%?%[%^%$]', '%%%1')
end

local function getMatchingPrograms(baseName)
    local result = {}
    -- TODO only matching files with .lua extension for now, might want to
    --      extend this to other extensions at some point? env var? file attrs?
    if not baseName or #baseName == 0 then
        baseName = "^(.*)%.lua$"
    else
        baseName = "^(" .. escapeMagic(baseName) .. ".*)%.lua$"
    end
    for basePath in string.gmatch(os.getenv("PATH"), "[^:]+") do
        for file in fs.list(basePath) do
            local match = file:match(baseName)
            if match then
                table.insert(result, match)
            end
        end
    end
    return result
end

local function getMatchingFiles(basePath, name)
    local resolvedPath = shell.resolve(basePath)
    local result, baseName = {}
    
    -- note: we strip the trailing / to make it easier to navigate through
    -- directories using tab completion (since entering the / will then serve
    -- as the intention to go into the currently hinted one).
    -- if we have a directory but no trailing slash there may be alternatives
    -- on the same level, so don't look inside that directory... (cont.)
    if fs.isDirectory(resolvedPath) and name:len() == 0 then
        baseName = "^(.-)/?$"
    else
        baseName = "^(" .. escapeMagic(name) .. ".-)/?$"
    end
    
    for file in fs.list(resolvedPath) do
        local match = file:match(baseName)
        if match then
            table.insert(result, basePath ..  match)
        end
    end
    -- (cont.) but if there's only one match and it's a directory, *then* we
    -- do want to add the trailing slash here.
    if #result == 1 and fs.isDirectory(result[1]) then
        result[1] = result[1] .. "/"
    end
    return result
end

local function hintHandler(line, cursor)
    local line = unicode.sub(line, 1, cursor - 1)
    if not line or #line < 1 then
        return nil
    end
    local result
    local prefix, partial = string.match(line, "^(.+%s)(.+)$")
    local searchInPath = not prefix and not line:find("/")
    if searchInPath then
        -- first part and no path, look for programs in the $PATH
        result = getMatchingPrograms(line)
    else -- just look normal files
        local partialPrefix = (partial or line)
        local name = partialPrefix:gsub("/+", "/")
        name = name:sub(-1) == '/' and '' or fs.name(name)
        partialPrefix = partialPrefix:sub(1, -name:len() - 1)
        result = getMatchingFiles(partialPrefix, name)
    end
    local resultSuffix = ""
    if searchInPath then
        resultSuffix  = " "
    elseif #result == 1 and result[1]:sub(-1) ~= '/' then
        resultSuffix = " "
    end
    prefix = prefix or ""
    for i = 1, #result do
        result[i] = prefix .. result[i] .. resultSuffix
    end
    table.sort(result)
    return result
end

-------------------
-- Built-in aliases

builtin.alias("la", "ls", "-a")
builtin.alias("la", "ll", "-la")
builtin.alias("lh", "ls", "-h")
builtin.alias("help", "man")
builtin.alias("service", "rc")
builtin.alias("upgrade", "mpt", "-Syu")
builtin.alias("pacman", "mpt")

builtin.alias("..", "cd", "..")
builtin.alias("...", "cd", "../..")
builtin.alias("....", "cd", "../../..")


-------------------
-- Main loop

if arg[1] and fs.exists(arg[1]) then
    script(arg[1])
    return
end

if fs.exists("~/.shrc") then
    script(arg[1])
end

while run do
    --if term.getCursor() > 1 then
    --    io.write("\n")
    --end
    io.write("\x1b[49m\x1b[39m")
    io.write(expand(os.getenv("PS1")))
    local cmd = term.read(history, nil, hintHandler)--io.read("*l")
    --print("--IN: ", cmd)
    execute(cmd)
    --print("--OUT: ", cmd)
    log(cmd)
end
