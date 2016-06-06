local args = table.pack(...)
for i = 1, #args do
    if i > 1 then
        io.write(" ")
    end
    io.write(args[i])
end
io.write("\n")