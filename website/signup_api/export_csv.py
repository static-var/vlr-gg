import csv
import os
import sqlite3
import sys
from pathlib import Path


db_path = Path(os.environ.get("SIGNUP_DB_PATH", "/data/signups.sqlite3"))
connection = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
try:
    rows = connection.execute(
        "SELECT email FROM signups WHERE invited_at IS NULL ORDER BY created_at, email"
    )
    writer = csv.writer(sys.stdout)
    writer.writerow(["email"])
    writer.writerows(rows)
finally:
    connection.close()
