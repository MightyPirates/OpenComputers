# Adapter

![Freeeedooooooom!](block:OpenComputers:adapter)

The adapter's serial interface does not implement a hard-coded protocol. Instead, the protocol is defined by the software running on the computer controlling the adapter. Please refer to the component's API using an OpenComputers computer for specifics.

The adapter's serial interface has a small internal buffer for values passed along in either direction. Note that by default the adapter's serial interface does not read from the serial port. Reading has to be enabled from the component's API.
