tapes = {}

local function buildDevfs()
    for file in pairs(kernel.modules.devfs.data) do
        if file:match("^tape") then
            kernel.modules.devfs.data[file] = nil
        end
    end
    for k, tape in ipairs(tapes) do
        kernel.modules.devfs.data["tape" .. tape:sub(1,4):upper()] = {
            __type = "f",
            open = function(hnd)
                if not component.invoke(tape, "isReady") then
                    error("Tape drive is not ready")
                end
                component.invoke(tape, "seek", -math.huge)
                hnd.tape = tape
                hnd.pos = 0
            end,
            size = function()
                return component.invoke(tape, "getSize")
            end,
            write = function(h, data)
                component.invoke(tape, "write", data)
                h.pos = h.pos + #data
                return not (h.pos >= component.invoke(tape, "getSize"))
                --TODO: do this correctly
            end,
            read = function(h, len)
                if h.pos >= component.invoke(tape, "getSize") then
                    return
                end
                h.pos = h.pos + len
                return component.invoke(tape, "read", len)
            end,
            seek = function(h, whence, offset)
                if whence == "end" then
                    h.pos = h.pos + component.invoke(tape, "seek", component.invoke(tape, "getSize") - h.pos - (offset or 0))
                elseif whence == "set" then
                    h.pos = h.pos + component.invoke(tape, "seek", (offset or 0) - h.pos)
                else
                    h.pos = h.pos + component.invoke(tape, "seek", offset or 0)
                end
                return math.floor(h.pos)
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

--function start()
--    for tape, t in component.list("tape_drive") do
--        onComponentAdded(_, tape, t)
--    end
--end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
