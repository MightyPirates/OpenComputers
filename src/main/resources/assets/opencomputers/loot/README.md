Lootable Disks
==============

OpenComputers registers a lootable read-only floppy disk for a couple of chests. This folder contains the folders that are the contents of the possible instances of that disk. In other words, there is a certain chance that a disk is added to a dungeon chest, and if it is, one of these folders will be picked at random as the content for the disk.

To add a disk, create a folder and put the files the disk should contain into that folder. Then add an entry to the `loot.properties` file, where each line represents one possibility for a disks contents. The key (part left of the `=`) on each line is the name of the *folder* with the disk's contents, the value (part right of the `=`) is the *label* of the created floppy disk.

You are invited to submit your own programs as pull requests! The more the merrier :-)

For example, say you have a program named "chat.lua". You'd create a folder, say `netchat` or whatever the program likes to call itself, and put the `chat.lua` file into that folder. You then add the line `netchat=NetChat` to the `loot.properties` file. And that's it. Make a pull request and your program is in OpenComputers - unless it fails the arbitrary quality check, of course. Feel free to submit pull requests for fixes to your submitted programs (or of others) at any time!
