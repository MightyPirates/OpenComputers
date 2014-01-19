local http = {}

function http.request(url, data)
  checkArg(1, url, "string")
  checkArg(2, data, "string", "table", "nil")

  local m = component.internet
  if not m then
    error("no primary internet card found")
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

  local result, reason = m.request(url, post)
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