# Deploy SportMate ขึ้น Google Compute Engine (VM)

## ภาพรวม

Compute Engine คือ **เครื่องคอมพิวเตอร์เสมือน (VM)** บนคลาวด์ ที่เราลง Docker เองได้
ข้อดีคือ **`docker compose` ที่ใช้ในเครื่องตัวเอง ใช้บน VM ได้เหมือนกันเป๊ะ**

```
ในเครื่องเรา:  [app container] ──► [mysql container]      (บน Docker)
บน VM:        [app container] ──► [mysql container]      (บน Docker เหมือนกัน)
                     ▲
                     └── ผู้ใช้เข้าผ่าน External IP ของ VM
```

ต่างจาก Cloud Run ตรงที่ **รัน MySQL container บน VM ได้เลย** ไม่ต้องแยกไปใช้ Cloud SQL
เพราะ VM มีดิสก์ถาวรของตัวเอง ข้อมูลไม่หายเวลารีสตาร์ต

> ⚠️ **ค่าใช้จ่าย:** VM คิดเงินตามเวลาที่**เปิดเครื่องไว้** (ประมาณ $13-25/เดือน แล้วแต่ขนาด)
> บัญชีใหม่มักได้เครดิตฟรี $300 / 90 วัน
> **ปิด VM เมื่อไม่ใช้** จะไม่เสียค่า CPU/RAM (เสียแค่ค่าดิสก์นิดหน่อย) — ดูหัวข้อท้ายไฟล์

---

## เตรียมตัว

1. มี Google Cloud Project + **เปิด Billing** แล้ว
2. ใช้ **Cloud Shell** (ง่ายสุด — กดไอคอน `>_` มุมขวาบนใน Console) หรือติดตั้ง [gcloud CLI](https://cloud.google.com/sdk/docs/install)

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

เปิด API:
```bash
gcloud services enable compute.googleapis.com
```

---

## ขั้นที่ 1: สร้าง VM

```bash
gcloud compute instances create sportmate-vm \
  --zone=asia-southeast1-b \
  --machine-type=e2-medium \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=30GB \
  --tags=http-server
```

**เลือกขนาดเครื่อง (`--machine-type`) ยังไง:**

| ขนาด | RAM | ราคาโดยประมาณ | เหมาะกับ |
|---|---|---|---|
| `e2-micro` | 1GB | ~$7/เดือน | ❌ **ไม่พอ** — build Maven จะ OOM |
| `e2-small` | 2GB | ~$13/เดือน | พอไหว แต่ตอน build อาจตึง (ต้องเพิ่ม swap) |
| `e2-medium` | 4GB | ~$25/เดือน | ✅ **แนะนำ** — build ลื่น ไม่มีปัญหา |

> เหตุผล: การ build Spring Boot ด้วย Maven ใน Docker กิน RAM ค่อนข้างเยอะ
> ถ้า RAM น้อยจะเจอ error `Killed` หรือ build ค้างกลางคัน
> (ถ้าจำเป็นต้องใช้เครื่องเล็ก ดูหัวข้อ "เครื่อง RAM น้อย" ท้ายไฟล์)

**`asia-southeast1-b`** = สิงคโปร์ ใกล้ไทยที่สุด เว็บจะเร็ว

---

## ขั้นที่ 2: เปิด Firewall ให้เข้าเว็บได้

โดย default Google Cloud **ปิดทุกพอร์ต** ต้องเปิดพอร์ต 80 (HTTP) เอง:

```bash
gcloud compute firewall-rules create allow-http-80 \
  --allow=tcp:80 \
  --target-tags=http-server \
  --description="อนุญาตให้เข้าเว็บ SportMate"
```

> `--target-tags=http-server` ทำงานคู่กับ `--tags=http-server` ตอนสร้าง VM
> คือกฎนี้จะมีผลเฉพาะกับ VM ที่ติด tag นี้เท่านั้น

**อย่าเปิดพอร์ต 3306 (MySQL) สู่อินเทอร์เน็ตเด็ดขาด** — ไฟล์ `docker-compose.prod.yml`
ตั้งค่าไว้ให้ MySQL เข้าถึงได้เฉพาะภายในเครื่องอยู่แล้ว

---

## ขั้นที่ 3: เข้า VM แล้วติดตั้ง Docker

เข้า VM:
```bash
gcloud compute ssh sportmate-vm --zone=asia-southeast1-b
```

พอเข้าไปแล้ว (prompt จะเปลี่ยนเป็นชื่อ VM) ติดตั้ง Docker:
```bash
# อัปเดตระบบ
sudo apt update && sudo apt upgrade -y

# ติดตั้ง Docker (สคริปต์ทางการ)
curl -fsSL https://get.docker.com | sudo sh

# อนุญาตให้ใช้ docker โดยไม่ต้องพิมพ์ sudo ทุกครั้ง
sudo usermod -aG docker $USER
```

**สำคัญ:** ออกแล้วเข้าใหม่เพื่อให้สิทธิ์มีผล
```bash
exit
```
```bash
gcloud compute ssh sportmate-vm --zone=asia-southeast1-b
```

ตรวจว่าติดตั้งสำเร็จ:
```bash
docker --version
docker compose version
```

---

## ขั้นที่ 4: อัปโหลดโค้ดขึ้น VM

เลือกวิธีใดวิธีหนึ่ง

### วิธี A — คัดลอกจากเครื่องตัวเอง (ง่ายสุด)

**ออกจาก VM ก่อน** (`exit`) แล้วรันบนเครื่องตัวเอง/Cloud Shell:
```bash
gcloud compute scp --recurse ./sportmate sportmate-vm:~/ --zone=asia-southeast1-b
```

> รันคำสั่งนี้จากโฟลเดอร์**ที่อยู่เหนือ** `sportmate` (โฟลเดอร์แม่)

### วิธี B — ผ่าน GitHub (สะดวกกว่าเวลาแก้โค้ดบ่อย)

ถ้าโปรเจกต์อยู่บน GitHub แล้ว ให้ SSH เข้า VM แล้ว:
```bash
sudo apt install -y git
git clone https://github.com/USERNAME/sportmate.git
```

ทีหลังเวลาแก้โค้ด แค่ `git pull` แล้ว build ใหม่

---

## ขั้นที่ 5: ตั้งรหัสผ่านฐานข้อมูล

SSH เข้า VM แล้วเข้าโฟลเดอร์โปรเจกต์:
```bash
cd ~/sportmate
```

สร้างไฟล์ `.env` จากตัวอย่าง:
```bash
cp .env.example .env
nano .env
```

แก้บรรทัด `DB_PASSWORD=` ให้เป็นรหัสผ่านที่เดายาก แล้วกด
`Ctrl+O` → `Enter` (บันทึก) → `Ctrl+X` (ออก)

> **อย่าใช้ `rootpass` ที่เป็นค่า default** เพราะเครื่องนี้อยู่บนอินเทอร์เน็ตจริงแล้ว

---

## ขั้นที่ 6: รันเว็บ

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

ครั้งแรกจะใช้เวลาราว 3-8 นาที (ดาวน์โหลด image + build Maven)

ตรวจสถานะ:
```bash
docker compose -f docker-compose.prod.yml ps
```
ต้องเห็นทั้ง `sportmate-mysql` และ `sportmate-app` เป็น `Up ... (healthy)`

ดู log ถ้ามีปัญหา:
```bash
docker compose -f docker-compose.prod.yml logs app --tail=100
```

---

## ขั้นที่ 7: เปิดเว็บ

หา External IP ของ VM:
```bash
gcloud compute instances describe sportmate-vm --zone=asia-southeast1-b \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)"
```

เปิดเบราว์เซอร์ไปที่ `http://EXTERNAL_IP` (ไม่ต้องใส่ `:8080` เพราะ prod ใช้พอร์ต 80 แล้ว)

> ต้องเป็น `http://` ไม่ใช่ `https://` เพราะยังไม่ได้ติดตั้ง SSL

---

## ทำให้ IP ไม่เปลี่ยน (แนะนำ)

โดย default External IP จะเปลี่ยนทุกครั้งที่ปิด-เปิด VM ถ้าอยากให้คงที่:

```bash
# จอง IP
gcloud compute addresses create sportmate-ip --region=asia-southeast1

# ดู IP ที่จองได้
gcloud compute addresses describe sportmate-ip --region=asia-southeast1 --format="get(address)"
```

แล้วผูกเข้ากับ VM ผ่าน Console: Compute Engine → VM instances → กด VM → EDIT →
Network interfaces → External IPv4 address → เลือก `sportmate-ip` → SAVE

> Static IP ที่**ผูกกับ VM ที่เปิดอยู่** ไม่เสียเงินเพิ่ม
> แต่ถ้าจองไว้เฉยๆ ไม่ได้ใช้ จะโดนคิดเงิน (~$7/เดือน) — ถ้าเลิกใช้ให้ลบด้วย

---

## เวลาแก้โค้ดแล้วอยาก deploy ใหม่

SSH เข้า VM แล้ว:
```bash
cd ~/sportmate
git pull                                                   # ถ้าใช้ GitHub
docker compose -f docker-compose.prod.yml up -d --build
```

ข้อมูลในฐานข้อมูล**ไม่หาย** เพราะเก็บใน Docker volume

> ถ้าแก้ไฟล์ใน `db/init/` (เช่น เพิ่มสถานที่) ต้อง `down -v` ซึ่ง**ข้อมูลจะหายหมด**:
> `docker compose -f docker-compose.prod.yml down -v`

---

## รีสตาร์ต VM แล้วเว็บกลับมาเองไหม?

**กลับมาเองครับ** เพราะ:
- `docker-compose.prod.yml` ตั้ง `restart: always` ไว้
- Docker service ถูกตั้งให้เริ่มพร้อมเครื่องอัตโนมัติตอนติดตั้ง

ทดสอบได้: `sudo reboot` แล้วรอสัก 1-2 นาที เว็บจะกลับมาเอง

---

## แก้ปัญหาที่พบบ่อย

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| เปิดเว็บไม่ได้ ค้างโหลด | ยังไม่ได้สร้าง firewall rule (ขั้นที่ 2) หรือ VM ไม่ได้ติด tag `http-server` |
| `permission denied ... docker.sock` | ยังไม่ได้ออกแล้ว SSH เข้าใหม่หลัง `usermod -aG docker` |
| build ค้าง / ขึ้น `Killed` | RAM ไม่พอ → เปลี่ยนเป็น `e2-medium` หรือเพิ่ม swap (ดูหัวข้อล่าง) |
| `container is unhealthy` | ดู log: `docker compose -f docker-compose.prod.yml logs app --tail=100` |
| ต่อ MySQL ไม่ได้ | เช็คว่าสร้างไฟล์ `.env` แล้วและมี `DB_PASSWORD=` อยู่จริง |

---

## เครื่อง RAM น้อย (e2-small) — เพิ่ม swap

ถ้าจำเป็นต้องใช้เครื่องเล็กเพื่อประหยัด ให้เพิ่ม swap ก่อน build:
```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h        # ตรวจว่ามี swap แล้ว
```

---

## ⚠️ จัดการค่าใช้จ่าย

**ปิด VM เมื่อไม่ใช้** (ไม่เสียค่า CPU/RAM แต่ข้อมูลยังอยู่):
```bash
gcloud compute instances stop sportmate-vm --zone=asia-southeast1-b
```

**เปิดใหม่:**
```bash
gcloud compute instances start sportmate-vm --zone=asia-southeast1-b
```

**ลบทิ้งถาวรเมื่อจบงาน:**
```bash
gcloud compute instances delete sportmate-vm --zone=asia-southeast1-b
gcloud compute addresses delete sportmate-ip --region=asia-southeast1   # ถ้าจอง static IP ไว้
```

ตั้ง **Budget Alert** ด้วย: Console → Billing → Budgets & alerts → สร้างงบ เช่น $10
แล้วให้แจ้งเตือนอีเมลเมื่อใช้ถึง 50%/90%

---

## สรุปลำดับขั้น

```
1. สร้าง VM (e2-medium, Ubuntu 22.04)
2. เปิด firewall พอร์ต 80
3. SSH เข้า VM → ติดตั้ง Docker → ออกแล้วเข้าใหม่
4. อัปโหลดโค้ด (scp หรือ git clone)
5. cp .env.example .env → ตั้งรหัสผ่าน
6. docker compose -f docker-compose.prod.yml up -d --build
7. เปิด http://EXTERNAL_IP
```

การรันในเครื่องแบบเดิม (`docker compose up -d --build`) ยังใช้ได้เหมือนเดิมทุกอย่าง
