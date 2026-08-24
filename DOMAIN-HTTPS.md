# ขอ Domain Name + เปิด HTTPS ให้ SportMate

จากเดิมที่เข้าเว็บด้วย `http://34.126.xxx.xxx` (จำยาก + ขึ้น "Not secure")
เป้าหมายคือเปลี่ยนเป็น `https://sportmate.com` (จำง่าย + มีกุญแจล็อก 🔒)

มี 3 ขั้นตอนใหญ่:
```
1. หาโดเมน  →  2. ตั้ง DNS ชี้มาที่ VM  →  3. เปิด HTTPS
```

---

## ขั้นที่ 0: ต้องมี Static IP ก่อน (สำคัญมาก)

**ถ้ายังไม่ได้จอง Static IP ต้องทำก่อน** เพราะ External IP ของ VM จะเปลี่ยนทุกครั้งที่ปิด-เปิดเครื่อง
ถ้า IP เปลี่ยน โดเมนจะชี้ผิดที่ทันที เว็บจะเข้าไม่ได้

```bash
# จอง IP
gcloud compute addresses create sportmate-ip --region=asia-southeast1

# ดู IP ที่จองได้ (จดไว้ ต้องใช้ในขั้นที่ 2)
gcloud compute addresses describe sportmate-ip --region=asia-southeast1 --format="get(address)"
```

จากนั้นผูกเข้ากับ VM ผ่าน Console:
Compute Engine → VM instances → กดชื่อ VM → **EDIT** → Network interfaces →
External IPv4 address → เลือก `sportmate-ip` → **SAVE**

---

## ขั้นที่ 1: หาโดเมน

### ตัวเลือก A — ซื้อโดเมนจริง

| ผู้ให้บริการ | ราคาโดยประมาณ (.com/ปี) | หมายเหตุ |
|---|---|---|
| **Cloudflare Registrar** | ~$10 | ขายราคาทุน ไม่บวกกำไร + DNS ฟรีเร็วมาก **แนะนำ** |
| **Namecheap** | ~$10-15 | ปีแรกมักลดราคา ใช้ง่าย |
| **Google Domains** | — | ถูกขายให้ Squarespace แล้ว |
| **Cloud Domains (GCP)** | ~$12 | จัดการในหน้า Console เดียวกับ VM |
| **ผู้ให้บริการไทย** (Z.com, THAI Data Cloud) | ~400-600฿ | จ่ายเงินบาทได้ มีซัพพอร์ตภาษาไทย |

**โดเมนราคาถูกลง** ถ้าเลือกนามสกุลอื่น เช่น `.xyz`, `.site`, `.online` (บางที่ปีแรก ~$1-3)

> ถ้าเป็นนักศึกษา ลองดู **GitHub Student Developer Pack** — มักแจกโดเมน `.me` ฟรี 1 ปี จาก Namecheap

### ตัวเลือก B — โดเมนฟรี (เหมาะกับงานเรียน/ทดลอง)

**DuckDNS** (`sportmate.duckdns.org`) — ฟรี ถาวร ใช้ง่ายที่สุด
1. เข้า https://www.duckdns.org → login ด้วย Google/GitHub
2. พิมพ์ชื่อที่ต้องการ เช่น `sportmate` → กด **add domain**
3. ใส่ Static IP ของ VM ในช่อง `current ip` → กด **update ip**
4. ได้โดเมน `sportmate.duckdns.org` ใช้ได้ทันที (ข้ามขั้นที่ 2 ไปเลย)

**nip.io** — ไม่ต้องสมัครอะไรเลย ใช้ IP ประกอบเป็นชื่อโดเมน
เช่น IP `34.126.100.50` → ใช้ `34.126.100.50.nip.io` ได้ทันที
(เหมาะกับทดสอบ HTTPS เร็วๆ แต่ชื่อยังจำยากอยู่ดี)

> ⚠️ **เลี่ยง Freenom** (`.tk`, `.ml`, `.ga`) — เคยแจกโดเมนฟรีแต่ปัจจุบันมีปัญหาและหยุดให้บริการจดใหม่แล้ว

---

## ขั้นที่ 2: ตั้ง DNS ให้ชี้มาที่ VM

*(ถ้าใช้ DuckDNS หรือ nip.io ข้ามขั้นนี้ได้เลย)*

หลังซื้อโดเมน ให้เข้าหน้าจัดการ DNS ของผู้ให้บริการ แล้วเพิ่ม **A record**:

| Type | Name (Host) | Value | TTL |
|---|---|---|---|
| `A` | `@` | `34.126.xxx.xxx` (Static IP ของ VM) | 3600 |
| `A` | `www` | `34.126.xxx.xxx` (IP เดียวกัน) | 3600 |

**อธิบายค่าต่างๆ:**
- **A record** = บันทึกที่บอกว่า "โดเมนนี้ ชี้ไปที่ IP นี้"
- **`@`** หมายถึงโดเมนหลัก (`sportmate.com`)
- **`www`** ทำให้ `www.sportmate.com` ใช้ได้ด้วย
- ถ้าอยากใช้แค่ซับโดเมน เช่น `app.sportmate.com` ให้ใส่ Name เป็น `app`

### ตรวจว่า DNS ทำงานแล้วหรือยัง

DNS ใช้เวลากระจายข้อมูล (propagate) ตั้งแต่ **5 นาที ถึง 48 ชั่วโมง** (ปกติ ~15-30 นาที)

ตรวจด้วยคำสั่ง (บน Cloud Shell หรือเครื่องตัวเอง):
```bash
nslookup sportmate.com
```
ถ้าขึ้น IP ตรงกับ VM = ใช้ได้แล้ว

หรือเช็คผ่านเว็บ https://dnschecker.org (ดูได้ว่ากระจายไปทั่วโลกแค่ไหน)

> **อย่าข้ามไปขั้นที่ 3 จนกว่า `nslookup` จะขึ้น IP ถูกต้อง**
> เพราะ Let's Encrypt จะตรวจโดเมนก่อนออกใบรับรอง ถ้า DNS ยังไม่พร้อมจะขอไม่สำเร็จ
> และถ้าขอถี่เกินไปจะโดนจำกัด (rate limit) ต้องรอเป็นชั่วโมง

---

## ขั้นที่ 3: เปิด HTTPS

ผมเตรียมไฟล์ให้แล้ว 2 ไฟล์:
- **`docker-compose.https.yml`** — เพิ่ม container `caddy` เข้ามาเป็นด่านหน้า
- **`Caddyfile`** — ตั้งค่า reverse proxy

**Caddy** จะขอใบรับรอง SSL จาก Let's Encrypt **ให้อัตโนมัติ** และต่ออายุเองทุก 90 วัน
ไม่ต้องรันคำสั่ง certbot หรือตั้ง cron เอง

### 3.1 เปิด firewall พอร์ต 443 เพิ่ม

เดิมเปิดแค่พอร์ต 80 ต้องเพิ่ม 443 (HTTPS):
```bash
gcloud compute firewall-rules create allow-https-443 \
  --allow=tcp:443 \
  --target-tags=http-server \
  --description="อนุญาต HTTPS"
```

> พอร์ต 80 ยังต้องเปิดไว้ด้วย เพราะ Let's Encrypt ใช้ยืนยันตัวตน และใช้ redirect ไป https

### 3.2 ตั้งค่าไฟล์ .env บน VM

SSH เข้า VM:
```bash
gcloud compute ssh sportmate-vm --zone=asia-southeast1-b
cd ~/sportmate
nano .env
```

เพิ่ม/แก้ 2 บรรทัดนี้ (นอกเหนือจาก `DB_PASSWORD` ที่มีอยู่แล้ว):
```
DOMAIN=sportmate.com
ACME_EMAIL=you@example.com
```

- `DOMAIN` = โดเมนของคุณ **ไม่ต้องใส่** `http://` หรือ `https://` นำหน้า
- `ACME_EMAIL` = อีเมลจริงของคุณ (Let's Encrypt ใช้แจ้งเตือนถ้าใบรับรองใกล้หมดอายุผิดปกติ)

บันทึก: `Ctrl+O` → `Enter` → `Ctrl+X`

### 3.3 สลับมารันแบบ HTTPS

```bash
# หยุดตัวเดิมก่อน (ข้อมูลใน DB ไม่หาย)
docker compose -f docker-compose.prod.yml down

# รันแบบมี Caddy + HTTPS
docker compose -f docker-compose.https.yml up -d --build
```

ดู log ของ Caddy เพื่อดูว่าขอใบรับรองสำเร็จไหม:
```bash
docker compose -f docker-compose.https.yml logs caddy --tail=50
```

ถ้าสำเร็จจะเห็นข้อความประมาณ `certificate obtained successfully`

### 3.4 เปิดเว็บ

```
https://sportmate.com
```

ต้องเห็น **กุญแจล็อก 🔒** ในแถบ address bar

ลองพิมพ์ `http://sportmate.com` (ไม่มี s) — Caddy จะ redirect ไป `https://` ให้อัตโนมัติ

---

## แก้ปัญหาที่พบบ่อย

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| Caddy log ขึ้น `no such host` | DNS ยังไม่ propagate → รอแล้วเช็ค `nslookup` ใหม่ |
| `connection refused` ตอนขอใบรับรอง | ยังไม่เปิด firewall พอร์ต 80/443 |
| `too many certificates already issued` | ขอใบรับรองถี่เกินไป (Let's Encrypt จำกัด 5 ครั้ง/สัปดาห์ ต่อโดเมน) → **รอ 1 สัปดาห์** หรือใช้ซับโดเมนอื่นชั่วคราว |
| เข้าเว็บได้แต่ไม่มีกุญแจล็อก | ยังเข้าผ่าน IP อยู่ ต้องเข้าด้วยชื่อโดเมน |
| `502 Bad Gateway` | container `app` ยังไม่พร้อม → `docker compose -f docker-compose.https.yml logs app` |

> **เคล็ดลับกัน rate limit:** ตอนทดสอบครั้งแรก ถ้าไม่แน่ใจว่าตั้งค่าถูก
> ให้เพิ่มบรรทัด `acme_ca https://acme-staging-v02.api.letsencrypt.org/directory`
> ในบล็อกปีกกาบนสุดของ `Caddyfile` เพื่อใช้เซิร์ฟเวอร์ทดสอบ (ขอได้ไม่จำกัด แต่เบราว์เซอร์จะเตือนว่าใบรับรองไม่น่าเชื่อถือ)
> พอมั่นใจแล้วค่อยลบบรรทัดนั้นออกแล้วรันใหม่

---

## สรุปลำดับขั้น

```
0. จอง Static IP + ผูกกับ VM
1. ซื้อโดเมน (หรือใช้ DuckDNS ฟรี)
2. ตั้ง A record ชี้มาที่ Static IP  →  รอ DNS propagate  →  เช็คด้วย nslookup
3. เปิด firewall 443
   → ตั้ง DOMAIN + ACME_EMAIL ใน .env
   → docker compose -f docker-compose.https.yml up -d --build
4. เปิด https://โดเมนของคุณ  🔒
```

## ค่าใช้จ่ายรวม

| รายการ | ราคา |
|---|---|
| โดเมน | ~$10/ปี (หรือ **ฟรี** ถ้าใช้ DuckDNS) |
| ใบรับรอง SSL (Let's Encrypt) | **ฟรี** |
| Static IP (ผูกกับ VM ที่เปิดอยู่) | **ฟรี** |
| Static IP (จองไว้เฉยๆ ไม่ได้ใช้) | ~$7/เดือน ← อย่าลืมลบถ้าเลิกใช้ |
