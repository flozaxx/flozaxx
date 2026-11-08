"""Site içeriğini (Firestore site/content) depo ile eşitler.

  python tools/content.py pull   -> content/content.json dosyasına canlı içeriği yazar
  python tools/content.py push   -> (yalnızca GitHub Actions'ta) dosyayı Firestore'a yükler

pull, belgenin o anki güncellenme zamanını "basedOn" olarak saklar. push bu zamanı ön koşul
olarak gönderir: arada panelden kayıt yapılmışsa Firestore yazmayı reddeder ve panel
değişikliği ezilmez.
"""
import datetime
import json
import os
import sys
import urllib.error
import urllib.request

DOC_URL = "https://firestore.googleapis.com/v1/projects/flozaxx-a8521/databases/(default)/documents/site/content"
FILE = os.path.join(os.path.dirname(__file__), "..", "content", "content.json")


def pull():
    with urllib.request.urlopen(DOC_URL) as res:
        doc = json.load(res)
    data = {"basedOn": doc["updateTime"], "content": json.loads(doc["fields"]["json"]["stringValue"])}
    with open(FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"content/content.json güncellendi (basedOn {doc['updateTime']})")


def push(token):
    with open(FILE, encoding="utf-8") as f:
        data = json.load(f)
    now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")
    body = {"fields": {
        "json": {"stringValue": json.dumps(data["content"], ensure_ascii=False)},
        "updatedAt": {"timestampValue": now},
    }}
    url = f"{DOC_URL}?currentDocument.updateTime={data['basedOn']}"
    req = urllib.request.Request(url, data=json.dumps(body).encode(), method="PATCH",
                                 headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as res:
            print(f"Yüklendi: {json.load(res)['updateTime']}")
    except urllib.error.HTTPError as err:
        detail = err.read().decode()
        if err.code in (400, 409) and "FAILED_PRECONDITION" in detail:
            sys.exit("Panelden yeni bir kayıt yapılmış; içerik yeniden çekilmeli (pull) ve değişiklik yeniden uygulanmalı.")
        sys.exit(f"Yüklenemedi ({err.code}): {detail}")


if __name__ == "__main__":
    if sys.argv[1:] == ["pull"]:
        pull()
    elif sys.argv[1:] == ["push"]:
        push(os.environ["FIRESTORE_TOKEN"])
    else:
        sys.exit("kullanım: content.py pull | push")
