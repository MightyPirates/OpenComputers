local uuid = {}

function uuid.next()
  -- e.g. 3c44c8a9-0613-46a2-ad33-97b6ba2e9d9a
  -- 8-4-4-4-12 (halved sizes because bytes make hex pairs)
  local sets = {4, 2, 2, 2, 6}
  local result = ""

  for _,set in ipairs(sets) do
    if result:len() > 0 then
      result = result .. "-"
    end
    for i = 1,set do
      result = result .. string.format("%02x", math.random(0, 255))
    end
  end

  return result
end

return uuid
