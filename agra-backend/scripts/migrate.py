from __future__ import annotations

import os
from datetime import datetime
from typing import Any, Dict

from bson import ObjectId
from pymongo import MongoClient

MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017")
DB_NAME = os.getenv("MONGO_DB", "agra-platform")
COLLECTION = os.getenv("MONGO_COLLECTION", "courses")
COURSE_ID = os.getenv("COURSE_ID", "68d97f561c8630af6e2d7d76")

LESSON_1_AR_TITLE = "الأسس والخلفية"
LESSON_1_AR_CONTENT = (
    "يُعد علم الزراعة (Agronomy) مجالًا حيويًا يؤثر بشكل مباشر على التنمية الاقتصادية، "
    "والأمن الغذائي، والاستدامة البيئية. يركز هذا المجال على دراسة إنتاج المحاصيل، "
    "وإدارة التربة، وتحسين الموارد الزراعية باستخدام مبادئ علمية وتطبيقات عملية. "
    "يوفر فهم الخلفية التاريخية والعلمية لعلم الزراعة أساسًا قويًا لتحليل التحديات "
    "الزراعية الحديثة والتعامل معها بفعالية."
)
LESSON_1_FR_TITLE = "Fondements et contexte"
LESSON_1_FR_CONTENT = (
    "L’agronomie est un domaine essentiel qui influence directement le développement "
    "économique, la sécurité alimentaire et la durabilité environnementale. Elle se "
    "concentre sur l’étude de la production végétale, la gestion des sols et "
    "l’optimisation des ressources agricoles à l’aide de principes scientifiques et "
    "d’applications pratiques. Comprendre le contexte historique et scientifique de "
    "l’agronomie permet de mieux analyser et relever les défis agricoles contemporains."
)

LESSON_2_AR_TITLE = "المفاهيم الأساسية والتطبيق العملي"
LESSON_2_AR_CONTENT = (
    "تتناول هذه الدرس المفاهيم الأساسية والأطر التحليلية المستخدمة في علم الزراعة، "
    "مع التركيز على كيفية تطبيق المعرفة النظرية في مواقف واقعية. يتعلم المتدربون "
    "كيفية تقييم المشكلات الزراعية، وتحليل البيانات، واتخاذ قرارات مدروسة لتحسين "
    "الإنتاجية والاستدامة. يربط هذا الدرس بين النظرية والتطبيق العملي لتعزيز الفهم "
    "الشامل للمجال."
)
LESSON_2_FR_TITLE = "Concepts fondamentaux et application pratique"
LESSON_2_FR_CONTENT = (
    "Cette leçon examine les concepts clés et les cadres analytiques utilisés en "
    "agronomie, en mettant l’accent sur l’application des connaissances théoriques à "
    "des situations concrètes. Les apprenants développent leur capacité à évaluer des "
    "problématiques agricoles, analyser des données et prendre des décisions éclairées "
    "afin d’améliorer la productivité et la durabilité. Cette approche relie la théorie "
    "à la pratique pour une compréhension approfondie du domaine."
)

QUIZ_AR_TITLE = "اختبار مراجعة المفاهيم"
QUIZ_AR_CONTENT = "أجب عن الأسئلة التالية لتقييم مدى فهمك للمفاهيم الأساسية."
QUIZ_FR_TITLE = "Quiz de révision des concepts"
QUIZ_FR_CONTENT = "Répondez aux questions suivantes pour évaluer votre compréhension."

QUIZ_Q_AR = "لماذا يُعد فهم المفاهيم الأساسية أمرًا مهمًا؟"
QUIZ_Q_FR = "Pourquoi est-il important de bien comprendre les concepts fondamentaux ?"

ANSWER_1_AR = "يساعد على اتخاذ قرارات مدروسة وتقديم حلول فعّالة"
ANSWER_1_FR = "Cela permet de prendre des décisions éclairées et de proposer des solutions efficaces"

ANSWER_2_AR = "يُلغي الحاجة إلى التعاون"
ANSWER_2_FR = "Cela supprime le besoin de collaboration"

ANSWER_3_AR = "يفيد فقط في الامتحانات الأكاديمية"
ANSWER_3_FR = "Cela n’est utile que pour les examens académiques"


def _as_map(value: Any) -> Dict[str, str]:
    if isinstance(value, dict):
        return dict(value)
    if isinstance(value, str) and value:
        return {"en": value}
    return {}


def _as_translations(value: Any) -> Dict[str, Dict[str, str]]:
    if isinstance(value, dict):
        return dict(value)
    return {}


def _merge_locale_map(container: Dict[str, Any], key: str, locale: str, text: str) -> None:
    data = _as_map(container.get(key))
    data[locale] = text
    container[key] = data


def _merge_translation_map(
    container: Dict[str, Any],
    locale: str,
    title: str | None,
    content: str | None,
) -> None:
    translations = _as_translations(container.get("translations"))
    payload: Dict[str, str] = {}
    if title is not None:
        payload["title"] = title
    if content is not None:
        payload["content"] = content
    translations[locale] = payload
    container["translations"] = translations


def _merge_question_translation(container: Dict[str, Any], locale: str, question: str) -> None:
    translations = _as_translations(container.get("translations"))
    translations[locale] = {"question": question}
    container["translations"] = translations


def _merge_answer_translation(container: Dict[str, Any], locale: str, text: str) -> None:
    translations = _as_translations(container.get("translations"))
    translations[locale] = {"text": text}
    container["translations"] = translations


def _find_by_order(text_content: list[Dict[str, Any]], order: int) -> Dict[str, Any] | None:
    for item in text_content:
        if item.get("order") == order:
            return item
    return None


def _update_lesson(item: Dict[str, Any], ar_title: str, ar_content: str, fr_title: str, fr_content: str) -> None:
    title_map = _as_map(item.get("title"))
    content_map = _as_map(item.get("content"))
    en_title = title_map.get("en")
    en_content = content_map.get("en")

    _merge_locale_map(item, "title", "ar", ar_title)
    _merge_locale_map(item, "title", "fr", fr_title)
    _merge_locale_map(item, "content", "ar", ar_content)
    _merge_locale_map(item, "content", "fr", fr_content)

    if en_title:
        _merge_translation_map(item, "en", en_title, None)
    if en_content:
        translations = _as_translations(item.get("translations"))
        payload = translations.get("en", {})
        payload["content"] = en_content
        payload["title"] = payload.get("title", en_title)
        translations["en"] = payload
        item["translations"] = translations

    _merge_translation_map(item, "ar", ar_title, ar_content)
    _merge_translation_map(item, "fr", fr_title, fr_content)


def _update_quiz(item: Dict[str, Any]) -> None:
    title_map = _as_map(item.get("title"))
    content_map = _as_map(item.get("content"))
    en_title = title_map.get("en")
    en_content = content_map.get("en")

    _merge_locale_map(item, "title", "ar", QUIZ_AR_TITLE)
    _merge_locale_map(item, "title", "fr", QUIZ_FR_TITLE)
    _merge_locale_map(item, "content", "ar", QUIZ_AR_CONTENT)
    _merge_locale_map(item, "content", "fr", QUIZ_FR_CONTENT)

    if en_title:
        _merge_translation_map(item, "en", en_title, None)
    if en_content:
        translations = _as_translations(item.get("translations"))
        payload = translations.get("en", {})
        payload["content"] = en_content
        payload["title"] = payload.get("title", en_title)
        translations["en"] = payload
        item["translations"] = translations

    _merge_translation_map(item, "ar", QUIZ_AR_TITLE, QUIZ_AR_CONTENT)
    _merge_translation_map(item, "fr", QUIZ_FR_TITLE, QUIZ_FR_CONTENT)

    questions = item.get("quizQuestions") or []
    if questions:
        question = questions[0]
        _merge_locale_map(question, "question", "ar", QUIZ_Q_AR)
        _merge_locale_map(question, "question", "fr", QUIZ_Q_FR)
        _merge_question_translation(question, "ar", QUIZ_Q_AR)
        _merge_question_translation(question, "fr", QUIZ_Q_FR)

        answers = question.get("answers") or []
        if len(answers) >= 1:
            _merge_locale_map(answers[0], "text", "ar", ANSWER_1_AR)
            _merge_locale_map(answers[0], "text", "fr", ANSWER_1_FR)
            _merge_answer_translation(answers[0], "ar", ANSWER_1_AR)
            _merge_answer_translation(answers[0], "fr", ANSWER_1_FR)
        if len(answers) >= 2:
            _merge_locale_map(answers[1], "text", "ar", ANSWER_2_AR)
            _merge_locale_map(answers[1], "text", "fr", ANSWER_2_FR)
            _merge_answer_translation(answers[1], "ar", ANSWER_2_AR)
            _merge_answer_translation(answers[1], "fr", ANSWER_2_FR)
        if len(answers) >= 3:
            _merge_locale_map(answers[2], "text", "ar", ANSWER_3_AR)
            _merge_locale_map(answers[2], "text", "fr", ANSWER_3_FR)
            _merge_answer_translation(answers[2], "ar", ANSWER_3_AR)
            _merge_answer_translation(answers[2], "fr", ANSWER_3_FR)


def _find_course(collection, course_id: str) -> Dict[str, Any] | None:
    course = collection.find_one({"_id": course_id})
    if course:
        return course
    try:
        return collection.find_one({"_id": ObjectId(course_id)})
    except Exception:
        return None


def main() -> None:
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    courses = db[COLLECTION]

    course = _find_course(courses, COURSE_ID)
    if not course:
        raise SystemExit(f"Course not found for _id={COURSE_ID}")

    text_content = course.get("textContent") or []
    lesson_1 = _find_by_order(text_content, 1)
    lesson_2 = _find_by_order(text_content, 2)
    quiz = _find_by_order(text_content, 3)

    if not lesson_1 or not lesson_2 or not quiz:
        raise SystemExit("Course textContent missing expected items with order 1/2/3.")

    _update_lesson(lesson_1, LESSON_1_AR_TITLE, LESSON_1_AR_CONTENT, LESSON_1_FR_TITLE, LESSON_1_FR_CONTENT)
    _update_lesson(lesson_2, LESSON_2_AR_TITLE, LESSON_2_AR_CONTENT, LESSON_2_FR_TITLE, LESSON_2_FR_CONTENT)
    _update_quiz(quiz)

    result = courses.update_one(
        {"_id": course["_id"]},
        {"$set": {"textContent": text_content, "updatedAt": datetime.utcnow()}},
    )

    print(f"Updated course {course['_id']} (matched={result.matched_count}, modified={result.modified_count}).")


if __name__ == "__main__":
    main()
