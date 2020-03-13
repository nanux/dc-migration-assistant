import subprocess
import re
import json
import sys

def getLastLineOfSyncOutput(syncFilePath: str) -> str:
    last_line = subprocess.check_output(["tail", "-1", sys.argv[1]])
    return last_line.decode("utf-8")

def checkIfSyncCompleted(last_line_of_output: str) -> (bool, int):
    match = re.search("s3 sync with shared home complete with exit code ([0-9]*)", last_line_of_output)
    if match is None:
        return False, 0

    return True, match.group(1)

def getMultiplierForDataUnit(unit: str):
    if unit == "K":
        return 1024
    if unit == "M":
        return 1024 * 1024
    if unit == "G":
        return 1024 * 1024 * 1024

    raise ValueError('Must be K, M or G')

def parseSyncOutput(last_line_of_output):
    match = re.search("Completed ([0-9]*\.[0-9]*) (M|K|G)iB\/~?([0-9]*\.[0-9]*) (M|K|G)iB \([0-9]*\.[0-9]* [MKG]iB\/s\) with ~?([0-9]*) file\(s\) remaining( \(calculating...\))?", last_line_of_output)

    if match is not None:
        progress_bytes_prefix = float(match.group(1))
        progress_bytes_multiplier = getMultiplierForDataUnit(match.group(2))
        progress = progress_bytes_prefix * progress_bytes_multiplier

        total_bytes_prefix = float(match.group(3))
        total_bytes_multiplier = getMultiplierForDataUnit(match.group(4))
        total_bytes = total_bytes_prefix * total_bytes_multiplier

        files_remaining = int(match.group(5))
        calculating = match.group(6) is not None

        status = {
            'progress': progress,
            'files_remaining': files_remaining,
            'total': total_bytes,
            'isCalculating': calculating
        }
        return status

    else:
        raise ValueError('could not find sync progress in sync output {}'.format(last_line_of_output))

def parseError(error_file_path: str) -> str:
    with open(error_file_path) as errFile:
        return errFile.readlines()

if len(sys.argv) != 3:
    print("Usage: {} <output file> <error file>".format(sys.argv[0]))
    exit(1)

output_file_path = sys.argv[1]
error_file_path = sys.argv[2]

last_line = getLastLineOfSyncOutput(output_file_path)

finished, exit_code = checkIfSyncCompleted(last_line)

if finished:
    progress = {
        'finished': True,
        'code': exit_code
    }
    print(json.dumps(progress))
    exit(0)

try:
    progress = parseSyncOutput(last_line)
except ValueError:
    print("could not find file progress in last line of progress file {}".format(output_file_path), file=sys.stderr)
    exit(1)

try:
    errors = parseError(error_file_path)
    result = {
        'status': progress,
        'hasErrors': True,
        'errors': errors
    }
except:
    result = {
        'status': progress,
        'hasErrors': False
    }

print(json.dumps(result))
exit(0)