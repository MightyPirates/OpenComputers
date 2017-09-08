--[[
An adaptation of Wobbo's grep
https://raw.githubusercontent.com/OpenPrograms/Wobbo-Programs/master/grep/grep.lua
]]--

-- POSIX grep for OpenComputers
-- one difference is that this version uses Lua regex, not POSIX regex.

local fs = require("filesystem")
local shell = require("shell")
local tty = require("tty")
local computer = require("computer")

-- Process the command line arguments

local args, options = shell.parse(...)

local gpu = tty.gpu()

local function printUsage(ostream, msg)
  local s = ostream or io.stdout
  if msg then
    s:write(msg,'\n')
  end
  s:write([[Usage: grep [OPTION]... PATTERN [FILE]...
Example: grep -i "hello world" menu.lua main.lua
for more information, run: man grep
]])
end

local PATTERNS = {args[1]}
local FILES = {select(2, table.unpack(args))}

local LABEL_COLOR = 0xb000b0
local LINE_NUM_COLOR = 0x00FF00
local MATCH_COLOR = 0xFF0000
local COLON_COLOR = 0x00FFFF

local function pop(...)
  local result
  for _,key in ipairs({...}) do
    result = options[key] or result
    options[key] = nil
  end
  return result
end

-- Specify the variables for the options
local plain = pop('F','fixed-strings')
      plain = not pop('e','--lua-regexp') and plain
local pattern_file = pop('file')
local match_whole_word = pop('w','word-regexp')
local match_whole_line = pop('x','line-regexp')
local ignore_case = pop('i','ignore-case')
local stdin_label = pop('label') or '(standard input)'
local stderr = pop('s','no-messages') and {write=function()end} or io.stderr
local invert_match = not not pop('v','invert-match')

-- no version output, just help
if pop('V','version','help') then
  printUsage()
  return 0
end

local max_matches = tonumber(pop('max-count')) or math.huge
local print_line_num = pop('n','line-number')
local search_recursively = pop('r','recursive')

-- Table with patterns to check for
if pattern_file then
  local pattern_file_path = shell.resolve(pattern_file)
  if not fs.exists(pattern_file_path) then
    stderr:write('grep: ',pattern_file,': file not found')
    return 2
  end
  table.insert(FILES, 1, PATTERNS[1])
  PATTERNS = {}
  for line in io.lines(pattern_file_path) do
    PATTERNS[#PATTERNS+1] = line
  end
end

if #PATTERNS == 0 then
  printUsage(stderr)
  return 2
end

if #FILES == 0 then
  FILES = search_recursively and {'.'} or {'-'}
end

if not options.h and search_recursively then
  options.H = true
end

if #FILES < 2 then
  options.h = true
end

local f_only = pop('l','files-with-matches')
local no_only = pop('L','files-without-match') and not f_only

local include_filename = pop('H','with-filename')
  include_filename = not pop('h','no-filename') or include_filename

local m_only = pop('o','only-matching')
local quiet = pop('q','quiet','silent')

local print_count = pop('c','count')
local colorize = pop('color','colour') and io.output().tty and tty.isAvailable()

local noop = function(...)return ...;end
local setc = colorize and gpu.setForeground or noop
local getc = colorize and gpu.getForeground or noop

local trim = pop('t','trim')
local trim_front = trim and function(s)return s:gsub('^%s+','')end or noop
local trim_back  = trim and function(s)return s:gsub('%s+$','')end or noop

if next(options) then
  if not quiet then
    printUsage(stderr, 'unexpected option: '..next(options))
    return 2
  end
  return 0
end
-- Resolve the location of a file, without searching the path
local function resolve(file)
  if file:sub(1,1) == '/' then
    return fs.canonical(file)
  else
    if file:sub(1,2) == './' then
      file = file:sub(3, -1)
    end
    return fs.canonical(fs.concat(shell.getWorkingDirectory(), file))
  end
end

--- Builds a case insensitive patterns, code from stackoverflow
--- (questions/11401890/case-insensitive-lua-pattern-matching)
if ignore_case then
  for i=1,#PATTERNS do
    -- find an optional '%' (group 1) followed by any character (group 2)
    PATTERNS[i] = PATTERNS[i]:gsub("(%%?)(.)", function(percent, letter)
      if percent ~= "" or not letter:match("%a") then
        -- if the '%' matched, or `letter` is not a letter, return "as is"
        return percent .. letter
      else -- case-insensitive
        return string.format("[%s%s]", letter:lower(), letter:upper())
      end
    end)
  end
end

local function getAllFiles(dir, file_list)
  for node in fs.list(shell.resolve(dir)) do
    local rel_path = dir:gsub("/+$","") .. '/' .. node
    local resolved_path = shell.resolve(rel_path)
    if fs.isDirectory(resolved_path) then
      getAllFiles(rel_path, file_list)
    else
      file_list[#file_list+1] = rel_path
    end
  end
end

if search_recursively then
  local files = {}
  for i,arg in ipairs(FILES) do
    if fs.isDirectory(arg) then
      getAllFiles(arg, files)
    else
      files[#files+1]=arg
    end
  end
  FILES=files
end

-- Prepare an iterator for reading files
local function readLines()
  local curHand = nil
  local curFile = nil
  local meta = nil
  return function()
    if not curFile then
      local file = table.remove(FILES, 1)
      if not file then
        return
      end
      meta = {line_num=0,hits=0}
      if file == "-" then
        curFile = file
        meta.label = stdin_label
        curHand = io.input()
      else
        meta.label = file
        local file, reason = resolve(file)
        if fs.exists(file) then
          curHand, reason = io.open(file, 'r')
          if not curHand then
            local msg = string.format("failed to read from %s: %s", meta.label, reason)
            stderr:write("grep: ",msg,"\n")
            return false, 2
          else
            curFile = meta.label
          end
        else
          stderr:write("grep: ",file,": file not found\n")
          return false, 2
        end
      end
    end
    meta.line = nil
    if not meta.close and curHand then
      meta.line_num = meta.line_num + 1
      meta.line = curHand:read("*l")
    end
    if not meta.line then
      curFile = nil
      if curHand then
        curHand:close()
      end
      return false, meta
    else
      return meta, curFile
    end
  end
end

local function write(part, color)
  local prev_color = color and getc()
  if color then setc(color) end
  io.write(part)
  if color then setc(prev_color) end
end
local flush=(f_only or no_only or print_count) and function(m)
  if no_only and m.hits == 0 or f_only and m.hits ~= 0 then
    write(m.label, LABEL_COLOR)
    write('\n')
  elseif print_count then
    if include_filename then
      write(m.label, LABEL_COLOR)
      write(':', COLON_COLOR)
    end
    write(m.hits)
    write('\n')
  end
end
local ec = nil
local any_hit_ec = 1
local function test(m,p)
  local empty_line = true
  local last_index, slen = 1, #m.line
  local needs_filename, needs_line_num = include_filename, print_line_num
  local hit_value = 1
  while last_index <= slen and not m.close do
    local i, j = m.line:find(p, last_index, plain)
    local word_fail, line_fail =
      match_whole_word and not (i and not (m.line:sub(i-1,i-1)..m.line:sub(j+1,j+1)):find("[%a_]")),
      match_whole_line and not (i==1 and j==slen)
    local matched = not ((m_only or last_index==1) and not i)
    if (hit_value == 1 and word_fail) or line_fail then
      matched,i,j = false
    end
    if invert_match == matched then break end
    if max_matches == 0 then os.exit(1) end
    any_hit_ec = 0
    m.hits, hit_value = m.hits + hit_value, 0
    if f_only or no_only then
      m.close = true
    end
    if flush or quiet then return end
    if needs_filename then
      write(m.label, LABEL_COLOR)
      write(':', COLON_COLOR)
      needs_filename = nil
    end
    if needs_line_num then
      write(m.line_num, LINE_NUM_COLOR)
      write(':', COLON_COLOR)
      needs_line_num = nil
    end
    local s=m_only and '' or m.line:sub(last_index,(i or 0)-1)
    local g=i and m.line:sub(i,j) or ''
    if i==1 then g=trim_front(g) elseif last_index==1 then s=trim_front(s) end
    if j==slen then g=trim_back(g) elseif not i then s=trim_back(s) end
    write(s)
    write(g, MATCH_COLOR)
    empty_line = false
    last_index = (j or slen)+1
    if m_only or last_index>slen then
      write("\n")
      empty_line = true
      needs_filename, needs_line_num = include_filename, print_line_num
    elseif p:find("^^") and not plain then p="^$" end
  end
  if not empty_line then write("\n") end
  if max_matches ~= math.huge and max_matches >= m.hits then
    m.close = true
  end
end

local uptime = computer.uptime
local last_sleep = uptime()
for meta,status in readLines() do
  if uptime() - last_sleep > 1 then
    os.sleep(0)
    last_sleep = uptime()
  end
  if not meta then
    if type(status) == 'table' then if flush then
      flush(status) end -- this was the last object, closing out
    elseif status then
      ec = status or ec
    end
  else
    for _,p in ipairs(PATTERNS) do
      test(meta,p)
    end
  end
end

return ec or any_hit_ec
