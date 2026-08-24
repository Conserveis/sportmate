# Deploy SportMate ขึ้น Google Cloud (Cloud Run + Cloud SQL)

## ภาพรวมสถาปัตยกรรมบนคลาวด์

ตอนรันในเครื่อง เรามี 2 container คุยกันใน Docker network:
```
[app container] ──► [mysql container]
```

บน Google Cloud จะเปลี่ยนเป็น:
```
ผู้ใช้ ──► Cloud Run (แอป Java)  ──Unix socket──►  Cloud SQL (MySQL)
```

**ทำไมต้องแยก:** Cloud Run เป็น *stateless* — container ถูกสร้าง/ลบตลอดเวลาตามจำนวนผู้ใช้
ถ้าเอา MySQL ไปรันข้างในด้วย ข้อมูลจะหายทุกครั้งที่ container รีสตาร์ต
จึงต้องใช้ **Cloud SQL** ซึ่งเป็นฐานข้อมูลที่เก็บข้อมูลถาวรแยกต่างหาก

> ⚠️ **เรื่องค่าใช้จ่าย:** Cloud Run มี free tier ค่อนข้างเยอะ (ถ้าคนใช้น้อยแทบไม่เสียเงิน)
> แต่ **Cloud SQL ไม่มี free tier ถาวร** — เสียเงินตามเวลาที่เปิดเครื่องไว้ (ราว $8-10/เดือน สำหรับ instance เล็กสุด)
> บัญชีใหม่มักได้เครดิตฟรี $300 / 90 วัน ใช้ทดลองได้สบาย
> **ถ้าทำเสร็จแล้วไม่ใช้ อย่าลืมลบ instance ทิ้ง** (ดูหัวข้อสุดท้าย)

---

## เตรียมตัวก่อนเริ่ม

1. มีบัญชี Google Cloud + สร้าง Project แล้ว + **เปิด Billing** (จำเป็น แม้ใช้เครดิตฟรี)
2. ติดตั้ง [gcloud CLI](https://cloud.google.com/sdk/docs/install) หรือใช้ **Cloud Shell** บนเว็บ (ง่ายกว่า ไม่ต้องติดตั้งอะไร — กดไอคอน `>_` มุมขวาบนใน Google Cloud Console)

ตั้งค่าเริ่มต้น (แทน `YOUR_PROJECT_ID` ด้วยของจริง):
```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud config set run/region asia-southeast1     # สิงคโปร์ ใกล้ไทยสุด
```

เปิด API ที่ต้องใช้:
```bash
gcloud services enable run.googleapis.com sqladmin.googleapis.com \
  artifactregistry.googleapis.com cloudbuild.googleapis.com
```

---

## ขั้นที่ 1: สร้างฐานข้อมูล Cloud SQL

สร้าง instance (ใช้เวลาราว 5-10 นาที):
```bash
gcloud sql instances create sportmate-db \
  --database-version=MYSQL_8_0 \
  --tier=db-f1-micro \
  --region=asia-southeast1 \
  --root-password=CHANGE_ME_STRONG_PASSWORD
```

> เปลี่ยน `CHANGE_ME_STRONG_PASSWORD` เป็นรหัสผ่านของคุณเอง และ**จำไว้** ต้องใช้ต่อในขั้นที่ 3

ดู **Instance connection name** (หน้าตาแบบ `project:region:instance`) — ต้องใช้ในขั้นถัดไป:
```bash
gcloud sql instances describe sportmate-db --format="value(connectionName)"
```

---

## ขั้นที่ 2: นำเข้า schema เข้า Cloud SQL

บนเครื่องเรา ไฟล์ SQL ใน `db/init/` ถูกรันอัตโนมัติโดย Docker
แต่บน Cloud SQL **ต้อง import เอง** ครั้งเดียว

ไฟล์ `db/cloudsql/full-schema.sql` คือทั้ง 3 ไฟล์รวมกันไว้ให้แล้ว (ตาราง + seed + Notification)

**วิธีที่แนะนำ — ใช้ Cloud Shell:**

1. อัปโหลดไฟล์ `db/cloudsql/full-schema.sql` เข้า Cloud Shell (เมนู ⋮ → Upload)
2. เชื่อมต่อฐานข้อมูล:
```bash
gcloud sql connect sportmate-db --user=root
```
3. พอเข้า prompt `mysql>` แล้ว สั่ง:
```sql
source full-schema.sql
```
4. ตรวจว่าตารางครบ:
```sql
USE sportmate;
SHOW TABLES;
SELECT * FROM Location;
exit
```

ต้องเห็นตาราง 11 ตาราง (User, Post, Event, Chat, Review, Notification ฯลฯ)

> ทำไมไม่ใช้ `gcloud sql import sql`: ไฟล์ schema มีคำสั่ง `DELIMITER` (สำหรับสร้าง trigger)
> ซึ่งเป็นคำสั่งของ mysql client ไม่ใช่ของ server — วิธี `source` ผ่าน `gcloud sql connect` จึงชัวร์กว่า

---

## ขั้นที่ 3: Deploy แอปขึ้น Cloud Run

Cloud Run สามารถ build จาก source code ให้เลย โดยใช้ `Dockerfile` ที่เรามีอยู่แล้ว

รันคำสั่งนี้ในโฟลเดอร์ `sportmate` (ที่มี `Dockerfile`):

```bash
gcloud run deploy sportmate \
  --source . \
  --region=asia-southeast1 \
  --allow-unauthenticated \
  --add-cloudsql-instances=INSTANCE_CONNECTION_NAME \
  --set-env-vars="SPRING_DATASOURCE_URL=jdbc:mysql:///sportmate?cloudSqlInstance=INSTANCE_CONNECTION_NAME&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false&serverTimezone=Asia/Bangkok&characterEncoding=UTF-8" \
  --set-env-vars="SPRING_DATASOURCE_USERNAME=root" \
  --set-env-vars="SPRING_DATASOURCE_PASSWORD=CHANGE_ME_STRONG_PASSWORD" \
  --memory=1Gi \
  --timeout=300
```

**แทนค่าเหล่านี้ก่อนรัน:**
- `INSTANCE_CONNECTION_NAME` → ค่าที่ได้จากขั้นที่ 1 (มี **2 จุด** ต้องแก้ทั้งคู่)
- `CHANGE_ME_STRONG_PASSWORD` → รหัสผ่านที่ตั้งไว้ตอนสร้าง instance

อธิบายพารามิเตอร์สำคัญ:

| พารามิเตอร์ | ทำอะไร |
|---|---|
| `--source .` | ให้ Cloud Build อ่าน `Dockerfile` แล้ว build image ให้อัตโนมัติ |
| `--allow-unauthenticated` | เปิดให้คนทั่วไปเข้าเว็บได้ (ไม่ต้อง login Google ก่อน) |
| `--add-cloudsql-instances` | ผูก Cloud SQL เข้ากับ container (สร้าง Unix socket ให้) |
| `--memory=1Gi` | Spring Boot ต้องการ RAM พอสมควร ค่า default 512Mi อาจไม่พอ |
| `--timeout=300` | เผื่อเวลา start ครั้งแรก |

เมื่อเสร็จ จะได้ URL แบบ `https://sportmate-xxxxx-as.a.run.app` → เปิดใช้งานได้เลย

---

## ตรวจสอบ / แก้ปัญหา

**ดู log ตอนแอปเริ่มทำงาน:**
```bash
gcloud run services logs read sportmate --region=asia-southeast1 --limit=100
```

| อาการ | สาเหตุที่พบบ่อย |
|---|---|
| `Table 'sportmate.user' doesn't exist` | ยังไม่ได้ import schema (ขั้นที่ 2) หรือ import ไม่สำเร็จ |
| `Access denied for user 'root'` | รหัสผ่านใน `--set-env-vars` ไม่ตรงกับที่ตั้งตอนสร้าง instance |
| `Communications link failure` | `INSTANCE_CONNECTION_NAME` ผิด หรือลืมใส่ `--add-cloudsql-instances` |
| Container ไม่ start ใน timeout | เพิ่ม `--memory=2Gi` |

**Deploy ใหม่หลังแก้โค้ด** — รันคำสั่ง `gcloud run deploy` เดิมซ้ำได้เลย

---

## ⚠️ ลบทิ้งเมื่อเลิกใช้ (กันโดนเรียกเก็บเงิน)

Cloud SQL คิดเงินตามเวลาที่เปิดไว้ แม้ไม่มีคนใช้ **อย่าลืมลบเมื่อไม่ใช้แล้ว**

```bash
# หยุดชั่วคราว (ยังเสียค่า storage แต่ถูกลง)
gcloud sql instances patch sportmate-db --activation-policy=NEVER

# ลบถาวร (ข้อมูลหายหมด)
gcloud sql instances delete sportmate-db
gcloud run services delete sportmate --region=asia-southeast1
```

แนะนำให้ตั้ง **Budget Alert** ไว้ด้วย: Console → Billing → Budgets & alerts → สร้างงบ เช่น $10
แล้วให้แจ้งเตือนทางอีเมลเมื่อใช้ถึง 50%/90%

---

## สรุปลำดับขั้น

```
1. เปิด API + ตั้งค่า project
2. สร้าง Cloud SQL instance          ──► ได้ INSTANCE_CONNECTION_NAME
3. import full-schema.sql เข้า DB    ──► ได้ตารางครบ
4. gcloud run deploy --source .      ──► ได้ URL เว็บ
5. ตั้ง Budget Alert / ลบเมื่อเลิกใช้
```

การรันในเครื่องด้วย `docker compose up -d --build` ยังใช้ได้เหมือนเดิมทุกอย่าง
ไฟล์ที่เพิ่มมาสำหรับ cloud ไม่กระทบการรันแบบเดิม
