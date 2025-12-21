from pymongo import MongoClient
from datetime import datetime

# -------------------- Mongo Config --------------------
MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "agra-platform"
COLLECTION = "courses"

client = MongoClient(MONGO_URI)
db = client[DB_NAME]
courses = db[COLLECTION]


# -------------------- Helpers --------------------
def course_title(domain, country):
    return f"{domain} Fundamentals ({country})"


def course_description(domain):
    return f"An introductory course covering essential concepts in {domain.lower()}."


def french_title(domain, country):
    return f"Fondamentaux de {domain.lower()} ({country})"


def french_description(domain):
    return f"Un cours introductif couvrant les concepts essentiels de {domain.lower()}."


def arabic_title(domain, country):
    return f"أساسيات {domain} ({country})"


def arabic_description(domain):
    return f"دورة تمهيدية تغطي المفاهيم الأساسية في مجال {domain}."


# -------------------- Migration Logic --------------------
def migrate_course(course):
    domain = course.get("domain", "General")
    country = course.get("country", "XX")

    title_en = course_title(domain, country)
    desc_en = course_description(domain)

    title_fr = french_title(domain, country)
    desc_fr = french_description(domain)

    title_ar = arabic_title(domain, country)
    desc_ar = arabic_description(domain)

    migrated_text_content = []

    for index, item in enumerate(course.get("textContent", []), start=1):
        lesson_title_en = f"Lesson {index}"
        lesson_title_fr = f"Leçon {index}"
        lesson_title_ar = f"الدرس {index}"

        content_en = item.get("content", "This lesson introduces key ideas and concepts.")
        content_fr = (
            "Cette leçon présente les idées et concepts clés nécessaires à la compréhension du sujet."
        )
        content_ar = (
            "يقدم هذا الدرس الأفكار والمفاهيم الأساسية اللازمة لفهم الموضوع."
        )

        migrated_text_content.append({
            "id": f"lesson-{index}",
            "title": lesson_title_en,
            "content": content_en,
            "order": index,
            "type": "TEXT",
            "translations": {
                "en": {
                    "title": lesson_title_en,
                    "content": content_en
                },
                "fr": {
                    "title": lesson_title_fr,
                    "content": content_fr
                },
                "ar": {
                    "title": lesson_title_ar,
                    "content": content_ar
                }
            }
        })

    migrated_course = {
        "title": title_en,
        "description": desc_en,
        "goals": [
            "Understand fundamental concepts",
            "Apply knowledge in real-world contexts"
        ],
        "defaultLanguage": "en",
        "translations": {
            "en": {
                "title": title_en,
                "description": desc_en,
                "goals": [
                    "Understand fundamental concepts",
                    "Apply knowledge in real-world contexts"
                ]
            },
            "fr": {
                "title": title_fr,
                "description": desc_fr,
                "goals": [
                    "Comprendre les concepts fondamentaux",
                    "Appliquer les connaissances dans des contextes réels"
                ]
            },
            "ar": {
                "title": title_ar,
                "description": desc_ar,
                "goals": [
                    "فهم المفاهيم الأساسية",
                    "تطبيق المعرفة في سياقات واقعية"
                ]
            }
        },
        "domain": domain.lower(),
        "country": country,
        "textContent": migrated_text_content,
        "updatedAt": datetime.utcnow()
    }

    return migrated_course


# -------------------- Run Migration --------------------
count = 0

for course in courses.find():
    migrated = migrate_course(course)

    courses.update_one(
        {"_id": course["_id"]},
        {"$set": migrated}
    )

    count += 1

print(f"Migration completed successfully. {count} courses updated.")
