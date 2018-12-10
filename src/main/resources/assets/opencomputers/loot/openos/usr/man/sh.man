NAME
  sh - command interpreter (shell)

SYNOPSIS
  sh

DESCRIPTION
  This is the basic, built-in standard shell of OpenOS. It provides basic functionality and does the job for getting started. To run a command, enter it and press enter. The first token in a command will usually be a program. Any additional parameters will be passed along to the program.

  Arguments to programs can be quoted, to provide strings with multiple spaces in them, for example:
    echo "a   b"
  will print the string `a   b` to the screen. It is also possible to use single quotes (echo 'a b').

  Single quotes also suppress variable expansion. Per default, expressions like `$NAME` and `${NAME}` are expanded using environment variables (also accessible via the `os.getenv` method).

  Globbing is supported, i.e. '*' and '?' are expanded approriately. For example:
    ls b?n/
  will list all files in `/bin/` (and, if it exists `/ban` and so on).
    cp /bin/* /usr/bin/
  will copy all files from `/bin` to `/usr/bin`.

  The shell provides redirects and piping:
    cat f > f2
  copies the contents of file `f` to `f2`, for example.
    echo 'this  is  a  "test"' >> f2
  will append the string 'this is a "test"' to the file `f2`.
    2>/dev/null ./some_program_with_errors
  will redirect all stderr to /dev/null [i.e. supress errors].
  This example also demonstrates redirects can go at the front

  Redirects can be combined:
    cat < f >> f2
  will feed the contents of file `f` to cat, which will then output it (in append mode) to file `f2`.

  Finally, pipes can be used to pass data between programs:
    ls | cat > f
  will enumerate the files and directories in the working directory, write them to its output stream, which is cat's input stream, which will in turn write the data to file `f`.

  The shell also supports aliases, which can be created using `alias` and removed using `unalias` (or using the `shell` API). For example, `dir` is a standard alias for `ls`.

EXAMPLES
  sh
    Starts a new shell.