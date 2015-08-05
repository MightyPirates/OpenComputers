--[[
--   A program that allows user to perform all crypto operations provided by Tier II / Tier III data cards
--   Author: makkarpov
--]]

local shell = require("shell")
local data = require("data")
local term = require("term")
local filesystem = require("filesystem")
local serialization = require("serialization")

local args, options = shell.parse(...)

local function writeFile(path, data)
  if filesystem.exists(path) then
    io.stderr:write("gpg: failed to write file: " .. path .. "\n")
    io.stderr:write("gpg: error was: file already exists\n")
    return false
  end

  if type(data) == "table" then
    data = serialization.serialize(data)
  end

  local h, err = io.open(path, "wb")

  if not h then
    io.stderr:write("gpg: failed to write file: " .. path .. "\n")
    io.stderr:write("gpg: error was: " .. err .. "\n")
    return false
  end

  h:write(data)
  h:close()
  return true
end

local function readFile(path, deserialize)
  local h = io.open(path, "rb")
  local r = h:read("*a")
  h:close()

  if deserialize then
    r = serialization.unserialize(r)
  end

  return r
end

local function parseKey(path, isPublic)
  local d = readFile(path, true)
  local k, err = data.deserializeKey(d.d, d.t)

  if not k then
    io.stderr:write("gpg: failed to parse key: " .. err .. "\n")
    return nil
  end

  if k.isPublic() ~= isPublic then
    io.stderr:write("gpg: wrong key type\n")
    return nil
  end

  return k
end

local function deriveName(base, encrypt)
  if encrypt then
    return base .. ".gpg"
  else
    local d = base:gsub(".gpg", "")
    if d == base then
      d = d .. ".dec"
      io.write("gpg: decrypting to " .. d .. "\n")
    end
    return d
  end
end

local function ensureMethods(...)
  if not require("component").isAvailable("data") then
    io.stderr:write("gpg: you must have data card in order to run this program\n")
    error("data card is absent")
  end

  local names = table.pack(...)
  for i = 1, names.n do
    if names[i] and not data[names[i]] then
      io.stderr:write("gpg: method " .. names[i] .. " required on data card to run this program\n")
      error("data card tier insufficient")
    end
  end
end

if options['g'] and (#args == 2) then
  ensureMethods("generateKeyPair")
  local pub, priv = data.generateKeyPair(384)

  priv = { t = priv.keyType(), d = priv.serialize() }
  pub = { t = pub.keyType(), d = pub.serialize() }

  if not writeFile(args[1], priv) then
    io.stderr:write("gpg: failed to write private key, aborting\n")
    return false
  end

  if not writeFile(args[2], pub) then
    io.stderr:write("gpg: failed to write public key, aborting\n")
    return false
  end

  return true
end

if options['c'] and (options['e'] or options['d']) and (#args == 1) then
  ensureMethods("md5", "sha256", "encrypt", "decrypt", "random")
  if options['d'] and options['e'] then
    io.stderr:write("gpg: please specify either -d or -e\n")
    return false
  end

  io.write("gpg: enter password: ")
  local aesKey = data.md5(term.read(nil, nil, nil, "*"))
  local checkValue = data.sha256(aesKey)

  if options['e'] then
    local iv = data.random(16)
    local d = data.encrypt(readFile(args[1]), aesKey, iv)

    return writeFile(deriveName(args[1], true), {
      t = "pwd",
      kdf = "md5",
      iv = iv,
      cv = checkValue,
      d = d
    })
  else
    local d = readFile(args[1], true)

    if d.t ~= "pwd" then
      io.stderr:write("gpg: file is not encrypted with a password\n")
      return false
    end

    if checkValue ~= d.cv then
      io.stderr:write("gpg: password incorrect\n")
      return false
    end

    return writeFile(deriveName(args[1], false), data.decrypt(d.d, aesKey, d.iv))
  end
end

if (options['d'] or options['e']) and (#args == 2) then
  ensureMethods("md5", "sha256", "encrypt", "decrypt", "random", "generateKeyPair", "deserializeKey", "ecdh")
  if options['d'] and options['e'] then
    io.stderr:write("gpg: please specify either -d or -e\n")
    return false
  end

  if options['e'] then
    local userPub = parseKey(args[1], true)
    local tmpPub, tmpPriv = data.generateKeyPair(384)
    local aesKey = data.md5(data.ecdh(tmpPriv, userPub))
    local checkValue = data.sha256(aesKey)
    local iv = data.random(16)

    local d = data.encrypt(readFile(args[2]), aesKey, iv)
    return writeFile(deriveName(args[2], true), {
      t = "ecdh",
      kdf = "md5",
      iv = iv,
      cv = checkValue,
      k = {
        t = tmpPub.keyType(),
        d = tmpPub.serialize()
      },
      d = d
    })
  else
    local userPriv = parseKey(args[1], false)
    local d = readFile(args[2], true)

    if d.t ~= "ecdh" then
      io.stderr:write("gpg: file is not encrypted with a key\n")
      return false
    end

    local tmpPub = data.deserializeKey(d.k.d, d.k.t)
    local aesKey = data.md5(data.ecdh(userPriv, tmpPub))

    if d.cv ~= data.sha256(aesKey) then
      io.stderr:write("gpg: invalid key\n")
      return false
    end

    return writeFile(deriveName(args[2], false), data.decrypt(d.d, aesKey, d.iv))
  end
end

if (options['s'] or options['v']) and (#args == 2) then
  ensureMethods("deserializeKey", "ecdsa")
  if options['s'] and options['v'] then
    io.stderr:write("gpg: please specify either -s or -v\n")
    return false
  end

  if options['s'] then
    local userPriv = parseKey(args[1], false)
    local sign = data.ecdsa(readFile(args[2]), userPriv)

    return writeFile(args[2] .. ".sig", {
      t = "ecdsa",
      s = sign
    })
  else
    local userPub = parseKey(args[1], true)
    local sign = readFile(args[2] .. ".sig", true)

    if sign.t ~= "ecdsa" then
      io.stderr:write("gpg: unsupported signature type\n")
      return false
    end

    if not data.ecdsa(readFile(args[2]), userPub, sign.s) then
      io.stderr:write("gpg: signature verification failed\n")
      return false
    end

    io.write("gpg: signature is valid\n")

    return true
  end
end

io.write("Usages:\n")
io.write("gpg -ce <file> -- encrypt file with password\n")
io.write("gpg -cd <file> -- decrypt file with password\n")
io.write("gpg -e <key> <file> -- encrypt file\n")
io.write("gpg -d <key> <file> -- decrypt file\n")
io.write("gpg -g <private key file> <public key file> -- generate keypair\n")
io.write("gpg -s <key> <file> -- sign file\n")
io.write("gpg -v <key> <file> -- verify file\n")
return false