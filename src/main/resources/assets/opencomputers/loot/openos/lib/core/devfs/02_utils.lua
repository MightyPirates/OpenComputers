return
{
  eeprom =
  {
    link = "components/by-type/eeprom/0/contents",
    isAvailable = function()
      local comp = require("component")
      return comp.list("eeprom")()
    end
  },
  ["eeprom-data"] =
  {
    link = "components/by-type/eeprom/0/data",
    isAvailable = function()
      local comp = require("component")
      return comp.list("eeprom")()
    end
  },
  null =
  {
    open = function()
      return
      {
        read = function() end,
        write = function() end
      }
    end
  },
  random =
  {
    open = function(mode)
      if mode and not mode:match("r") then
        return nil, "read only"
      end
      return
      {
        read = function(_, n)
          local chars = {}
          for _=1,n do
            table.insert(chars,string.char(math.random(0,255)))
          end
          return table.concat(chars)
        end
      }
    end
  },
  zero =
  {
    open = function()
      return
      {
        read = function(_, n) return ("\0"):rep(n) end,
        write = function() end
      }
    end
  },
}
