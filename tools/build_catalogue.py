#!/usr/bin/env python3
"""Build the app's seed asset from the prayer corpus.

    python3 tools/build_catalogue.py            # writes app/src/main/assets/prayer_catalogue.json
    python3 tools/build_catalogue.py --check     # fails if the asset is stale, writes nothing
    python3 tools/build_catalogue.py --enums     # prints the Kotlin enum bodies the asset needs

`docs/prayers/*.json` is the source of truth: one file per prayer, as generated and reviewed. This
script is the only thing that turns it into `assets/prayer_catalogue.json`, and the asset is
committed, so the line splitting below is reviewable in a diff and identical on every machine.

What it does, and why:

* Holds back any prayer a reviewer marked `"revise"`. Everything else ships.
* Splits each movement's paragraph into the lines a session is prayed in. The corpus gives prose;
  the session needs one thought per screen. Sentence enders and em dashes are where the prayer
  already breathes, so that is where it breaks.
* Drops the fields that are ours rather than the reader's: generation metadata, editorial notes,
  review verdicts, suggested tags.
* Validates before writing. A prayer with an unknown tag, a missing heading or an empty line is a
  build failure here rather than a crash on someone's phone.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CORPUS_DIR = REPO_ROOT / "docs" / "prayers"
ASSET_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "prayer_catalogue.json"

SEED_VERSION = 2

# A reviewer's verdict that the prayer is not ready. Absent verdicts are unreviewed, not rejected.
HELD_BACK_VERDICT = "revise"

# The book's own eleven parts, in the order they appear, mapped to the enum member that carries each.
PARTS = {
    "Introductory": "Introductory",
    "Father, Son, and Holy Spirit": "FatherSonAndHolySpirit",
    "Redemption and Reconciliation": "RedemptionAndReconciliation",
    "Penitence and Deprecation": "PenitenceAndDeprecation",
    "Needs and Devotions": "NeedsAndDevotions",
    "Holy Aspirations": "HolyAspirations",
    "Approach to God": "ApproachToGod",
    "Gifts of Grace": "GiftsOfGrace",
    "Service and Ministry": "ServiceAndMinistry",
    "Valediction": "Valediction",
    "A Week's Shared Prayers": "AWeeksSharedPrayers",
}

VOICES = {"personal": "Personal", "corporate": "Corporate"}

# The corpus's tag vocabulary, slug to enum member. A slug not listed here fails the build: the
# vocabulary is closed on purpose, so a new tag is a decision someone makes rather than a typo that
# ships.
TAGS = {
    "adoption": "Adoption",
    "assurance": "Assurance",
    "atonement": "Atonement",
    "christ-sufficiency": "ChristSufficiency",
    "church-and-community": "ChurchAndCommunity",
    "contentment": "Contentment",
    "death-and-eternity": "DeathAndEternity",
    "evangelism": "Evangelism",
    "faith": "Faith",
    "family": "Family",
    "father-heart-of-god": "FatherHeartOfGod",
    "fear-and-anxiety": "FearAndAnxiety",
    "forgiveness": "Forgiveness",
    "grace": "Grace",
    "guidance": "Guidance",
    "holiness-of-god": "HolinessOfGod",
    "holy-spirit": "HolySpirit",
    "humility": "Humility",
    "incarnation": "Incarnation",
    "joy": "Joy",
    "justification": "Justification",
    "lords-day": "LordsDay",
    "love-for-others": "LoveForOthers",
    "love-of-god": "LoveOfGod",
    "ministry-and-service": "MinistryAndService",
    "morning-and-evening": "MorningAndEvening",
    "prayer": "Prayer",
    "providence": "Providence",
    "reconciliation": "Reconciliation",
    "redemption": "Redemption",
    "repentance": "Repentance",
    "resurrection": "Resurrection",
    "sanctification": "Sanctification",
    "scripture": "Scripture",
    "second-coming": "SecondComing",
    "self-examination": "SelfExamination",
    "sin-and-conviction": "SinAndConviction",
    "sovereignty": "Sovereignty",
    "spiritual-dryness": "SpiritualDryness",
    "spiritual-warfare": "SpiritualWarfare",
    "suffering": "Suffering",
    "surrender": "Surrender",
    "temptation": "Temptation",
    "thanksgiving": "Thanksgiving",
    "trinity": "Trinity",
    "union-with-christ": "UnionWithChrist",
    "work-and-vocation": "WorkAndVocation",
    "worship": "Worship",
}

# Corrections to an adapted title, keyed by prayer id. The corpus files stay as generated; this is
# where a title that lost something in adaptation is put right, in the open.
#
# vov-170 is the *eve* of the Lord's Day — a week ending, Saturday evening — and vov-176 is Sunday
# evening itself. Adaptation gave both the title "Lord's Day Evening", which is wrong for the first
# and would have put two identical rows in the Library.
TITLE_OVERRIDES = {
    "vov-170-lords-day-eve": "Lord’s Day Eve",
}

# A sentence ender followed by space, or an em dash with or without spaces around it. Both are places
# the prayer already pauses; neither invents a break the writing does not have.
LINE_BREAK = re.compile(r"(?<=[.!?;:])\s+|\s*—\s*")


def split_into_lines(text: str) -> list[str]:
    """One prayed thought per line, in the order the paragraph says them."""
    return [line.strip() for line in LINE_BREAK.split(text) if line.strip()]


class CorpusError(Exception):
    """A prayer that cannot be seeded as it stands."""


def read_corpus() -> list[dict]:
    paths = sorted(CORPUS_DIR.glob("*.json"))
    if not paths:
        raise CorpusError(f"no prayers found in {CORPUS_DIR}")
    return [json.loads(path.read_text(encoding="utf-8")) for path in paths]


def is_held_back(prayer: dict) -> bool:
    return (prayer.get("review_summary") or {}).get("verdict") == HELD_BACK_VERDICT


def convert(prayer: dict, path_hint: str) -> dict:
    source = prayer["_source"]
    provenance = prayer["content_provenance"]

    part = PARTS.get(normalise(source["section_title"]))
    if part is None:
        raise CorpusError(f"{path_hint}: unknown part {source['section_title']!r}")

    voice = VOICES.get(source["voice"])
    if voice is None:
        raise CorpusError(f"{path_hint}: unknown voice {source['voice']!r}")

    tags = []
    for tag in prayer["tags"]:
        member = TAGS.get(tag)
        if member is None:
            raise CorpusError(f"{path_hint}: unknown tag {tag!r} — add it to TAGS and to PrayerTag")
        if member not in tags:
            tags.append(member)

    movements = []
    for position, section in enumerate(prayer["modern_prayer"]["sections"]):
        heading = section["heading"].strip()
        if not heading:
            raise CorpusError(f"{path_hint}: movement {position} has no heading")

        lines = split_into_lines(section["text"])
        if not lines:
            raise CorpusError(f"{path_hint}: movement {position} has no lines")

        scriptures = [
            {
                "reference": reference["reference"].strip(),
                "translation": reference["translation"].strip(),
                "connection": reference["connection"].strip(),
            }
            for reference in section["scripture_references"]
        ]
        if not scriptures:
            raise CorpusError(f"{path_hint}: movement {position} has no scripture reference")

        movements.append(
            {
                "heading": heading,
                "lines": lines,
                "themes": [theme.strip() for theme in section["theological_themes"]],
                "scriptures": scriptures,
            }
        )

    if not movements:
        raise CorpusError(f"{path_hint}: no movements")

    return {
        "id": source["id"],
        "title": TITLE_OVERRIDES.get(source["id"], prayer["title"].strip()),
        "part": part,
        "voice": voice,
        "tags": tags,
        "movements": movements,
        "provenance": {
            "originalTitle": provenance["original_title"],
            "originalAuthor": provenance["original_author"],
            "originalSource": provenance["original_source"],
            "originalPublicationDate": provenance["original_publication_date"],
            "copyrightStatus": provenance["source_copyright_status"],
            "adaptationType": provenance["adaptation_type"],
            "adaptationNote": provenance["modern_version_note"],
        },
    }


def normalise(text: str) -> str:
    """The corpus uses a curly apostrophe in one part title; match on a straight one."""
    return unicodedata.normalize("NFC", text).replace("’", "'")


def build() -> dict:
    corpus = read_corpus()
    seeded, held_back = [], []

    for prayer in sorted(corpus, key=lambda p: p["_source"]["order"]):
        path_hint = prayer["_source"]["id"]
        if is_held_back(prayer):
            held_back.append(path_hint)
            continue
        seeded.append(convert(prayer, path_hint))

    assert_unique(seeded, "id")
    assert_unique(seeded, "title")

    catalogue = {
        "version": SEED_VERSION,
        "corpusHash": corpus_hash(corpus),
        "prayers": seeded,
    }
    return catalogue, held_back


def assert_unique(prayers: list[dict], field: str) -> None:
    seen = {}
    for prayer in prayers:
        value = prayer[field]
        if value in seen:
            raise CorpusError(f"two prayers share a {field}: {seen[value]} and {prayer['id']}")
        seen[value] = prayer["id"]


def corpus_hash(corpus: list[dict]) -> str:
    """Identifies the corpus this asset was built from, so a stale asset can be spotted."""
    digest = hashlib.sha256()
    for prayer in sorted(corpus, key=lambda p: p["_source"]["id"]):
        digest.update(prayer["_generation"]["source_hash"].encode("utf-8"))
    return digest.hexdigest()


def render(catalogue: dict) -> str:
    return json.dumps(catalogue, ensure_ascii=False, indent=2) + "\n"


def print_enums(catalogue: dict) -> None:
    for slug, member in sorted(TAGS.items(), key=lambda pair: pair[1]):
        print(f'    {member}("{slug}"),')
    print()
    for title, member in PARTS.items():
        print(f'    {member}("{title}"),')


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if the asset is out of date")
    parser.add_argument("--enums", action="store_true", help="print the Kotlin enum bodies")
    args = parser.parse_args()

    try:
        catalogue, held_back = build()
    except (CorpusError, KeyError) as error:
        print(f"catalogue: {error}", file=sys.stderr)
        return 1

    if args.enums:
        print_enums(catalogue)
        return 0

    rendered = render(catalogue)
    lines = sum(len(movement["lines"]) for p in catalogue["prayers"] for movement in p["movements"])
    movements = sum(len(prayer["movements"]) for prayer in catalogue["prayers"])
    summary = (
        f"{len(catalogue['prayers'])} prayers, {movements} movements, {lines} lines"
        f" — {len(held_back)} held back for revision: {', '.join(held_back) or 'none'}"
    )

    if args.check:
        current = ASSET_PATH.read_text(encoding="utf-8") if ASSET_PATH.exists() else ""
        if current != rendered:
            print("catalogue: asset is stale, run tools/build_catalogue.py", file=sys.stderr)
            return 1
        print(f"catalogue up to date — {summary}")
        return 0

    ASSET_PATH.write_text(rendered, encoding="utf-8")
    print(f"wrote {ASSET_PATH.relative_to(REPO_ROOT)} — {summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
