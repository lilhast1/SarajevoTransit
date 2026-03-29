#!/usr/bin/env python3
import argparse
import json
import os
import time
from typing import Any, Dict, List, Optional

import requests


BASE_URL = "https://javniprevozks.ba/public-api/public/lines"


def get_json(url: str, timeout: int = 30, retries: int = 3, pause: float = 0.3) -> Any:
    last_err = None
    for attempt in range(1, retries + 1):
        try:
            response = requests.get(url, timeout=timeout)
            response.raise_for_status()
            return response.json()
        except Exception as ex:
            last_err = ex
            if attempt < retries:
                time.sleep(pause * attempt)
    raise RuntimeError(f"Failed GET {url}: {last_err}")


def to_float_or_none(value: Any) -> Optional[float]:
    if value is None:
        return None
    try:
        return float(value)
    except Exception:
        return None


def build_snapshot(timeout: int, pause: float, retries: int) -> Dict[str, Any]:
    snapshot: Dict[str, Any] = {"lines": []}
    line_map: Dict[int, Dict[str, Any]] = {}

    for vehicle_type_id in [1, 2, 3, 4]:
        lines_url = f"{BASE_URL}/vehicle-type/{vehicle_type_id}/list"
        lines_data = get_json(lines_url, timeout=timeout, retries=retries, pause=pause) or []
        time.sleep(pause)

        for raw_line in lines_data:
            line_id = raw_line.get("id")
            if line_id is None:
                continue

            line_obj = line_map.get(line_id)
            if line_obj is None:
                line_obj = {
                    "id": line_id,
                    "vehicleTypeId": vehicle_type_id,
                    "code": raw_line.get("code"),
                    "name": raw_line.get("name"),
                    "directions": [],
                    "timetableGroups": [],
                }
                line_map[line_id] = line_obj
                snapshot["lines"].append(line_obj)

            directions_url = f"{BASE_URL}/line/{line_id}/directions/list"
            directions_data = get_json(directions_url, timeout=timeout, retries=retries, pause=pause) or []
            time.sleep(pause)

            directions_out: List[Dict[str, Any]] = []
            for direction in directions_data:
                terminus_line_id = direction.get("terminus_line_id")
                if terminus_line_id is None:
                    continue

                stations_url = f"{BASE_URL}/terminus-line/{terminus_line_id}/stations-with-distance/list/0/0"
                stations_data = get_json(stations_url, timeout=timeout, retries=retries, pause=pause) or []
                time.sleep(pause)

                stations_out = []
                for station in stations_data:
                    if station.get("id") is None:
                        continue

                    lat = to_float_or_none(station.get("latitude"))
                    lon = to_float_or_none(station.get("longitude"))
                    if lat is None or lon is None:
                        continue

                    stations_out.append(
                        {
                            "id": station.get("id"),
                            "code": station.get("code"),
                            "name": station.get("name"),
                            "address": station.get("address"),
                            "latitude": lat,
                            "longitude": lon,
                        }
                    )

                points_url = f"{BASE_URL}/terminus-line/{terminus_line_id}/station-line/points/list"
                points_data = get_json(points_url, timeout=timeout, retries=retries, pause=pause) or []
                time.sleep(pause)

                route_points_out = []
                for point in points_data:
                    lat = to_float_or_none(point.get("latitude"))
                    lon = to_float_or_none(point.get("longitude"))
                    if lat is None or lon is None:
                        continue

                    route_points_out.append(
                        {
                            "id": point.get("id"),
                            "latitude": lat,
                            "longitude": lon,
                        }
                    )

                directions_out.append(
                    {
                        "terminus_line_id": terminus_line_id,
                        "code": direction.get("code"),
                        "name": direction.get("name"),
                        "direction": direction.get("direction"),
                        "line_id": direction.get("line_id"),
                        "length_meters": float(direction.get("length_meters") or 0.0),
                        "can_delete": bool(direction.get("can_delete")),
                        "stations": stations_out,
                        "routePoints": route_points_out,
                    }
                )

            line_obj["directions"] = directions_out

            timetables_url = f"{BASE_URL}/timetables/{line_id}/list"
            timetable_groups = get_json(timetables_url, timeout=timeout, retries=retries, pause=pause) or []
            time.sleep(pause)

            groups_out = []
            for group in timetable_groups:
                entries = group.get("timetable") or []
                entries_out = []

                for entry in entries:
                    if entry.get("id") is None:
                        continue

                    entries_out.append(
                        {
                            "id": entry.get("id"),
                            "name": entry.get("name"),
                            "terminus_line_id": entry.get("terminus_line_id"),
                            "start_time": entry.get("start_time"),
                            "valid_from": entry.get("valid_from"),
                            "valid_to": entry.get("valid_to"),
                            "rides_on_holidays": bool(entry.get("rides_on_holidays")),
                            "days_of_week": entry.get("days_of_week") or [],
                            "line_id": entry.get("line_id"),
                            "receives_passengers": bool(entry.get("receives_passengers")),
                        }
                    )

                groups_out.append(
                    {
                        "id": group.get("id"),
                        "code": group.get("code"),
                        "name": group.get("name"),
                        "timetable": entries_out,
                    }
                )

            line_obj["timetableGroups"] = groups_out

    return snapshot


def main() -> None:
    parser = argparse.ArgumentParser(description="Scrape Sarajevo transit snapshot JSON for routingservice import")
    parser.add_argument("--output", default="routingservice/data/scrape-snapshot.json", help="Output JSON path")
    parser.add_argument("--timeout", type=int, default=30, help="HTTP timeout in seconds")
    parser.add_argument("--pause", type=float, default=0.25, help="Pause between requests in seconds")
    parser.add_argument("--retries", type=int, default=3, help="Number of retries for failed requests")
    args = parser.parse_args()

    snapshot = build_snapshot(timeout=args.timeout, pause=args.pause, retries=args.retries)

    output_dir = os.path.dirname(args.output)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)

    with open(args.output, "w", encoding="utf-8") as output_file:
        json.dump(snapshot, output_file, ensure_ascii=False, indent=2)

    print(f"Saved snapshot to: {args.output}")
    print(f"Total lines: {len(snapshot.get('lines', []))}")


if __name__ == "__main__":
    main()
