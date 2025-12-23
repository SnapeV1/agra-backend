#!/usr/bin/env python3
import argparse
from pymongo import MongoClient

def wrap_en(value):
    if isinstance(value, str):
        return {"en": value}
    return value

def migrate_course(doc):
    changed = False
    text_content = doc.get("textContent") or []
    if not isinstance(text_content, list):
        return None, False

    for tc in text_content:
        if not isinstance(tc, dict):
            continue

        title = tc.get("title")
        if isinstance(title, str):
            tc["title"] = {"en": title}
            changed = True

        content = tc.get("content")
        if isinstance(content, str):
            tc["content"] = {"en": content}
            changed = True

        quiz_questions = tc.get("quizQuestions") or []
        if isinstance(quiz_questions, list):
            for qq in quiz_questions:
                if not isinstance(qq, dict):
                    continue

                question = qq.get("question")
                if isinstance(question, str):
                    qq["question"] = {"en": question}
                    changed = True

                answers = qq.get("answers") or []
                if isinstance(answers, list):
                    for ans in answers:
                        if not isinstance(ans, dict):
                            continue
                        text = ans.get("text")
                        if isinstance(text, str):
                            ans["text"] = {"en": text}
                            changed = True

    return text_content, changed

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--uri", default="mongodb://localhost:27017")
    parser.add_argument("--db", default="agra-platform")
    parser.add_argument("--collection", default="courses")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    client = MongoClient(args.uri)
    col = client[args.db][args.collection]

    updated = 0
    scanned = 0

    for doc in col.find({"textContent": {"$exists": True}}):
        scanned += 1
        new_text_content, changed = migrate_course(doc)
        if changed:
            if args.dry_run:
                print(f"[DRY] would update {doc['_id']}")
            else:
                col.update_one(
                    {"_id": doc["_id"]},    
                    {"$set": {"textContent": new_text_content}}
                )
            updated += 1

    print(f"Scanned: {scanned}, Updated: {updated}")

if __name__ == "__main__":
    main()
