local guid = {}

function guid.toHex(n)
  if type(n) ~= 'number' then
    return nil, string.format("toHex only converts numbers to strings, %s is not a string, but a %s", tostring(n), type(n))
  end
  if n == 0 then
    return '0'
  end

  local hexchars = "0123456789abcdef"
  local result = ""
  local prefix = "" -- maybe later allow for arg to request 0x prefix
  if n < 0 then
    prefix = "-"
    n = -n
  end

  while n > 0 do
    local next = math.floor(n % 16) + 1 -- lua has 1 based array indices
    n = math.floor(n / 16)
    result = hexchars:sub(next, next) .. result
  end

  return prefix .. result
end

function guid.next()
  -- e.g. 3c44c8a9-0613-46a2-ad33-97b6ba2e9d9a
  -- 8-4-4-4-12
  local sets = {8, 4, 4, 12}
  local result = ""

  local i
  for _,set in ipairs(sets) do
    if result:len() > 0 then
      result = result .. "-"
    end
    for i = 1,set do
      result = result .. guid.toHex(math.random(0, 15))
    end
  end

  return result
end

return guid
