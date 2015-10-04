local shell = require("shell")
local fs = require("filesystem")

local shell_name = '-' .. fs.name(os.getenv("SHELL"))
local cmd_name = "cd"
local error_prefix = shell_name .. ": " .. cmd_name .. ": "

local args, ops = shell.parse(...)
local path = nil
local verbose = false

-- order of if blocks following POSIX specification on cd
-- this is not a complete support of the POSIX spec - I am
-- not supporting -L and -P options for differentiating between
-- logically and physically following symlink paths
-- but, we do support CDPATH
if #args == 0 then
  local home = os.getenv("HOME")
  if not home then
    io.stderr:write(error_prefix .. "HOME not set\n")
    return 1
  end
  path = home
elseif args[1]:len() > 0 and args[1]:sub(1,1) == '/' then
  path = args[1]
elseif args[1]:len() > 0 and args[1]:sub(1,1) == "." then
  path = args[1]
elseif args[1] == '-' then
  verbose = true
  local oldpwd = os.getenv("OLDPWD");
  if not oldpwd then
    io.stderr:write(error_prefix .. "OLDPWD not set\n")
    return 1
  end
  path = oldpwd
else
  local CDPATH = os.getenv("CDPATH")
  if CDPATH then
    for cd_path in CDPATH:gmatch("[^:]*:?") do
      -- [^:]+ would be simpler but CDPATH can define
      -- empty paths if the user wants to check . before
      -- the end of the cdpath list

      if cd_path and cd_path:len() > 0 then
        -- remove trailing : now if it was matched
        if cd_path:sub(-1) == ':' then
          cd_path = cd_path:sub(1, -2)
        end

        -- if cd_path is empty (POSIX says null) then check ./
        -- there is a case where we don't hit this if block for
        -- an empty cd_path, i.e. CDPATH="" which SHOULD be considered
        -- null for this purpose - but, that scenario would also act the
        -- same as no CDPATH and test ./ independently - so we get the
        -- same results. I've tested this on gentoo linux and confirmed
        -- curiously, empty checks are not verbose
        local empty_check = false
        if cd_path == "" then
          cd_path = "."
          empty_check = true
        end

        -- concat / on cd_path if it doesn't have one
        if cd_path:sub(-1) ~= '/' then
          cd_path = cd_path .. '/'
        end

        local test_path = cd_path .. args[1]

        -- resolve here because fs.exists doesn't
        -- understand . and ..
        test_path = shell.resolve(test_path)
        if fs.exists(test_path) then
          path = test_path
          verbose = not empty_check
          break
        end
      end
    end
  end
  if not path then
    path = args[1]
  end
end

local resolved = shell.resolve(path)
if not fs.exists(resolved) then
  io.stderr:write(error_prefix .. path .. ": No such file or directory\n")
  return 1
end

path = resolved

local oldpwd = shell.getWorkingDirectory()
local result, reason = shell.setWorkingDirectory(path);
if not result then
  io.stderr:write(error_prefix .. reason)
  return 1
else
  os.setenv("OLDPWD", oldpwd)
end

if verbose then
  os.execute("pwd")
end
