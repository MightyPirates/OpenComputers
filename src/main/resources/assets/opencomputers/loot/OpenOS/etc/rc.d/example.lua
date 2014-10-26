local count = 0

function start(msg)
  print("This script displays a welcome message and counts the number " ..
  "of times it has been called. The welcome message can be set in the " ..
  "config file /etc/rc.cfg")
  print(args)
  if msg then
    print(msg)
  end
  print(count)
  print("runlevel: " .. require("computer").runlevel())
  count = count + 1
end
