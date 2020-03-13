import subprocess
import re
import json
import sys

last_line = subprocess.check_output(["tail", "-1", "big-sync/output.txt"])
last_line_decoded = last_line.decode("utf-8")

calculating_match = re.search("calculating", last_line_decoded)
match = re.search("([0-9]*) file\(s\) remaining", last_line_decoded)

big_match = re.search("Completed ([0-9]*\.[0-9]*) (M|K|G)iB\/~?([0-9]*\.[0-9]*) (M|K|G)iB \([0-9]*\.[0-9]* [MKG]iB\/s\) with ~?([0-9]*) file\(s\) remaining( \(calculating...\))?", last_line_decoded)

def getMultiplierForDataUnit(unit: str):
    if unit == "K":
        return 1024
    if unit == "M":
        return 1024 * 1024
    if unit == "G":
        return 1024 * 1024 * 1024

    raise ValueError('Must be K, M or G')

if big_match is not None:
    progress_bytes_prefix = float(big_match.group(1))
    progress_bytes_multiplier = getMultiplierForDataUnit(big_match.group(2))
    progress = progress_bytes_prefix * progress_bytes_multiplier

    total_bytes_prefix = float(big_match.group(3))
    total_bytes_multiplier = getMultiplierForDataUnit(big_match.group(4))
    total_bytes = total_bytes_prefix * total_bytes_multiplier

    files_remaining = int(big_match.group(5))

    calculating = big_match.group(6) is not None

    status = {
        'progress': progress,
        'files_remaining': files_remaining,
        'total': total_bytes,
        'isCalculating': calculating
    }

    print(json.dumps(status))
    exit(0)

else:
    print("could not find file progress", file=sys.stderr)
    exit(1)
