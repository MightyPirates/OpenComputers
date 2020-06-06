NAME
  ls - list directory contents

SYNOPSIS
  ls [OPTION]... [FILE]...

DESCRIPTION
  List information about the specified files, or the current working directory by default.

OPTIONS
  -a, --all
    do not ignore entries starting with .

  --full-time
    with -l, print time in full iso format

  -h, --human-readable
    with -l and/or -s, print human readable sizes

  --si
    likewise, but use powers of 1000 not 1024

  -l
    use a long listing format

  -r, --reverse
    reverse order while sorting

  -R, --recursive
    list subdirectories recursively

  -S
    sort by file size

  -t
    sort by modification time, newest first

  -X
    sort alphabetically by entry extension

  -1
    list one file per line

  --no-color
    Do not colorize the output (default colorized)

  --help
    display this help and exit

  -p
    append / indicator to directories

  -M
    display Microsoft-style file and directory count after listing

ENVIRONMENT VARIABLES

  LS_COLORS
    A serialized table listing colors to use for listing filesystem elements.

    FILE
      Coloring to use when listing a regular file

    DIR
      Coloring to use when listing a directory

    LINK
      Coloring to use when listing a symbolic link

    *.<extension>
      Coloring to use for regular files with an extension <extension>

    The default LS_COLORS string is "{FILE=0xFFFFFF,DIR=0x66CCFF,LINK=0xFFAA00,["*.lua"]=0x00FF00}"

EXAMPLES
  ls
    Displays the contents of the current directory.

  ls /bin /mnt
    Displays the contents of the `/bin`/ and `/mnt` directories, one after the other.
