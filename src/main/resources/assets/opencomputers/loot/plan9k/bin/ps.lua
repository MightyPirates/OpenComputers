local pipes = require("pipes")

print("PID    PARENT    NAME")
for _, thread in pairs(pipes.getThreadInfo()) do
    print(thread.pid, "  ", tostring(thread.parent), "    ", thread.name)
end
