"""
Seed script to add agriculture courses with rich lessons and randomized quizzes.

Usage:
    python scripts/seed_courses.py

Configuration (env vars):
    MONGODB_URI: Mongo connection string (default: mongodb://localhost:27017)
    MONGODB_DB:  Database name (default: agra-platform)
"""

import datetime
import os
import random
import uuid
from typing import Dict, List

from pymongo import MongoClient
from pymongo.errors import ConfigurationError


def new_id() -> str:
    return str(uuid.uuid4())


def get_db():
    uri = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
    db_name = os.getenv("MONGODB_DB", "agra-platform")
    client = MongoClient(uri)
    try:
        db = client.get_default_database()
    except ConfigurationError:
        db = None
    if db is None:
        db = client[db_name]
    return db


QUIZ_BANK = [
    {
        "topic": "soil",
        "question": "Which soil texture drains the fastest after heavy rain?",
        "answers": [
            ("Clay", False),
            ("Sandy", True),
            ("Silty clay", False),
            ("Peat", False),
        ],
    },
    {
        "topic": "soil",
        "question": "Adding composted manure each season primarily improves which soil property?",
        "answers": [
            ("Organic matter and structure", True),
            ("Salinity", False),
            ("Subsoil compaction", False),
            ("Soil color only", False),
        ],
    },
    {
        "topic": "soil",
        "question": "What is the ideal soil pH range for most field crops?",
        "answers": [
            ("4.5 - 5.0", False),
            ("5.5 - 6.0", False),
            ("6.0 - 7.0", True),
            ("8.0 - 8.5", False),
        ],
    },
    {
        "topic": "crop",
        "question": "Why does crop rotation lower pest pressure?",
        "answers": [
            ("It interrupts pest life cycles that depend on a single host", True),
            ("It permanently sterilizes the soil", False),
            ("It requires more insecticide", False),
            ("It removes all weeds automatically", False),
        ],
    },
    {
        "topic": "crop",
        "question": "Certified seed benefits farmers because it is",
        "answers": [
            ("Verified for purity and germination", True),
            ("Always the cheapest option", False),
            ("Immune to drought", False),
            ("Guaranteed organic", False),
        ],
    },
    {
        "topic": "crop",
        "question": "Which practice helps prevent lodging in cereals?",
        "answers": [
            ("Balanced nitrogen timing", True),
            ("Late-season flood irrigation", False),
            ("Skipping basal fertilizer", False),
            ("Planting at very high density", False),
        ],
    },
    {
        "topic": "water",
        "question": "A key advantage of drip irrigation is that it",
        "answers": [
            ("Delivers water directly to the root zone with minimal evaporation", True),
            ("Requires the highest pumping pressure", False),
            ("Works best only on clay soils", False),
            ("Eliminates the need for filtration", False),
        ],
    },
    {
        "topic": "water",
        "question": "What does ET stand for when scheduling irrigation?",
        "answers": [
            ("Evapotranspiration", True),
            ("Erosion threshold", False),
            ("Electrical transmittance", False),
            ("Effective tilth", False),
        ],
    },
    {
        "topic": "water",
        "question": "Mulching between crop rows primarily",
        "answers": [
            ("Reduces evaporation and suppresses weeds", True),
            ("Increases soil salinity", False),
            ("Prevents infiltration", False),
            ("Raises canopy temperature", False),
        ],
    },
    {
        "topic": "postharvest",
        "question": "Why should produce be cooled rapidly after harvest?",
        "answers": [
            ("Respiration and decay slow down at lower temperatures", True),
            ("It removes all field heat permanently", False),
            ("To increase ethylene production", False),
            ("To reduce weight loss by freezing water", False),
        ],
    },
    {
        "topic": "postharvest",
        "question": "Which factor most influences shelf life of fresh leafy greens?",
        "answers": [
            ("Temperature and relative humidity control", True),
            ("Harvesting at noon", False),
            ("Using oversized crates", False),
            ("High nitrogen fertilization", False),
        ],
    },
    {
        "topic": "postharvest",
        "question": "Food safety plans (HACCP/GAP) help producers because they",
        "answers": [
            ("Identify and manage contamination risks along the chain", True),
            ("Guarantee premium prices regardless of quality", False),
            ("Replace the need for sanitation", False),
            ("Eliminate record keeping", False),
        ],
    },
]


COURSE_BLUEPRINTS = [
    {
        "title": "Foundations of Soil Health",
        "description": "Build resilient soils through testing, balanced fertility, cover crops, and erosion control tailored to mixed crop-livestock systems.",
        "goals": [
            "Interpret soil test reports and set fertility targets",
            "Increase organic matter and aggregation",
            "Use cover crops to fix nitrogen and cycle nutrients",
            "Control erosion on slopes and light soils",
        ],
        "domain": "Agronomy",
        "country": "Global",
        "languagesAvailable": ["en"],
        "lessons": [
            ("Soil as a living system", "Soil is a biological factory. Bacteria, fungi, and fauna drive nutrient cycling and aggregate stability. Managing for biology means minimizing bare soil, keeping roots growing, and feeding microbes with residues and compost."),
            ("Reading a soil test", "Good programs start with data. Key numbers: pH for nutrient availability, organic matter for structure, cation exchange capacity for buffering, and nutrient levels by crop removal rates. Calibrate lime and fertilizer by yield targets, not blanket doses."),
            ("Organic matter management", "Composted manure, crop residues, and green manures increase stable carbon. Even a 1% organic matter gain can improve water holding, infiltration, and cation exchange. Avoid burning residues; incorporate or mulch to protect the surface."),
            ("Balancing pH and liming", "Most crops thrive at pH 6.0–7.0. Liming raises pH and supplies Ca/Mg; sulfur can lower pH in alkaline soils. Apply lime based on buffer pH and soil texture to avoid over-application and micronutrient tie-up."),
            ("Erosion control and cover crops", "Contour planting, grassed waterways, and cover crop roots reduce runoff. Mix grasses for rooting density with legumes for N supply. Terminate covers at the right stage to avoid moisture competition."),
        ],
        "quiz_topics": ["soil", "soil"],
    },
    {
        "title": "Climate-Smart Crop Production",
        "description": "Plan rotations, choose resilient varieties, and integrate IPM to buffer climate shocks while sustaining yields.",
        "goals": [
            "Select varieties for heat, drought, or disease pressure",
            "Design rotations that break pest cycles and improve soil",
            "Integrate biological and cultural controls before pesticides",
            "Align nutrient timing with crop demand under variable rainfall",
        ],
        "domain": "Crop Systems",
        "country": "Global",
        "languagesAvailable": ["en"],
        "lessons": [
            ("Mapping climate risks", "Heat waves, erratic rainfall, and new pest ranges are the core climate risks. Use historical weather, degree days, and seasonal forecasts to shift planting windows and pick tolerant varieties."),
            ("Seed and variety decisions", "Certified seed reduces seed-borne disease. Pair drought-tolerant hybrids with early vigor traits on light soils. For humid zones, prioritize disease packages and stay current with resistance ratings."),
            ("Rotation design", "Rotations that alternate deep and shallow roots improve structure and reduce nutrient mining. Follow heavy feeders with legumes. Avoid planting the same family back-to-back to limit pathogens like fusarium and nematodes."),
            ("Integrated pest management", "Scout weekly, use thresholds, and rotate modes of action. Promote natural enemies with flowering borders. Start with cultural controls—clean seed, residue management, planting density—before spraying."),
            ("Nutrient timing under variability", "Split nitrogen, use banding to reduce losses, and place P close to seed in cool soils. In drought-prone fields, favor stabilized N and fertigate when possible to match peak uptake."),
        ],
        "quiz_topics": ["crop", "crop"],
    },
    {
        "title": "Efficient Irrigation and Water Stewardship",
        "description": "Design, operate, and schedule irrigation systems that save water, protect soil, and keep crops on target yields.",
        "goals": [
            "Calculate crop water needs using evapotranspiration",
            "Choose and maintain efficient irrigation methods",
            "Prevent salinity build-up and runoff losses",
            "Capture and store rainfall for supplemental irrigation",
        ],
        "domain": "Water Management",
        "country": "Global",
        "languagesAvailable": ["en"],
        "lessons": [
            ("Water balance basics", "Crop demand equals evapotranspiration minus effective rainfall. Soil texture and rooting depth set how much you can store. Track depletion to decide when to irrigate."),
            ("Choosing an irrigation method", "Drip targets the root zone with low pressure and minimal evaporation, while sprinklers suit uniform soils and germination. Furrow requires level fields and good intake rates to avoid runoff."),
            ("Setting up drip", "Filter every system, flush lines, and use pressure regulators on slopes. Place emitters near the root zone; in orchards, start with double lines to cover the canopy footprint as trees grow."),
            ("Scheduling with ET and sensors", "Blend ET data with tensiometers or capacitance probes. Irrigate before the managed allowable depletion is exceeded. Adjust runtime by crop stage and canopy cover."),
            ("Rainwater harvesting and drainage", "Roof catchment, lined ponds, and check dams store storm events. Pair harvesting with field drains to avoid waterlogging that starves roots of oxygen."),
        ],
        "quiz_topics": ["water", "water"],
    },
    {
        "title": "Post-Harvest Handling and Value Addition",
        "description": "Maintain quality from field to market with gentle handling, rapid cooling, and simple processing steps that add value safely.",
        "goals": [
            "Harvest at proper maturity indices for each crop group",
            "Cool produce quickly and keep cold chain intact",
            "Reduce mechanical damage and ethylene exposure",
            "Implement food safety checks and basic processing",
        ],
        "domain": "Post-Harvest",
        "country": "Global",
        "languagesAvailable": ["en"],
        "lessons": [
            ("Harvest timing and maturity", "Use color, firmness, dry matter, and soluble solids to decide harvest. Morning harvest preserves field heat advantage. Overripe fruit collapses during transport."),
            ("Gentle handling and packaging", "Avoid drops over 15 cm for fruits. Use ventilated crates and sanitize them regularly. Line containers for delicate produce to prevent bruising."),
            ("Cold chain basics", "Pre-cool leafy greens to 0–4°C and maintain 90–95% RH. Separate ethylene producers (bananas, tomatoes) from sensitive crops (leafy greens, cucumbers)."),
            ("Simple processing and drying", "Solar or mechanical dryers reduce moisture safely if airflow is steady. Blanching before drying preserves color. Label processed goods with batch and date."),
            ("Quality, safety, and certification", "Adopt GAP/HACCP steps: clean water, worker hygiene, equipment sanitation, and traceability. Certifications open markets when backed by records and audits."),
        ],
        "quiz_topics": ["postharvest", "postharvest"],
    },
]


def build_quiz(topic: str, order: int) -> Dict:
    pool = [q for q in QUIZ_BANK if q["topic"] == topic]
    if not pool:
        return {}
    questions = random.sample(pool, k=min(3, len(pool)))
    quiz_questions = []
    for q in questions:
        answers = [
            {"id": new_id(), "text": text, "correct": correct} for text, correct in q["answers"]
        ]
        random.shuffle(answers)
        quiz_questions.append(
            {
                "id": new_id(),
                "question": q["question"],
                "answers": answers,
            }
        )
    return {
        "id": new_id(),
        "title": f"Quiz on {topic.title()}",
        "content": "",
        "order": order,
        "type": "QUIZ",
        "quizQuestions": quiz_questions,
    }


def build_text_content(blueprint: Dict) -> List[Dict]:
    blocks: List[Dict] = []
    order = 1
    for lesson_title, lesson_body in blueprint["lessons"]:
        blocks.append(
            {
                "id": new_id(),
                "title": lesson_title,
                "content": lesson_body,
                "order": order,
                "type": "LESSON",
                "quizQuestions": None,
            }
        )
        order += 1
    for topic in blueprint.get("quiz_topics", []):
        quiz_block = build_quiz(topic, order)
        if quiz_block:
            blocks.append(quiz_block)
            order += 1
    return blocks


def make_course_documents() -> List[Dict]:
    docs = []
    now = datetime.datetime.utcnow()
    for bp in COURSE_BLUEPRINTS:
        docs.append(
            {
                "title": bp["title"],
                "description": bp["description"],
                "goals": bp["goals"],
                "domain": bp["domain"],
                "country": bp["country"],
                "trainerId": bp.get("trainerId", "seed-script"),
                "sessionIds": [],
                "languagesAvailable": bp["languagesAvailable"],
                "createdAt": now,
                "updatedAt": now,
                "archived": False,
                "activeCall": False,
                "imagePublicId": None,
                "imageUrl": None,
                "thumbnailUrl": None,
                "detailImageUrl": None,
                "videoUrl": None,
                "videoPublicId": None,
                "files": [],
                "textContent": build_text_content(bp),
            }
        )
    return docs


def upsert_courses():
    random.seed(42)
    db = get_db()
    collection = db["courses"]
    docs = make_course_documents()

    inserted, updated = 0, 0
    for doc in docs:
        result = collection.replace_one({"title": doc["title"]}, doc, upsert=True)
        if result.matched_count:
            updated += 1
        elif result.upserted_id:
            inserted += 1

    print(f"Courses processed: {len(docs)} (inserted={inserted}, updated={updated})")


if __name__ == "__main__":
    upsert_courses()
