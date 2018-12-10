local os = require("os")

for k,v in pairs(os.getenv()) do
    print(k.."="..v)
end
