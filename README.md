# SportMate

เว็บแอปนัดชวนเล่นกีฬา (Spring Boot + Thymeleaf + MySQL/Docker) 

## ฟีเจอร์ (4 หน้า)

| หน้า | คำอธิบาย |
|------|----------|
| **Post** (`/posts`) | โพสต์หาเพื่อนเล่นกีฬาที่คนอื่นจัด |
| **Tournament** (`/tournaments`) | เฉพาะสมาชิกรายเดือนเท่านั้นที่สร้างได้และไม่หายแม้หมดเวลา |
| **จัดเก็บกิจกรรม** (`/archive`) | โพสต์ที่เราเข้าร่วมแล้วและหมดเวลาเข้าร่วม จะย้ายมาที่นี่ + รีวิวผู้จัด |
| **โปรไฟล์** (`/profile`) | ประวัติการเข้าร่วม, กีฬาที่สนใจ, ประวัติการจัดกิจกรรม, สมัครสมาชิก |

ฟีเจอร์อื่น: สมัคร/เข้าสู่ระบบ, เข้าร่วม/ยกเลิกกิจกรรม, **คอมเมนต์ใต้โพสต์ที่เข้าร่วมแล้ว**,
โพสต์สาธารณะ/ส่วนตัว (รออนุมัติ), โควตาโพสต์ 3 ครั้ง/สัปดาห์สำหรับผู้ใช้ทั่วไป, ให้คะแนนผู้จัด

## Technologies
- Java 17+ , Spring Boot 3.3, Spring Data JPA, Thymeleaf
- MySQL 8.0 รันบน Docker (localhost:3306)
- `sportmate_schema.sql`

---

> **Deploy:**
> - **[DEPLOY-GCE.md](DEPLOY-GCE.md)** — Google Compute Engine
> - **[DOMAIN-HTTPS.md](DOMAIN-HTTPS.md)** — ขอโดเมน + เปิด HTTPS 
> - **[DEPLOY.md](DEPLOY.md)** — Google Cloud Run + Cloud SQL

---

### โหมด A: Dockerize ทั้งคู่ (production-style)

```bash
docker compose up -d --build
```

คำสั่งเดียวนี้จะ:
1. ดึง MySQL image มารัน + สร้าง schema/seed ข้อมูลอัตโนมัติ (เหมือนเดิม)
2. **Build image ของตัวแอป Java จาก Dockerfile** (คอมไพล์ด้วย Maven ข้างในเอง ไม่ต้องมี Maven บนเครื่อง)
3. รอ MySQL พร้อม (`healthy`) แล้วค่อยเริ่มแอป
4. เปิดแอปที่ `http://localhost:8080`

เช็คสถานะ:
```bash
docker compose ps
```
ต้องเห็นทั้ง `sportmate-mysql` และ `sportmate-app` เป็น `Up (healthy)`

ดู log ตอนแอปกำลังเริ่ม (มีประโยชน์ตอน debug):
```bash
docker compose logs -f app
```

**แก้โค้ดแล้วอยาก build ใหม่:**
```bash
docker compose up -d --build app
```

**ปิดทั้งหมด:**
```bash
docker compose down          # ข้อมูลใน MySQL ยังอยู่ (เก็บใน volume)
docker compose down -v       # ลบข้อมูลทั้งหมด เริ่มใหม่หมด
```

---

### โหมด B: Dev — MySQL ใน Docker, แอปรันตรงจากเครื่อง

**ขั้นตอนที่ 1 — เปิดเฉพาะ MySQL**
```bash
docker compose up -d mysql
```

**ขั้นตอนที่ 2 — รัน Spring Boot จากเครื่อง**
```bash
mvn spring-boot:run
```
หรือเปิดใน IntelliJ IDEA / VS Code แล้ว Run คลาส `SportmateApplication.java`

โหมดนี้แอปจะอ่านค่าเชื่อมต่อจาก `application.properties` (`localhost:3306`) ตามปกติ

---

### เปิดเว็บ (ทั้ง 2 โหมด)
```
http://localhost:8080
```

### บัญชีทดลอง (สร้างอัตโนมัติครั้งแรก)
| ผู้ใช้ | รหัสผ่าน | ประเภท |
|--------|----------|--------|
| `demo`   | `password123` | ผู้ใช้ทั่วไป |
| `member` | `password123` | สมาชิกรายเดือน (สร้างทัวร์นาเมนต์ได้) |

หรือกด **สมัครสมาชิก** เพื่อสร้างบัญชีใหม่

---

## การตั้งค่าเชื่อมต่อ DB

มี 2 ที่ที่กำหนดค่าเชื่อมต่อฐานข้อมูล ใช้คนละโหมดกัน:

**`src/main/resources/application.properties`** — ค่า default ใช้ตอนรันแบบ**โหมด B** (แอปอยู่นอก Docker)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sportmate
spring.datasource.username=root
spring.datasource.password=rootpass
```

**`docker-compose.yml` (service `app`)** — ใช้ตอนรันแบบ**โหมด A** (แอปอยู่ใน Docker ด้วย) จะ**ทับค่าด้านบน**ผ่าน environment variable
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/sportmate...
```
สังเกตว่าใช้ `mysql` (ชื่อ service) แทน `localhost` เพราะเมื่อ 2 container อยู่ใน Docker network เดียวกัน (`sportmate-net`) แต่ละ container จะมองเห็นกันผ่าน**ชื่อ service** ไม่ใช่ `localhost` — `localhost` ในมุมมองของ container `app` หมายถึงตัว container นั้นเอง ไม่ใช่ container `mysql`

ทั้งสอง credential (root/rootpass) ต้องตรงกับที่ตั้งใน `docker-compose.yml` service `mysql`

---

## ระบบที่เพิ่มเข้ามา (OTP / Payment / Notification / Search)

- **ยืนยัน OTP ตอนสมัคร (UC-1/FR02)** — สมัครแล้วต้องกรอก OTP 6 หลักก่อนเข้าใช้งาน
  (ไม่มี mail server จริง จึง**แสดง OTP บนหน้าจอ**ในโหมดทดสอบ + พิมพ์ลง log ด้วย)
- **ชำระเงินสมัครสมาชิก (UC-5)** — มีหน้ากรอกบัตรเครดิต/เดบิต ตรวจรูปแบบ (16 หลัก, MM/YY, CVV 3 หลัก)
  แล้วอัปเกรดเป็น Member + ออกใบเสร็จ (จำลอง ไม่ตัดเงินจริง)
- **การแจ้งเตือน (UC-4)** — กระดิ่งใน nav แสดงจำนวนที่ยังไม่อ่าน + หน้า `/notifications`
  แจ้งเมื่อ: มีคนเข้าร่วม/ยกเลิกกิจกรรมของคุณ, มีกิจกรรมใหม่ในกีฬาที่คุณสนใจ, กิจกรรมที่เข้าร่วมถูกยกเลิก
- **ค้นหา/กรองกิจกรรม (FR14)** — แถบค้นหาบนหน้า Post และ Tournament กรองด้วย
  ประเภทกีฬา / สถานที่ / วันที่ / เวลา แล้วแสดงเฉพาะรายการที่ตรงเงื่อนไข

## หมายเหตุการทำให้ง่ายขึ้น (Simplifications)
บางส่วนยังทำแบบจำลองเพราะต้องต่อบริการภายนอกจริง:
- **การส่ง OTP / อีเมล** — ไม่ได้ต่อ mail server จริง จึงแสดง OTP บนหน้าจอแทนการส่งอีเมล
- **Payment Gateway** — ตรวจรูปแบบบัตรแบบจำลอง ไม่ได้ตัดเงินจริงผ่านผู้ให้บริการ
- **การแจ้งเตือนแบบ Real-time** — เป็นแบบบันทึกลงฐานข้อมูลแล้วแสดงตอนรีเฟรชหน้า (ยังไม่ใช้ websocket/push จริง)
- **ล็อกบัญชี/Session timeout (UC-2)** — คอลัมน์มีในฐานข้อมูล แต่ยังไม่ผูก logic เต็มรูปแบบ

## แก้ปัญหาที่พบบ่อย (Troubleshooting)

**อาการ: `docker compose ps` แสดง `sportmate-app` เป็น `Up Less than a second` วนซ้ำๆ ไม่ยอมเป็น `healthy`**
→ แอป crash แล้ว restart วนลูป ต้องดู log หาสาเหตุจริง:
```bash
docker compose logs app --tail=100
```
อ่านหาบรรทัดที่มีคำว่า `Caused by:` (มักอยู่ใกล้ท้ายสุด) นั่นคือสาเหตุจริง

**อาการ: แก้โค้ด/config แล้ว แต่รันใหม่เหมือนเดิมไม่เปลี่ยน**
→ เพราะ `docker compose up -d` ใช้ image เดิมที่เคย build ไว้ ต้องบังคับ build ใหม่ทุกครั้งที่แก้โค้ด:
```bash
docker compose down
docker compose up -d --build
```
ถ้ายังไม่หายให้ build แบบไม่ใช้ cache เลย:
```bash
docker compose build --no-cache app
docker compose up -d
```

**อาการ: อยากเริ่มทุกอย่างใหม่หมด (ล้างข้อมูลฐานข้อมูลด้วย)**
```bash
docker compose down -v
docker compose up -d --build
```
---

## โครงสร้างโปรเจกต์
```
sportmate/
├── Dockerfile                    # build image ของตัวแอป Java (multi-stage)
├── .dockerignore
├── docker-compose.yml            # รันทั้ง mysql + app
├── pom.xml
├── db/init/                      # SQL รันตอน docker เริ่มครั้งแรก
│   ├── 01_schema.sql             # schema เดิม (มี trigger AvgScore)
│   └── 02_seed_location.sql
└── src/main/
    ├── java/com/sportmate/
    │   ├── entity/               # User, Post, Event, Chat, Review, ...
    │   ├── repository/           # Spring Data JPA
    │   ├── service/              # business logic
    │   ├── controller/           # routes
    │   └── config/               # auth interceptor, seed data, beans
    └── resources/
        ├── application.properties
        ├── templates/            # Thymeleaf (posts, tournaments, archive, profile, ...)
        └── static/css/style.css
```
