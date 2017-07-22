function start()
    kernel.modules.devfs.data.eeprom = {
        __type = "f",
        open = function(hnd)
            if not component.list("eeprom")() then
                error("No eeprom installed")
            end
            hnd.pos = 1
        end,
        size = function()
            return component.invoke(component.list("eeprom")() or "", "getSize")
        end,
        write = function(h, data)
            if h.pos > 1 then
                data = component.invoke(component.list("eeprom")() or "", "get"):sub(1,h.pos) .. data
            end
            h.pos = h.pos + #data
            component.invoke(component.list("eeprom")() or "", "set", data)
            return true
            --todo: handle overflow 
        end,
        read = function(h, len)
            local res = component.invoke(component.list("eeprom")() or "", "get")
            res = res:sub(h.pos, len)
            h.pos = h.pos + len
            return res ~= "" and res 
        end
    }
    kernel.modules.devfs.data["eeprom-data"] = {
        __type = "f",
        open = function(hnd)
            if not component.list("eeprom")() then
                error("No eeprom installed")
            end
            hnd.pos = 1
        end,
        size = function()
            return 256 --TODO: is this correct?
        end,
        write = function(h, data)
            if h.pos > 1 then
                data = component.invoke(component.list("eeprom")() or "", "getData"):sub(1,h.pos) .. data
            end
            h.pos = h.pos + #data
            component.invoke(component.list("eeprom")() or "", "setData", data)
            return true
        end,
        read = function(h, len)
            local res = component.invoke(component.list("eeprom")() or "", "getData")
            res = res:sub(h.pos, len)
            h.pos = h.pos + len
            return res ~= "" and res 
        end
    }
end
