#!/usr/bin/env python3
import json, os, sys

def read_stdin():
    stdin_lines = []
    for line in sys.stdin: stdin_lines.append( line )
    return stdin_lines

def write_text(data, path):
    with open(path, "a") as f:
        f.write( data )

def write_json(data, path):
    with open(path, "a") as f:
        f.write( json.dumps(data, indent=4) )

base = os.path.abspath(os.path.dirname(__file__))
stdin_lines = read_stdin()
if len(stdin_lines) > 0:
    write_text(" --> {}\n".format(str(sys.argv)), "{}/output.log".format(base))
    write_text(" ------------------------------------------------------------------- \n", "{}/output.log".format(base))
    parsed_input   = json.loads( "".join( stdin_lines ) )
    write_json(parsed_input, "{}/output.log".format(base))
    write_text("\n ------------------------------------------------------------------- \n\n\n", "{}/output.log".format(base))