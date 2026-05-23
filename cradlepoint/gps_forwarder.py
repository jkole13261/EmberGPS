"""
gps_forwarder.py — Cradlepoint R980 NCOS SDK Application
=========================================================
Reads live GPS data from the router's status tree and POSTs it to the
EmberGPS ingest endpoint at a configurable interval.

Installation
------------
1. Copy this file to the Cradlepoint router via NetCloud Manager or SCP.
2. In NetCloud Manager go to:
     Router > Configuration > System > SDK > Applications
3. Upload this script and enable it.
4. Set the following environment variables in the SDK app config
   (or hard-code them below for testing):

   SERVER_URL  — https://your-server.com/api/v1/gps/ingest
   API_KEY     — the device API key returned when you created the device
                  via POST /api/v1/admin/devices  (starts with "emb_")
   INTERVAL    — seconds between GPS reports (default: 30)

The router must have outbound HTTPS access to your server on port 443.
"""

import cs
import json
import os
import time

import requests  # bundled with Cradlepoint NCOS SDK

# ---------------------------------------------------------------------------
# Configuration — override via environment variables or edit here
# ---------------------------------------------------------------------------
SERVER_URL = os.environ.get("SERVER_URL", "https://your-server.com/api/v1/gps/ingest")
API_KEY    = os.environ.get("API_KEY",    "emb_replace_with_your_device_api_key")
INTERVAL   = int(os.environ.get("INTERVAL", "30"))

HEADERS = {
    "X-API-Key":    API_KEY,
    "Content-Type": "application/json",
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def get_utc_timestamp():
    """Return current UTC time in ISO-8601 format."""
    t = time.gmtime()
    return "{:04d}-{:02d}-{:02d}T{:02d}:{:02d}:{:02d}Z".format(
        t.tm_year, t.tm_mon, t.tm_mday,
        t.tm_hour, t.tm_min, t.tm_sec
    )


def build_payload():
    """Read GPS and system status from the NCOS status tree."""
    gps    = cs.get("status/gps")    or {}
    system = cs.get("status/system") or {}

    fix_type = gps.get("fix_type", 0)
    # Only report if we have a valid fix
    if not fix_type:
        cs.log("GPS: no fix yet, skipping report")
        return None

    return {
        "device_id":  system.get("serial") or system.get("mac", "unknown"),
        "timestamp":  get_utc_timestamp(),
        "latitude":   gps.get("latitude"),
        "longitude":  gps.get("longitude"),
        "altitude":   gps.get("altitude"),
        "speed":      gps.get("speed"),
        "heading":    gps.get("heading"),
        "fix_type":   fix_type,
        "hdop":       gps.get("hdop"),
        "satellites": gps.get("num_sat"),
    }


# ---------------------------------------------------------------------------
# Main loop
# ---------------------------------------------------------------------------

cs.log("EmberGPS forwarder starting. Server={}, interval={}s".format(SERVER_URL, INTERVAL))

while True:
    try:
        payload = build_payload()
        if payload:
            resp = requests.post(
                SERVER_URL,
                json=payload,
                headers=HEADERS,
                timeout=10,
                verify=True,   # enforce SSL certificate verification
            )
            if resp.status_code in (200, 201):
                cs.log("GPS posted OK: lat={latitude} lon={longitude}".format(**payload))
            elif resp.status_code == 429:
                cs.log("GPS forwarder: rate limited, will retry next interval")
            else:
                cs.log("GPS forwarder: unexpected status {} — {}".format(
                    resp.status_code, resp.text[:200]))
    except requests.exceptions.SSLError as e:
        cs.log("GPS forwarder: SSL error — check server certificate: {}".format(e))
    except requests.exceptions.ConnectionError as e:
        cs.log("GPS forwarder: connection error — {}".format(e))
    except Exception as e:
        cs.log("GPS forwarder: unexpected error — {}".format(e))

    time.sleep(INTERVAL)
