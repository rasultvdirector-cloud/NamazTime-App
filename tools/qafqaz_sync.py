#!/usr/bin/env python3
from __future__ import annotations

import argparse
import calendar as pycalendar
import json
import re
import subprocess
import tempfile
import unicodedata
from dataclasses import dataclass
from datetime import date
from html import unescape
from pathlib import Path
from typing import Iterable
from urllib.parse import urljoin
from urllib.request import urlopen, urlretrieve


ROOT = Path(__file__).resolve().parents[1]
ASSET_PATH = ROOT / "android_app" / "app" / "src" / "main" / "assets" / "azerbaijan_prayer_times.json"
DOCS_ROOT = ROOT / "docs" / "namaz-time-data" / "azerbaijan"
OCR_SCRIPT = ROOT / "tools" / "qafqaz_ocr.swift"
BASE_URL = "https://www.qafqazislam.com/"
TIMETABLE_PAGE = urljoin(BASE_URL, "index.php?lang=az&sectionid=123")
PRINT_PAGE = urljoin(BASE_URL, "print_teqvim.php")
COUNTRY = "Azerbaijan"
TIME_RE = re.compile(r"^\d{2}:\d{2}$")
DAY_RE = re.compile(r"^\d{1,2}$")
OPTION_RE = re.compile(r'<option value="(\d+)"[^>]*>([^<]+)</option>')
IMG_RE = re.compile(r'<img src="([^"]+/teqvim/\d+/[^"]+/\d+\.jpg)"')

OFFICIAL_COLUMNS = [
    "day",
    "hijri_day",
    "weekday",
    "imsak",
    "fajr",
    "sunrise",
    "dhuhr",
    "asr",
    "sunset",
    "maghrib",
    "isha",
    "midnight",
]

APP_FIELDS = ["day", "imsak", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]

COLUMN_CENTERS = {
    "day": 0.080,
    "hijri_day": 0.145,
    "weekday": 0.232,
    "imsak": 0.316,
    "fajr": 0.387,
    "sunrise": 0.457,
    "dhuhr": 0.530,
    "asr": 0.601,
    "sunset": 0.672,
    "maghrib": 0.744,
    "isha": 0.814,
    "midnight": 0.886,
}

TEXT_BANDS = {
    "hijri_day": (0.108, 0.200),
    "weekday": (0.200, 0.286),
}


@dataclass(frozen=True)
class Observation:
    y: float
    x: float
    text: str


def normalize_key(raw: str) -> str:
    normalized = (
        raw.strip()
        .replace("ə", "e")
        .replace("Ə", "E")
        .replace("ı", "i")
        .replace("I", "I")
        .replace("ö", "o")
        .replace("Ö", "O")
        .replace("ü", "u")
        .replace("Ü", "U")
        .replace("ğ", "g")
        .replace("Ğ", "G")
        .replace("ç", "c")
        .replace("Ç", "C")
        .replace("ş", "s")
        .replace("Ş", "S")
    )
    normalized = unicodedata.normalize("NFKD", normalized)
    normalized = normalized.encode("ascii", "ignore").decode("ascii")
    normalized = normalized.lower().strip()
    normalized = re.sub(r"[^a-z0-9]+", "", normalized)
    return normalized


def canonical_city_key(raw: str) -> str:
    aliases = {
        "baki": "baku",
        "baku": "baku",
        "gence": "ganja",
        "ganja": "ganja",
        "sumqayit": "sumgait",
        "sumgayit": "sumgait",
        "sumgait": "sumgait",
        "naxcivan": "nakhchivan",
        "nakhchivan": "nakhchivan",
        "seki": "sheki",
        "sheki": "sheki",
        "lenkeran": "lankaran",
        "lankaran": "lankaran",
        "mingecevir": "mingachevir",
        "mingachevir": "mingachevir",
        "sirvan": "shirvan",
        "shirvan": "shirvan",
        "yevlax": "yevlakh",
        "yevlakh": "yevlakh",
    }
    key = normalize_key(raw)
    return aliases.get(key, key)


def normalize_time_text(raw: str) -> str:
    text = raw.strip().upper()
    text = text.replace(".", ":").replace("O", "0")
    text = re.sub(r"[^0-9:]", "", text)
    if re.fullmatch(r"\d{4}", text):
        text = f"{text[:2]}:{text[2:]}"
    if re.fullmatch(r"\d{2}:\d{2}", text):
        return text
    return raw.strip()


def canonical_city_name(raw: str) -> str:
    aliases = {
        "baku": "Baku",
        "ganja": "Ganja",
        "sumgait": "Sumgait",
        "nakhchivan": "Nakhchivan",
        "sheki": "Sheki",
        "lankaran": "Lankaran",
        "mingachevir": "Mingachevir",
        "shirvan": "Shirvan",
        "yevlakh": "Yevlakh",
    }
    key = canonical_city_key(raw)
    return aliases.get(key, raw.strip())


def city_slug(raw: str) -> str:
    return canonical_city_key(raw)


def read_url(url: str) -> str:
    with urlopen(url, timeout=30) as response:
        return response.read().decode("utf-8", errors="replace")


def fetch_city_options() -> list[tuple[str, str]]:
    html = read_url(TIMETABLE_PAGE)
    options = [(city_id, unescape(label).strip()) for city_id, label in OPTION_RE.findall(html)]
    if not options:
        raise SystemExit("Could not parse city list from Qafqazislam page.")
    return options


def fetch_image_url(city_id: str, year: int, month: int) -> str:
    html = read_url(f"{PRINT_PAGE}?sheher={city_id}&il={year}&ay={month}")
    match = IMG_RE.search(html)
    if not match:
        raise SystemExit(f"Could not find timetable image for city_id={city_id} {year}-{month:02d}")
    return urljoin(BASE_URL, match.group(1).lstrip("./"))


def convert_to_png(jpg_path: Path, png_path: Path) -> None:
    subprocess.run(
        ["sips", "-s", "format", "png", str(jpg_path), "--out", str(png_path)],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


def run_ocr(png_path: Path) -> list[Observation]:
    output = subprocess.check_output(
        ["swift", str(OCR_SCRIPT), str(png_path)],
        text=True,
    )
    observations: list[Observation] = []
    for line in output.splitlines():
        parts = line.split("\t", 2)
        if len(parts) != 3:
            continue
        y, x, text = parts
        observations.append(Observation(y=float(y), x=float(x), text=text.strip()))
    return observations


def dedupe_row_centers(observations: Iterable[Observation]) -> list[tuple[int, float]]:
    imsak_cells = [
        item
        for item in observations
        if TIME_RE.fullmatch(item.text)
        and abs(item.x - COLUMN_CENTERS["imsak"]) <= 0.03
    ]
    imsak_cells.sort(key=lambda item: item.y, reverse=True)
    row_centers: list[float] = []
    for item in imsak_cells:
        if row_centers and abs(row_centers[-1] - item.y) < 0.008:
            continue
        row_centers.append(item.y)
    return [(index + 1, row_y) for index, row_y in enumerate(row_centers)]


def validate_day_column(observations: Iterable[Observation], rows: list[tuple[int, float]]) -> None:
    day_cells = [
        item
        for item in observations
        if 0.05 <= item.x <= 0.10 and DAY_RE.fullmatch(item.text)
    ]
    for item in day_cells:
        day = int(item.text)
        if day < 1 or day > len(rows):
            continue
        row_y = rows[day - 1][1]
        if abs(item.y - row_y) > 0.012:
            raise SystemExit(f"Day column mismatch near day {day}: OCR y={item.y:.4f}, row y={row_y:.4f}")


def nearest_time(observations: Iterable[Observation], row_y: float, column: str) -> str:
    center = COLUMN_CENTERS[column]
    candidates = [
        item
        for item in observations
        if TIME_RE.fullmatch(normalize_time_text(item.text))
        and abs(item.y - row_y) <= 0.009
        and abs(item.x - center) <= 0.03
    ]
    if not candidates:
        raise SystemExit(f"Missing {column} value near row y={row_y:.4f}")
    candidates.sort(key=lambda item: (abs(item.x - center), abs(item.y - row_y)))
    return normalize_time_text(candidates[0].text)


def join_text_band(observations: Iterable[Observation], row_y: float, band: tuple[float, float]) -> str:
    left, right = band
    candidates = [
        item
        for item in observations
        if left <= item.x <= right and abs(item.y - row_y) <= 0.0105
    ]
    candidates.sort(key=lambda item: item.x)
    text = " ".join(item.text for item in candidates).strip()
    return re.sub(r"\s+", " ", text)


def parse_month(observations: list[Observation]) -> list[dict]:
    rows = dedupe_row_centers(observations)
    validate_day_column(observations, rows)
    parsed: list[dict] = []
    for day, row_y in rows:
        row = {
            "day": day,
            "hijriDay": join_text_band(observations, row_y, TEXT_BANDS["hijri_day"]),
            "weekday": join_text_band(observations, row_y, TEXT_BANDS["weekday"]),
            "imsak": nearest_time(observations, row_y, "imsak"),
            "fajr": nearest_time(observations, row_y, "fajr"),
            "sunrise": nearest_time(observations, row_y, "sunrise"),
            "dhuhr": nearest_time(observations, row_y, "dhuhr"),
            "asr": nearest_time(observations, row_y, "asr"),
            "sunset": nearest_time(observations, row_y, "sunset"),
            "maghrib": nearest_time(observations, row_y, "maghrib"),
            "isha": nearest_time(observations, row_y, "isha"),
            "midnight": nearest_time(observations, row_y, "midnight"),
        }
        parsed.append(row)
    return parsed


def to_asset_days(days: list[dict]) -> list[dict]:
    return [{key: day[key] for key in APP_FIELDS} for day in days]


def load_asset() -> dict:
    return json.loads(ASSET_PATH.read_text(encoding="utf-8"))


def save_asset(asset: dict) -> None:
    ASSET_PATH.write_text(json.dumps(asset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def upsert_asset_month(asset: dict, city_name: str, year: int, month: int, days: list[dict]) -> bool:
    cities = asset.setdefault("cities", [])
    key = city_slug(city_name)
    matching_entries = [item for item in cities if city_slug(item["city"]) == key]
    city_entry = matching_entries[0] if matching_entries else None
    changed = False
    if city_entry is None:
        city_entry = {"city": city_name, "months": []}
        cities.append(city_entry)
        changed = True
    else:
        canonical_name = canonical_city_name(city_name)
        if city_entry.get("city") != canonical_name:
            city_entry["city"] = canonical_name
            changed = True
        if len(matching_entries) > 1:
            merged_months = city_entry.setdefault("months", [])
            seen = {(item["year"], item["month"]) for item in merged_months}
            for extra_entry in matching_entries[1:]:
                for extra_month in extra_entry.get("months", []):
                    month_key = (extra_month["year"], extra_month["month"])
                    if month_key not in seen:
                        merged_months.append(extra_month)
                        seen.add(month_key)
                cities.remove(extra_entry)
            changed = True
    months = city_entry.setdefault("months", [])
    month_entry = next((item for item in months if item.get("year") == year and item.get("month") == month), None)
    asset_days = to_asset_days(days)
    if month_entry == {"year": year, "month": month, "days": asset_days}:
        return changed
    if month_entry is None:
        months.append({"year": year, "month": month, "days": asset_days})
        changed = True
    else:
        month_entry["days"] = asset_days
        changed = True
    months.sort(key=lambda item: (item["year"], item["month"]))
    cities.sort(key=lambda item: city_slug(item["city"]))
    return changed


def upsert_docs_month(city_name: str, year: int, month: int, days: list[dict], image_url: str) -> Path:
    slug = city_slug(city_name)
    out_dir = DOCS_ROOT / slug
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{year:04d}-{month:02d}.json"
    payload = {
        "source": "qafqazislam-image-ocr",
        "officialColumnOrder": OFFICIAL_COLUMNS,
        "city": city_name,
        "country": COUNTRY,
        "month": f"{year:04d}-{month:02d}",
        "imageUrl": image_url,
        "days": days,
    }
    out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return out_path


def iter_target_months(year: int, month: int, include_next_month: bool) -> list[tuple[int, int]]:
    months = [(year, month)]
    if include_next_month:
        next_year, next_month = year, month + 1
        if next_month == 13:
            next_year += 1
            next_month = 1
        months.append((next_year, next_month))
    return months


def sync_city(city_id: str, city_name: str, year: int, month: int) -> dict:
    image_url = fetch_image_url(city_id, year, month)
    with tempfile.TemporaryDirectory(prefix="qafqaz_sync_") as temp_dir:
        temp_path = Path(temp_dir)
        jpg_path = temp_path / "month.jpg"
        png_path = temp_path / "month.png"
        urlretrieve(image_url, jpg_path)
        convert_to_png(jpg_path, png_path)
        observations = run_ocr(png_path)
    days = parse_month(observations)
    if len(days) != pycalendar.monthrange(year, month)[1]:
        raise SystemExit(f"Expected {pycalendar.monthrange(year, month)[1]} days, got {len(days)} for {city_name} {year}-{month:02d}")
    docs_path = upsert_docs_month(city_name, year, month, days, image_url)
    return {
        "city": city_name,
        "docs_path": str(docs_path),
        "days": days,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Sync Qafqazislam timetable images into app-ready JSON.")
    parser.add_argument("--year", type=int)
    parser.add_argument("--month", type=int)
    parser.add_argument("--city-id", help="Qafqazislam city id from the website")
    parser.add_argument("--city-name", help="Human-readable city name")
    parser.add_argument("--all-cities", action="store_true", help="Sync every city exposed on qafqazislam.com")
    parser.add_argument("--include-next-month", action="store_true", help="Also sync the next month")
    parser.add_argument("--current-window", action="store_true", help="Use today's month as the base window")
    args = parser.parse_args()

    if args.current_window:
        today = date.today()
        year = today.year
        month = today.month
    else:
        if args.year is None or args.month is None:
            raise SystemExit("Pass --current-window or both --year and --month.")
        year = args.year
        month = args.month

    if args.all_cities:
        targets = fetch_city_options()
    else:
        if not args.city_id or not args.city_name:
            raise SystemExit("Pass --all-cities or both --city-id and --city-name.")
        targets = [(args.city_id, args.city_name)]

    asset = load_asset()
    changed = False
    synced: list[str] = []

    for target_year, target_month in iter_target_months(year, month, args.include_next_month):
        for city_id, raw_city_name in targets:
            city_name = canonical_city_name(raw_city_name)
            result = sync_city(city_id, city_name, target_year, target_month)
            asset_changed = upsert_asset_month(asset, city_name, target_year, target_month, result["days"])
            changed = changed or asset_changed
            synced.append(f"{city_name} {target_year:04d}-{target_month:02d}")
            print(f"Synced {city_name} {target_year:04d}-{target_month:02d} -> {result['docs_path']}")

    if changed:
        asset["version"] = int(asset.get("version", 1)) + 1
        save_asset(asset)
        print(f"Updated asset version -> {asset['version']}")
    else:
        print("Asset already up to date.")

    print(f"Completed {len(synced)} sync job(s).")


if __name__ == "__main__":
    main()
