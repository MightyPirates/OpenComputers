if [[ -d "run/crash-reports" ]]; then
  echo "Crash reports detected:"
  cat $directory/*
  exit 1
fi

if grep --quiet "Fatal errors were detected" server.log; then
  echo "Fatal errors detected:"
  cat server.log
  exit 1
fi

if grep --quiet "The state engine was in incorrect state ERRORED and forced into state SERVER_STOPPED" server.log; then
  echo "Server force stopped:"
  cat server.log
  exit 1
fi

if grep --quiet 'Done .+ For help, type "help" or "?"' server.log; then
  echo "Server didn't finish startup:"
  cat server.log
  exit 1
fi

echo "No crash reports detected"
exit 0

