local http = {}

function http.request(url, data)
  checkArg(1, url, "string")
  checkArg(2, data, "string", "table", "nil")

  local m = component.modem
  if not m or not m.isWireless() then
    error("no primary wireless modem found")
  end

  if not m.isHttpEnabled() then
    error("http support is not enabled")
  end

  local post
  if type(data) == "string" then
    post = data
  elseif type(data) == "table" then
    for k, v in pairs(data) do
      post = post and (post .. "&") or ""
      post = post .. tostring(k) .. "=" .. tostring(v)
    end
  end

  local result, reason = m.send(url, post)
  if not result then
    error(reason)
  end

  return function()
    while true do
      local _, responseUrl, result, reason = event.pull("http_response")
      if responseUrl == url then
        if not result and reason then
          error(reason)
        end
        return result
      end
    end
  end
end

_G.http = http