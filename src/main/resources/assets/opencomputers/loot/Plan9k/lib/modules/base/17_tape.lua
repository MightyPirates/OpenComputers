tapes = {}

local function buildDevfs()
    for file in pairs(kernel.modules.devfs.data) do
        if file:match("^tape") then
            kernel.modules.devfs.data[file] = nil
        end
    end
    for k, tape in ipairs(tapes) do
        kernel.modules.devfs.data["tape" .. k] = {
            __type = "f",
            open = function(hnd)
                if not component.invoke(tape, "isReady") then
                    error("Tape drive is not ready")
                end
                component.invoke(tape, "seek", -math.huge)
                hnd.tape = tape
            end,
            size = function()
                return component.invoke(tape, "getSize")
            end,
            write = function(h, data)
                component.invoke(tape, "write", data)
                return not component.invoke(tape, "isEnd", data)
                --TODO: do this correctly
            end,
            read = function(h, len)
                if component.invoke(tape, "isEnd", data) then
                    return
                end
                return component.invoke(tape, "read", len)
            end
        }
    end
end

local function onComponentAdded(_, address, componentType)
    if componentType == "tape_drive" then
        tapes[#tapes + 1] = address
        buildDevfs()
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "tape_drive" then
        local t
        for i, tape in ipairs(tapes) do
            if tape == address then
                t = i
                break
            end
        end
        table.remove(tapes, t)
        buildDevfs()
    end
end

function start()
    for tape, t in component.list("tape_drive") do
        onComponentAdded(_, tape, t)
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
