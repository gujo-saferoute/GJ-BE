"""
Firestore evalInfo 중복 제거 스크립트

사용법:
1. serviceAccountKey.json과 같은 폴더에 넣기
2. python clean_eval_info.py
"""

import firebase_admin
from firebase_admin import credentials, firestore
import time

SERVICE_ACCOUNT_KEY = "safe-route-31943-firebase-adminsdk-fbsvc-ab0a27ad1c.json"

def clean_eval_info(eval_info: str) -> str:
    if not eval_info:
        return eval_info
    items = [item.strip() for item in eval_info.split(',')]
    # 중복 제거 (순서 유지)
    seen = []
    for item in items:
        if item and item not in seen:
            seen.append(item)
    return ', '.join(seen)

def main():
    cred = credentials.Certificate(SERVICE_ACCOUNT_KEY)
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    print("Firestore에서 대피소 목록 읽는 중...")
    docs = list(db.collection("shelters").stream())
    print(f"총 {len(docs)}개 처리 시작\n")

    updated = 0
    skipped = 0

    for i, doc in enumerate(docs):
        data = doc.to_dict()
        eval_info = data.get('evalInfo', '')

        if not eval_info:
            skipped += 1
            continue

        cleaned = clean_eval_info(eval_info)

        if cleaned != eval_info:
            db.collection("shelters").document(doc.id).update({'evalInfo': cleaned})
            print(f"[{i+1}] {data.get('name', '')} → {cleaned[:60]}")
            updated += 1
            time.sleep(0.05)
        else:
            skipped += 1

    print(f"\n=== 완료 ===")
    print(f"수정: {updated}개")
    print(f"변경 없음: {skipped}개")

if __name__ == "__main__":
    main()
