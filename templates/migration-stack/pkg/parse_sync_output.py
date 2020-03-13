import subprocess
import re

last_line = subprocess.check_output(["tail", "-1", "big-sync/output.txt"])
last_line_decoded = last_line.decode("utf-8")

calculating_match = re.search("calculating", last_line_decoded)
match = re.search("([0-9]*) file\(s\) remaining", last_line_decoded)

big_match = re.search("Completed ([0-9]*\.[0-9]*) (M|K|G)iB\/~?([0-9]*\.[0-9]*) (M|K|G)iB \([0-9]*\.[0-9]* [MKG]iB\/s\) with ~?([0-9]*) file\(s\) remaining( \(calculating...\))?", last_line_decoded)

if big_match is not None:
    print("Completed: {} {}iB".format(big_match.group(1), big_match.group(2)))
    print("Out of: {} {}iB".format(big_match.group(3), big_match.group(4)))
    print("{} files remaining".format(big_match.group(5)))
    if big_match.group(6) is not None:
        print("Still calculating total")
else:
    print("could not find file progress")
