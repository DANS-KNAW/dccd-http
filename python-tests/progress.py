# -*- coding: utf-8 -*-
import sys
from time import sleep

# show progress indicator (never tested on Windows)
def show_progress(counter, max_counter, size=40):
    spinner = "|/-\\"
    if not hasattr(show_progress, "call_cnt"):
        show_progress.call_cnt = 0  # it doesn't exist yet, so initialize it
    show_progress.call_cnt += 1
    show_progress.call_cnt = show_progress.call_cnt % len(spinner)  # prevent overflow

    max_percentage = 100
    percentage = counter * max_percentage / max_counter
    # always rotate even if counter did not change
    rot_counter = show_progress.call_cnt % len(spinner)

    # spinner will show change if counter changes, even if percentage is not!
    spinner_str = '(' + spinner[rot_counter % len(spinner)] + ')'
    # show bar
    bar_str = '[' + '#' * (percentage * size / max_percentage) + '.' * (
    size - (percentage * size / max_percentage)) + ']'
    # show percentage
    percentage_str = " %3d %%" % percentage
    line = spinner_str + bar_str + percentage_str
    sys.stdout.write(line)
    sys.stdout.flush()
    sys.stdout.write("\r")  # place insertion at start of line
    if counter == max_counter:
        sleep(0.3)  # 100% will show for a short period
        # clear line
        sys.stdout.write(' ' * (len(line)))
        sys.stdout.write("\r")
        sys.stdout.flush()