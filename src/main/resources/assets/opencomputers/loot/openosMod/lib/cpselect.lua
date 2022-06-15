local component = require("component")
local term = require("term")

--------------------------------------------------

local lib = {}

function lib.get(filter, message, important)
    checkArg(1, filter, "string")
    checkArg(2, message, "string", "nil")
    checkArg(3, important, "boolean", "nil")
    local count = 0
    local addresses = {}
    for address, ltype in component.list(filter) do
        if ltype == filter then
            count = count + 1
            print(count..": "..address..".")
            addresses[count] = address
        end
    end
    if count == 0 then
        print("не найден компонент " .. filter .. ((message and (", для " .. message)) or ""))
        if important then
            os.exit()
            return nil
        else
            os.sleep(0.2)
            return nil
        end
    end

    print("выберите компонент" .. ((important and " (обязательно)") or "") .. " с типом " .. filter .. ((message and (", для " .. message)) or "") .. ":")
    
    local read
    while true do
        term.write("number: ")
        read = io.read()
        if read == false then break end

        read = tonumber(read)
        if not read then
            print("ошибка ввода")
        else
            if read >= 1 and read <= count then
                break
            else
                print("ошибка ввода")
            end
        end
    end
    if not read then
        if important then
            print("выбор компонента отменен, программа была завершена")
            os.exit()
            return nil
        else
            print("выбор компонента отменен")
            os.sleep(0.2)
            return nil
        end
    end

    return addresses[read]
end

return lib