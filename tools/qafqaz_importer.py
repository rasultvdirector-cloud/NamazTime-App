#!/usr/bin/env python3
import argparse
import csv
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSET_PATH = ROOT / "android_app" / "app" / "src" / "main" / "assets" / "azerbaijan_prayer_times.json"


def normalize_city_key(raw: str) -> str:
    normalized = (
        raw.strip()
        .lower()
        .replace("ə", "e")
        .replace("ı", "i")
        .replace("ö", "o")
        .replace("ü", "u")
        .replace("ğ", "g")
        .replace("ç", "c")
        .replace("ş", "s")
    )
    normalized = re.sub(r"[^a-z0-9]+", "", normalized)
    aliases = {
        "baki": "Baku",
        "baku": "Baku",
        "gence": "Ganja",
        "ganja": "Ganja",
        "sumqayit": "Sumgait",
        "sumgait": "Sumgait",
        "naxcivan": "Nakhchivan",
        "nakhchivan": "Nakhchivan",
        "seki": "Sheki",
        "sheki": "Sheki",
        "lenkeran": "Lankaran",
        "lankaran": "Lankaran",
    }
    return aliases.get(normalized, raw.strip())


def load_asset() -> dict:
    with ASSET_PATH.open("r", encoding="utf-8") as fh:
        return json.load(fh)


def save_asset(data: dict) -> None:
    with ASSET_PATH.open("w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def parse_rows(input_path: Path) -> list[dict]:
    with input_path.open("r", encoding="utf-8") as fh:
        sample = fh.read(4096)
        fh.seek(0)
        dialect = csv.Sniffer().sniff(sample, delimiters=",;\t")
        reader = csv.DictReader(fh, dialect=dialect)
        required = {"day", "imsak", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"}
        missing = required - set(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"Missing columns: {', '.join(sorted(missing))}")
        rows: list[dict] = []
        for row in reader:
            rows.append(
                {
                    "day": int(row["day"]),
                    "imsak": row["imsak"].strip(),
                    "fajr": row["fajr"].strip(),
                    "sunrise": row["sunrise"].strip(),
                    "dhuhr": row["dhuhr"].strip(),
                    "asr": row["asr"].strip(),
                    "maghrib": row["maghrib"].strip(),
                    "isha": row["isha"].strip(),
                }
            )
        return rows


def upsert_month(data: dict, city: str, year: int, month: int, rows: list[dict]) -> None:
    normalized_city = normalize_city_key(city)
    cities = data.setdefault("cities", [])
    city_entry = next((item for item in cities if item.get("city") == normalized_city), None)
    if city_entry is None:
        city_entry = {"city": normalized_city, "months": []}
        cities.append(city_entry)

    months = city_entry.setdefault("months", [])
    month_entry = next((item for item in months if item.get("year") == year and item.get("month") == month), None)
    if month_entry is None:
        month_entry = {"year": year, "month": month, "days": []}
        months.append(month_entry)

    month_entry["days"] = sorted(rows, key=lambda item: item["day"])


def main() -> None:
    parser = argparse.ArgumentParser(description="Update Qafqazislam prayer time asset from CSV/TSV.")
    parser.add_argument("--city", required=True, help="City name, e.g. Baku")
    parser.add_argument("--year", required=True, type=int)
    parser.add_argument("--month", required=True, type=int)
    parser.add_argument("--input", required=True, help="CSV/TSV file with prayer times")
    args = parser.parse_args()

    input_path = Path(args.input).resolve()
    if not input_path.exists():
        raise SystemExit(f"Input file not found: {input_path}")

    asset = load_asset()
    rows = parse_rows(input_path)
    if not rows:
        raise SystemExit("No rows found in input file")

    asset["version"] = int(asset.get("version", 1)) + 1
    upsert_month(asset, args.city, args.year, args.month, rows)
    save_asset(asset)
    print(f"Updated {args.city} {args.month}/{args.year} with {len(rows)} rows")


if __name__ == "__main__":
    main()
