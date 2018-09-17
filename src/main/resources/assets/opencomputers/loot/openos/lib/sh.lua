local process = require("process")
local shell = require("shell")
local text = require("text")
local tx = require("transforms")

local sh = {}
sh.internal = {}

function sh.internal.isWordOf(w, vs)
 return w and #w == 1 and not w[1].qr and tx.first(vs,{{w[1].txt}}) ~= nil
end

local isWordOf = sh.internal.isWordOf

-------------------------------------------------------------------------------

--SH API

sh.internal.ec = {}
sh.internal.ec.parseCommand = 127
sh.internal.ec.last = 0

function sh.getLastExitCode()
  return sh.internal.ec.last
end

function sh.internal.command_result_as_code(ec, reason)
  -- convert lua result to bash ec
  local code
  if ec == false then
    code = 1
  elseif ec == nil or ec == true then
    code = 0
  elseif type(ec) ~= "number" then
    code = 2 -- illegal number
  else
    code = ec
  end

  if reason and code ~= 0 then io.stderr:write(reason, "\n") end
  return code
end

function sh.internal.resolveActions(input, resolved)
  resolved = resolved or {}

  local processed = {}

  local prev_was_delim = true
  local words, reason = text.internal.tokenize(input)

  if not words then
    return nil, reason
  end

  while #words > 0 do
    local next = table.remove(words,1)
    if isWordOf(next, {";","&&","||","|"}) then
      prev_was_delim = true
      resolved = {}
    elseif prev_was_delim then
      prev_was_delim = false
      -- if current is actionable, resolve, else pop until delim
      if next and #next == 1 and not next[1].qr then
        local key = next[1].txt
        if key == "!" then
          prev_was_delim = true -- special redo
        elseif not resolved[key] then
          resolved[key] = shell.getAlias(key)
          local value = resolved[key]
          if value and key ~= value then
            local replacement_tokens, resolve_reason = sh.internal.resolveActions(value, resolved)
            if not replacement_tokens then
              return replacement_tokens, resolve_reason
            end
            words = tx.concat(replacement_tokens, words)
            next = table.remove(words,1)
          end
        end
      end
    end

    table.insert(processed, next)
  end

  return processed
end

-- returns true if key is a string that represents a valid command line identifier
function sh.internal.isIdentifier(key)
  if type(key) ~= "string" then
    return false
  end

  return key:match("^[%a_][%w_]*$") == key
end

-- expand (interpret) a single quoted area
-- examples: $foo or "$foo"
function sh.expand(value)
  local expanded = value
  :gsub("%$([_%w%?]+)", function(key)
    if key == "?" then
      return tostring(sh.getLastExitCode())
    end
    return os.getenv(key) or ''
  end)
  :gsub("%${(.*)}", function(key)
    if sh.internal.isIdentifier(key) then
      return os.getenv(key) or ''
    end
    io.stderr:write("${" .. key .. "}: bad substitution\n")
    os.exit(1)
  end)
  return expanded
end

function sh.internal.createThreads(commands, env, start_args)
  -- Piping data between programs works like so:
  -- program1 gets its output replaced with our custom stream.
  -- program2 gets its input replaced with our custom stream.
  -- repeat for all programs
  -- custom stream triggers execution of "next" program after write.
  -- custom stream triggers yield before read if buffer is empty.
  -- custom stream may have "redirect" entries for fallback/duplication.
  local threads = {}
  for i = 1, #commands do
    local command = commands[i]
    local program, args, redirects = table.unpack(command)
    local name = tostring(program)
    local thread_env = type(program) == "string" and env or nil
    local thread, reason = process.load(program or "/dev/null", thread_env, function(...)
      if redirects then
        sh.internal.openCommandRedirects(redirects)
      end

      args = tx.concat(args, start_args[i] or {}, table.pack(...))

      -- popen expects each process to first write an empty string
      -- this is required for proper thread order
      io.write("")
      return table.unpack(args, 1, args.n or #args)
    end, name)

    if not thread then
      for _,t in ipairs(threads) do
        process.internal.close(t)
      end
      return nil, reason
    end

    threads[i] = thread

  end

  if #threads > 1 then
    require("pipe").buildPipeChain(threads)
  end

  return threads
end

function sh.internal.executePipes(pipe_parts, eargs, env)
  local commands = {}
  for _,words in ipairs(pipe_parts) do
    local args = {}
    local reparse
    for _,word in ipairs(words) do
      local value = ""
      for _,part in ipairs(word) do
        reparse = reparse or part.qr or part.txt:find("[%$%*%?<>]")
        value = value .. part.txt
      end
      args[#args + 1] = value
    end

    local redirects
    if reparse then
      args, redirects = sh.internal.evaluate(words)
      if not args then
        return false, redirects -- in this failure case, redirects has the error message
      end
    end

    commands[#commands + 1] = table.pack(table.remove(args, 1), args, redirects)
  end

  local threads, reason = sh.internal.createThreads(commands, env, {[#commands]=eargs})  
  if not threads then return false, reason end
  return process.internal.continue(threads[1])
end

function sh.execute(env, command, ...)
  checkArg(2, command, "string")
  if command:find("^%s*#") then return true, 0 end

  local words, reason = sh.internal.resolveActions(command)
  if type(words) ~= "table" then
    return words, reason
  elseif #words == 0 then
    return true, 0
  end

  -- MUST be table.pack for non contiguous ...
  local eargs = table.pack(...)

  -- simple
  if not command:find("[;%$&|!<>]") then
    sh.internal.ec.last = sh.internal.command_result_as_code(sh.internal.executePipes({words}, eargs, env))
    return true
  end

  return sh.internal.execute_complex(words, eargs, env)
end

function sh.hintHandler(full_line, cursor)
  return sh.internal.hintHandlerImpl(full_line, cursor)
end

require("package").delay(sh, "/lib/core/full_sh.lua")

return sh
