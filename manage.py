#!/usr/bin/env python
import os
import sys

if __name__ == "__main__":
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "wps_blog.settings")

    from configurations.management import execute_from_command_line

    execute_from_command_line(sys.argv)