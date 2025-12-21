from pymongo import MongoClient
from datetime import datetime

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "agra-platform"
COLLECTION = "courses"

client = MongoClient(MONGO_URI)
db = client[DB_NAME]
courses = db[COLLECTION]


def migrate_course(course):
    domain = course.get("domain", "General")
    country = course.get("country", "XX")

    # ---------------- Course Metadata ----------------
    title_en = f"{domain} Foundations ({country})"
    title_fr = f"Fondements de {domain.lower()} ({country})"
    title_ar = f"أسس {domain} ({country})"

    desc_en = (
        f"This course provides a comprehensive and practical introduction to the principles, "
        f"frameworks, and real-world applications of {domain.lower()}."
    )
    desc_fr = (
        f"Ce cours propose une introduction complète et pratique aux principes, "
        f"aux cadres et aux applications concrètes du domaine de {domain.lower()}."
    )
    desc_ar = (
        f"تقدم هذه الدورة مقدمة شاملة وعملية للمبادئ والأطر "
        f"والتطبيقات الواقعية في مجال {domain}."
    )

    goals_en = [
        "Understand foundational concepts in depth",
        "Develop analytical and critical thinking skills",
        "Apply theoretical knowledge to real situations"
    ]
    goals_fr = [
        "Comprendre en profondeur les concepts fondamentaux",
        "Développer des compétences analytiques et critiques",
        "Appliquer les connaissances théoriques à des situations réelles"
    ]
    goals_ar = [
        "فهم المفاهيم الأساسية بعمق",
        "تطوير مهارات التفكير التحليلي والنقدي",
        "تطبيق المعرفة النظرية في مواقف واقعية"
    ]

    # ---------------- Lesson 1 (RICH CONTENT) ----------------
    lesson1_en = (
        f"{domain} is a critical field that influences economic development, social well-being, "
        f"and long-term sustainability. Understanding its foundations is essential for anyone "
        f"involved in decision-making, planning, or implementation.\n\n"
        f"In this lesson, you will explore the historical evolution of {domain.lower()}, "
        f"including how key ideas emerged in response to societal needs and challenges. "
        f"You will also examine the role of institutions, stakeholders, and governance structures.\n\n"
        f"By the end of this lesson, you should be able to explain why {domain.lower()} matters, "
        f"identify its main components, and recognize its impact on communities and systems."
    )

    lesson1_fr = (
        f"{domain} est un domaine essentiel qui influence le développement économique, "
        f"le bien-être social et la durabilité à long terme. Comprendre ses fondements est "
        f"indispensable pour toute personne impliquée dans la prise de décision ou la planification.\n\n"
        f"Dans cette leçon, vous étudierez l’évolution historique de {domain.lower()}, "
        f"ainsi que l’émergence des idées clés en réponse aux besoins et défis sociétaux. "
        f"Vous analyserez également le rôle des institutions et des parties prenantes.\n\n"
        f"À la fin de cette leçon, vous serez en mesure d’expliquer l’importance de {domain.lower()}, "
        f"d’en identifier les principaux éléments et d’en comprendre l’impact sur les sociétés."
    )

    lesson1_ar = (
        f"يُعد مجال {domain} من المجالات الحيوية التي تؤثر في التنمية الاقتصادية "
        f"والرفاه الاجتماعي والاستدامة على المدى الطويل. إن فهم أسسه أمر ضروري "
        f"لكل من يشارك في التخطيط أو اتخاذ القرار.\n\n"
        f"في هذا الدرس، ستتعرف على التطور التاريخي لمجال {domain}، "
        f"وكيف ظهرت المفاهيم الأساسية استجابةً للتحديات المجتمعية. "
        f"كما ستدرس دور المؤسسات وأصحاب المصلحة وهياكل الحوكمة.\n\n"
        f"بنهاية هذا الدرس، ستكون قادرًا على شرح أهمية {domain} "
        f"وتحديد مكوناته الرئيسية وفهم تأثيره على المجتمعات والأنظمة."
    )

    lesson_1 = {
        "id": "lesson-1",
        "title": "Foundations and Background",
        "content": lesson1_en,
        "order": 1,
        "type": "TEXT",
        "translations": {
            "en": {"title": "Foundations and Background", "content": lesson1_en},
            "fr": {"title": "Fondements et contexte", "content": lesson1_fr},
            "ar": {"title": "الأسس والخلفية", "content": lesson1_ar}
        }
    }

    # ---------------- Lesson 2 (RICH CONTENT) ----------------
    lesson2_en = (
        f"This lesson examines the core concepts and analytical frameworks used in {domain.lower()}. "
        f"These frameworks help professionals assess challenges, evaluate options, and design solutions.\n\n"
        f"You will explore how theory is translated into practice through real-world examples, "
        f"including policy design, implementation strategies, and performance evaluation.\n\n"
        f"The lesson also highlights common challenges, such as limited resources, "
        f"conflicting stakeholder interests, and uncertainty, and discusses strategies "
        f"to address them effectively."
    )

    lesson2_fr = (
        f"Cette leçon examine les concepts clés et les cadres analytiques utilisés en {domain.lower()}. "
        f"Ces outils permettent d’évaluer les défis, de comparer les options et de concevoir des solutions.\n\n"
        f"Vous verrez comment la théorie se transforme en pratique à travers des exemples concrets, "
        f"notamment la conception et la mise en œuvre de stratégies.\n\n"
        f"La leçon aborde également les défis courants tels que les ressources limitées, "
        f"les intérêts divergents et l’incertitude, ainsi que les moyens d’y faire face."
    )

    lesson2_ar = (
        f"يركز هذا الدرس على المفاهيم الأساسية والأطر التحليلية المستخدمة في مجال {domain}. "
        f"تساعد هذه الأطر المتخصصين على تقييم التحديات وتصميم الحلول المناسبة.\n\n"
        f"ستتعرف على كيفية تحويل النظرية إلى تطبيق عملي من خلال أمثلة واقعية، "
        f"بما في ذلك تصميم السياسات واستراتيجيات التنفيذ وتقييم الأداء.\n\n"
        f"كما يناقش الدرس التحديات الشائعة مثل محدودية الموارد وتضارب المصالح "
        f"وعدم اليقين، ويقترح أساليب فعالة للتعامل معها."
    )

    lesson_2 = {
        "id": "lesson-2",
        "title": "Core Concepts and Practical Application",
        "content": lesson2_en,
        "order": 2,
        "type": "TEXT",
        "translations": {
            "en": {"title": "Core Concepts and Practical Application", "content": lesson2_en},
            "fr": {"title": "Concepts clés et application pratique", "content": lesson2_fr},
            "ar": {"title": "المفاهيم الأساسية والتطبيق العملي", "content": lesson2_ar}
        }
    }

    # ---------------- Quiz (unchanged, still rich) ----------------
    quiz = {
        "id": "quiz-1",
        "title": "Concept Review Quiz",
        "content": "Answer the following questions to assess your understanding.",
        "order": 3,
        "type": "QUIZ",
        "translations": {
            "en": {"title": "Concept Review Quiz", "content": "Answer the following questions to assess your understanding."},
            "fr": {"title": "Quiz de révision des concepts", "content": "Répondez aux questions suivantes pour évaluer votre compréhension."},
            "ar": {"title": "اختبار مراجعة المفاهيم", "content": "أجب عن الأسئلة التالية لتقييم مستوى فهمك."}
        },
        "quizQuestions": [
            {
                "id": "q1",
                "question": "Why is a strong understanding of core concepts important?",
                "translations": {
                    "en": {"question": "Why is a strong understanding of core concepts important?"},
                    "fr": {"question": "Pourquoi est-il important de bien comprendre les concepts fondamentaux ؟"},
                    "ar": {"question": "لماذا يُعد الفهم الجيد للمفاهيم الأساسية أمرًا مهمًا؟"}
                },
                "answers": [
                    {
                        "id": "a1",
                        "text": "It enables informed decisions and effective solutions",
                        "translations": {
                            "en": {"text": "It enables informed decisions and effective solutions"},
                            "fr": {"text": "Il permet des décisions éclairées et des solutions efficaces"},
                            "ar": {"text": "يساعد على اتخاذ قرارات مدروسة وحلول فعالة"}
                        },
                        "correct": True
                    },
                    {
                        "id": "a2",
                        "text": "It removes the need for collaboration",
                        "translations": {
                            "en": {"text": "It removes the need for collaboration"},
                            "fr": {"text": "Il supprime le besoin de collaboration"},
                            "ar": {"text": "يلغي الحاجة إلى التعاون"}
                        },
                        "correct": False
                    },
                    {
                        "id": "a3",
                        "text": "It only matters for academic exams",
                        "translations": {
                            "en": {"text": "It only matters for academic exams"},
                            "fr": {"text": "Il n’est utile que pour les examens académiques"},
                            "ar": {"text": "يقتصر على الامتحانات الأكاديمية فقط"}
                        },
                        "correct": False
                    }
                ]
            }
        ]
    }

    return {
        "title": title_en,
        "description": desc_en,
        "goals": goals_en,
        "defaultLanguage": "en",
        "translations": {
            "en": {"title": title_en, "description": desc_en, "goals": goals_en},
            "fr": {"title": title_fr, "description": desc_fr, "goals": goals_fr},
            "ar": {"title": title_ar, "description": desc_ar, "goals": goals_ar}
        },
        "domain": domain.lower(),
        "country": country,
        "textContent": [lesson_1, lesson_2, quiz],
        "updatedAt": datetime.utcnow()
    }


# ---------------- Run Migration ----------------
count = 0
for course in courses.find():
    courses.update_one(
        {"_id": course["_id"]},
        {"$set": migrate_course(course)}
    )
    count += 1

print(f"Migration completed. {count} courses updated with long-form lessons.")
