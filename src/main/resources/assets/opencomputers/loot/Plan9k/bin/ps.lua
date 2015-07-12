local pipes = require("pipes")

print("PID    NAME")
for _, thread in pairs(pipes.getThreadInfo()) do
    print(thread.pid, "  ", thread.name)
end
