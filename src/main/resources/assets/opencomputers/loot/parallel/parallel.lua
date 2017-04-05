local parallel = {}

-- Sanitization happens here
function parallel.spawn(fn, ...)
  local err
  if type(fn) == "function" then 
    fn = fn
  elseif type(fn) == "string" then
    fn, err = load(fn)
    if not fn then return false, err end
  elseif false then -- TODO: Add support for file handles
  else
    return false, "Not valid function or string"
  end
  local co = coroutine.create(fn)
  if not co then return false, "Failed to create thread" end
  return coroutine.yield({ 
    ["flag"] = "parallel_spawn",
    ["co"] = co,
    ["args"] = { ... },
    ["name"] = "anonymous",
  })
end
function parallel.yield( sleep_filter, filter_sleep )
  local sleep, filter
  if type(sleep_filter) == "number" then sleep = sleep_filter; filter = filter_sleep end
  if type(sleep_filter) == "string" then sleep = filter_sleep; filter = sleep_filter end
  
  return coroutine.yield({
    ["flag"] = "parallel_yield",
    ["filter"] = filter,
    ["sleep"] = sleep
  })
end
function parallel.pause(uid, amount)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  if type(amount) ~= "number" then amount = 1 end
  return coroutine.yield({
    ["flag"] = "parallel_pause",
    ["uid"] = uid,
    ["amount"] = amount,
  })  
end
function parallel.unpause(uid, amount)
  local amount = -(amount or 1)
  return parallel.pause( uid, amount )
end
function parallel.kill(uid)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_kill",
    ["uid"] = uid,
  })
end
function parallel.dig(uid)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_dig",
    ["uid"] = uid,
  })
end
function parallel.setName(uid, name)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  local ok, err = pcall(checkArg, 1, name, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_setName",
    ["uid"] = uid,
    ["name"] = name,
  })
end
function parallel.getName(uid)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_getName",
    ["uid"] = uid,
  })
end
function parallel.setAlarm( uid, alarm )
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  local ok, err = pcall(checkArg, 1, alarm, "number", "nil"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_setAlarm",
    ["uid"] = uid,
    ["alarm"] = alarm,
  })
end
function parallel.getAlarm( uid )
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_getAlarm",
    ["uid"] = uid,
  })
end
function parallel.setFilter( uid, filter )
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  local ok, err = pcall(checkArg, 1, filter, "string", "nil"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_setFilter",
    ["uid"] = uid,
    ["filter"] = filter,
  })
end
function parallel.getFilter( uid )
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_getFilter",
    ["uid"] = uid,
  })
end
function parallel.setAnswer(uid, answer)
  if answer ~= nil then
    local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  else
    answer = uid
    uid = parallel.getThread()
  end  
  return coroutine.yield({
    ["flag"] = "parallel_setAnswer",
    ["uid"] = uid,
    ["answer"] = answer,
  })  
end
function parallel.getAnswer(uid, msg)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_getAnswer",
    ["uid"] = uid,
    ["msg"] = msg,
  })  
end
function parallel.ask(uid, msg)
  return parallel.getAnswer(uid, msg)
end
function parallel.whisper(uid, ...)
  local ok, err = pcall(checkArg, 1, uid, "string"); if not ok then return ok, err end
  return coroutine.yield({
    ["flag"] = "parallel_whisper",
    ["uid"] = uid,
    ["args"] = { ... },
  })  
end
function parallel.getThreads()
  return coroutine.yield({
    ["flag"] = "parallel_getThreads"
  })
end
function parallel.getThread( uid )
  return coroutine.yield({
    ["flag"] = "parallel_getThread",
    ["uid"] = uid,
  })
end
function parallel.exit()
  return coroutine.yield({
    ["flag"] = "parallel_exit",
  })
end

-- Magic happens here
function parallel.manager( firstThread )
  local computer = require("computer")
  -- You should provide better versions of these functions.
  local countUid = 0
  local getUid = getUid or function ()
      countUid = countUid + 1
      return tostring(countUid)
    end
  local utils = utils or false
  if not utils then
    utils = {
      getTime = function () return os.clock()*60 end,
    }
  end
  local logger = logger or false
  if not logger then 
    logger = {
      fatal = function (...) io.write(string.format(...)) end,
      warning = function (...) io.write(string.format(...)) end,
      info = function (...) io.write(string.format(...)) end,
      spam = function (...) io.write(string.format(...)) end,
    }
  end
  if not firstThread then 
    firstThread = {
      ["name"] = "Default console",
      ["uid"] = getUid("coroutine"),
      ["co"] = coroutine.create(
        function ()
          local _ENV = setmetatable(_ENV,{__index=_G})
          local function read(history)
            local component = require("component")
            local prefix = "P# "
            local history = history
            if type(history) ~= "table" then history = {} end
            history[#history] = ""
            local command = ""
            local historyCursor = #history
            local cursorX, cursorY = 1,1
            while true do
              local newline = false
              local resX, resY = component.gpu.getResolution()
              for i=1,#prefix do
                if string.sub(prefix,i,i) ~= component.gpu.get(i,resY) then newline = true end
              end
              if newline then
                component.gpu.copy(1,1,resX,resY,0,-1)
                component.gpu.fill(1,resY,resX,1," ")
              end
              local output = prefix..string.sub(command,-(resX-3))
              for i=1,resX-#output do output = output.." " end
              component.gpu.set(1,resY,output)
              
              local name, address, charOrValue, code, player = coroutine.yield("[kc][el][yi][_p][db][oo][wa][nr]d?")
              if name == "clipboard" then player = code; code = nil end
              
              if name == "key_down" then
                if code == 28 then -- enter
                  component.gpu.fill(1,resY,resX,1," ")
                  return command
                elseif code == 14 then -- backspace
                  command = string.sub(history[historyCursor],1,#history[historyCursor]-1)
                  history[#history] = command
                  historyCursor = #history                  
                elseif code == 211 then -- del
                  command = ""
                  history[#history] = command
                  historyCursor = #history    
                elseif code == 200 then -- arrow up
                  historyCursor = math.max(1, historyCursor-1)
                  command = history[historyCursor]
                elseif code == 208 then -- arrow down
                  historyCursor = math.min(#history, historyCursor+1)
                  command = history[historyCursor]
                  historyCursor = #history
                elseif not(type(charOrValue) == "number" and (charOrValue < 0x20 or (charOrValue >= 0x7F and charOrValue <= 0x9F))) then -- is normal char
                  command = history[historyCursor]..string.char(charOrValue)
                  history[#history] = command
                  historyCursor = #history
                end
              elseif name == "clipboard" then
                command = history[historyCursor]..charOrValue
                history[#history] = command
                historyCursor = #history
                if string.find(command,"\n",-1, true) then 
                  component.gpu.fill(1,resY,resX,1," ")
                  return command 
                end
              end
            end
          end
          local history = {}
          while true do
            local str = read(history) -- yields here!
            history[#history+1] = str
            --local formattedTime = ""
            --if os.clock()*60 >= 60 * 60 then formattedTime = formattedTime .. string.format("%dh",os.clock/60) end
            --if os.clock()*60 >= 60 then formattedTime = formattedTime .. string.format("%dm",os.clock()-math.floor(os.clock()/60)) end
            --formattedTime = formattedTime .. string.format("%is",os.clock()*60-math.floor(os.clock())*60)            
            local fn, err = load(str) -- ,"console@"..formattedTime
            if fn and str ~="" and str ~="\n" then 
              print("executed:",pcall(fn))
            else
              if err then print("load error:", err) end
            end
          end
        end
        ),
      ["filter"] = nil,
      ["pause"] = 0,
      ["sleep"] = false,
      ["answer"] = nil,
      ["argSets"] = {},
      ["lastArgSet"] = 1,
    }
  end
  
  local event = {"parallel_dummy"}
  local focus, lastThread = 1, 1
  local threads = { [1] = firstThread } -- TODO: Sanitize
  local graveyard = {}
  local graveyardTime = {}

  local operations = {
    parallel_yield = function (data)
      threads[focus].filter = data.filter or false
      if data.sleep then threads[focus].alarm = data.sleep + utils.getTime() end
      return nil -- Cycle to next thread
    end,
    parallel_spawn = function (data)
      lastThread = lastThread + 1
      threads[ lastThread ] = {
        ["name"] = data.name or "anonymous",
        ["filter"] = data.filter or nil,
        ["pause"] = 0,
        ["argSets"] = { 
          [1] = data.args or nil },
        ["uid"] = getUid("coroutine"),
        ["co"] = data.co,
      }
      if data.args then threads[ lastThread ].lastArgSet = 1 else threads[ lastThread ].lastArgSet = 0 end
      return threads[ lastThread ].uid
    end,
    parallel_whisper = function (data)
      for _,thread in pairs(threads) do
        if thread.uid == data.uid then
          if 
            ((not thread.filter) or (thread.filter == data.args[1]))
            and ((not thread.alarm) or thread.alarm < utils.getTime())
          then
            thread.filter = nil
            thread.lastArgSet = thread.lastArgSet + 1 
            thread.argSets[ thread.lastArgSet ] = data.args or nil
            return true
          end
          local err = "Thread found"
          if thread.filter then err = err..", but filters for "..type(thread.filter).." "..thread.filter end
          if thread.alarm then err = err..", but has alarm on "..thread.alarm.." after "..(utils.getTime() - thread.alarm).." seconds" end
          return false, err, thread.filter, thread.alarm, utils.getTime() - thread.alarm
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_pause = function (data)
      for _,thread in pairs(threads) do
        if thread.uid == data.uid then
          if thread.pause == 0 and data.amount > 0 then
            if thread.alarm then
              thread.alarm = thread.alarm - utils.getTime()
              if thread.alarm < 0 then thread.alarm = false end
            end
          end
          if thread.pause > 0 and thread.pause + data.amount <= 0 then
            if thread.alarm then thread.alarm = thread.alarm + utils.getTime() end
          end
          thread.pause = math.max(0, thread.pause + data.amount)
          return true
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_setAnswer = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then 
          threads[i].answer = data.answer
          return true
        end
      end
      return false, "No running thread found, try to dig" 
    end,     
    parallel_getAnswer = function (data)
      for _,thread in pairs(threads) do
        if thread.uid == data.uid and thread.answer then
          if type(thread.answer) == "function" then
            return thread.answer(data.msg)
          else
            return thread.answer
          end
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_kill = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then 
          graveyard[ threads[i].uid ] = { false, "killed", threads[focus].uid, utils.getTime() }
          graveyardTime[ threads[i].uid ] = utils.getTime()
          threads[i] = nil
          return true
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_dig = function (data)
      if graveyard[data.uid] then
        return true, graveyard[data.uid]
      end
      return false, "Not found in Graveyard, check uid or try again later"
    end,
    parallel_setAlarm = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then 
          threads[i].alarm = data.alarm
          return true
        end
      end
      return false, "No running thread found, try to dig" 
    end, 
    parallel_getAlarm = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then
          return threads[i].alarm, threads[i].alarm and "No alarm"
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_setFilter = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then 
          threads[i].filter = data.filter
          return true
        end
      end
      return false, "No running thread found, try to dig" 
    end, 
    parallel_getFilter = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then
          return threads[i].filter, threads[i].filter and "No filter"
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_setName = function (data)
      if data == nil then 
        threads[focus] = data.name
        return true
      end
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then 
          threads[i].name = data.name
          return true
        end
      end
      return false, "No running thread found, try to dig" 
    end,  
    parallel_getName = function (data)
      for i=1,lastThread do
        if threads[i] and threads[i].uid == data.uid then
          return threads[i].name
        end
      end
      return false, "No running thread found, try to dig" 
    end,
    parallel_getThread = function (data)
      if data.uid == nil then
        local result = {}
        for key,value in pairs(threads[focus]) do
          if key ~= "argSets" and key ~= "co" then result[key] = value end
        end
        return result
      else
        for _,thread in pairs(threads) do
          if thread.uid == data.uid then
            local result = {}
            for key,value in pairs(thread) do
              if key ~= "argSets" and key ~= "co" then result[key] = value end
            end
            return result
          end
        end      
        return false, "No running thread found, try to dig" 
      end
    end,
    parallel_getThreads = function ()
      local result = {}
      local count = 0
      for _,thread in pairs(threads) do
        count = count + 1
        result[count] = {}
        for key,value in pairs(thread) do
          if key ~= "argSets" and key ~= "co" then result[count][key] = value end
        end
      end
      return result
    end,
    parallel_exit = function ()
      lastThread = -1
      return nil
    end,
  }
  while lastThread > 0 do  -- Run until no threads left (won't happen normally)
    local alarm = 0
    --logger.spam("\nThread %s/%s (%s,%s,%s)", focus, lastThread ,event[1], event[3], event[4])
    if type(threads[focus]) == "table" then 
      --logger.spam("%s,%s!", threads[focus].pause, threads[focus].lastArgSet)
      
      local i = 1
      while 
        threads[focus]
        and threads[focus].pause == 0
        and ((not threads[focus].alarm) or threads[focus].alarm < utils.getTime())
        and i <= threads[focus].lastArgSet
      do
        --logger.spam("\n  i=%s..",i)
        
        local result = table.pack( coroutine.resume( threads[focus].co, table.unpack( threads[focus].argSets[i] or {} ) ) )
        --for k,v in ipairs(result) do logger.spam("\n    [%s]: %s",k,v) end
        local ok, args = result[1], result[2]
        threads[focus].argSets[i] = nil
        threads[focus].filter = false
        threads[focus].alarm = false
        
        if not ok then 
          logger.warning("thread failed! %s\n", args )
          graveyard[ threads[focus].uid ] = result
          graveyardTime[ threads[focus].uid ] = utils.getTime()
          threads[focus] = nil
        elseif coroutine.status(threads[focus].co) == "dead" then 
          graveyard[ threads[focus].uid ] = result
          graveyardTime[ threads[focus].uid ] = utils.getTime()
          threads[focus] = nil
        else
          if args and type(args) == "string" and args ~= "" then 
            threads[focus].filter = args
          elseif args and type(args) == "number" and args ~= "" then 
            threads[focus].alarm = args + utils.getTime()
          elseif args and type(args) == "table" then
            if args.flag and operations[ args.flag ] then 
              threads[focus].argSets[i] = { operations[ args.flag ]( args ) }
              if threads[focus].argSets[i][1] == nil then threads[focus].argSets[i] = nil end
            end
            local computer = require("computer")
          end
        end
        
        if threads[focus] and threads[focus].argSets[i] == nil then i = i + 1 end
      end
      if threads[focus] then threads[focus].lastArgSet = 0 end
    else
      if focus == lastThread then lastThread = lastThread - 1 end
    end
    if type(threads[focus+1]) ~= "table" then 
      threads[focus+1] = threads[focus+2] 
      threads[focus+2] = nil
    end

    if threads[focus] 
      and not threads[focus].filter
      and (threads[focus].alarm or 0) < alarm 
    then alarm = threads[focus].alarm or 0 end
        
    --logger.spam("\n")
    if focus < lastThread then 
      focus = focus + 1
    else
      focus = 1
      event = table.pack(computer.pullSignal(math.max(0,alarm - utils.getTime())))
      for i=1,lastThread do
        if 
          threads[i]
          and ((not threads[i].filter) or string.match((type(event[1])=="string" and event[1]) or "",threads[i].filter))
          and ((not threads[i].alarm) or threads[i].alarm < utils.getTime())
        then
          threads[i].argSets[ threads[i].lastArgSet+1 ] = event
          threads[i].lastArgSet = threads[i].lastArgSet + 1
        end
      end
    end
    
  end

  logger.info("Out of threads\n")
  return false, "Out of threads"
end
return parallel

