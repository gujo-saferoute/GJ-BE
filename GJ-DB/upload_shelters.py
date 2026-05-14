"""
강원도 민방위 대피소 CSV → Firebase Firestore 자동 업로드 스크립트

사용법:
1. pip install firebase-admin
2. Firebase 콘솔 > 프로젝트 설정 > 서비스 계정 > 새 비공개 키 생성 > serviceAccountKey.json 다운로드
3. 이 스크립트와 같은 폴더에 serviceAccountKey.json, CSV 파일 넣기
4. python upload_shelters.py
"""

import csv
import firebase_admin
from firebase_admin import credentials, firestore
import time

# ===== 설정 =====
SERVICE_ACCOUNT_KEY = "safe-route-31943-firebase-adminsdk-fbsvc-ab0a27ad1c.json"
CSV_FILE = "민방위대피시설_강원특별자치도.csv"
COLLECTION_NAME = "shelters"
BATCH_SIZE = 400  # Firestore 배치 최대 500개, 여유있게 400
# =================

def load_csv(filepath):
    shelters = []
    with open(filepath, 'r', encoding='cp949') as f:
        reader = csv.DictReader(f)
        for row in reader:
            # 사용중인 것만
            if row['운영상태'] != '사용중':
                continue

            # 위경도 확인
            try:
                lat = float(row['위도(EPSG4326)'])
                lng = float(row['경도(EPSG4326)'])
            except (ValueError, KeyError):
                print(f"위경도 없음, 건너뜀: {row['시설명']}")
                continue

            # 위경도 유효성 검사 (강원도 범위)
            if not (37.0 <= lat <= 38.6 and 127.0 <= lng <= 129.3):
                print(f"좌표 범위 벗어남, 건너뜀: {row['시설명']} ({lat}, {lng})")
                continue

            shelter = {
                "markerId": row['관리번호'].replace('-', '_'),
                "name": row['시설명'],
                "address": row['도로명전체주소'] or row['소재지전체주소'],
                "description": f"민방위 대피소 ({row['시설위치(지상/지하)']})",
                "latitude": lat,
                "longitude": lng,
                "capacity": int(row['최대수용인원']) if row['최대수용인원'].isdigit() else 0,
                "facilityType": row['시설구분'],
                "location": row['시설위치(지상/지하)'],
                "barrierFree": False,  # 나중에 장애인편의시설 API로 업데이트
                "disasterTypes": ["CIVIL_DEFENSE"],
            }
            shelters.append(shelter)

    return shelters


def upload_to_firestore(shelters):
    cred = credentials.Certificate(SERVICE_ACCOUNT_KEY)
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    print(f"총 {len(shelters)}개 업로드 시작...")

    # 기존 데이터 삭제 여부 확인
    answer = input("기존 shelters 컬렉션 데이터를 먼저 삭제할까요? (y/n): ")
    if answer.lower() == 'y':
        print("기존 데이터 삭제 중...")
        docs = db.collection(COLLECTION_NAME).stream()
        batch = db.batch()
        count = 0
        for doc in docs:
            batch.delete(doc.reference)
            count += 1
            if count % BATCH_SIZE == 0:
                batch.commit()
                batch = db.batch()
                print(f"  {count}개 삭제됨")
        batch.commit()
        print(f"  총 {count}개 삭제 완료")

    # 배치로 업로드
    batch = db.batch()
    count = 0
    success = 0

    for shelter in shelters:
        ref = db.collection(COLLECTION_NAME).document()
        batch.set(ref, shelter)
        count += 1
        success += 1

        if count % BATCH_SIZE == 0:
            batch.commit()
            batch = db.batch()
            print(f"  {success}/{len(shelters)} 업로드됨...")
            time.sleep(0.5)  # API 부하 방지

    # 남은 것 업로드
    if count % BATCH_SIZE != 0:
        batch.commit()

    print(f"\n완료! 총 {success}개 대피소 Firestore 업로드 성공")


if __name__ == "__main__":
    print("=== 강원도 민방위 대피소 Firestore 업로드 ===")
    shelters = load_csv(CSV_FILE)
    print(f"CSV에서 {len(shelters)}개 유효 데이터 읽음")
    upload_to_firestore(shelters)
