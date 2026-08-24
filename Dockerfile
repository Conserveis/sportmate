# ============================================================================
#  STAGE 1: Build — คอมไพล์ + แพ็กเป็น .jar ด้วย Maven
#  ใช้ image ที่มี Maven + JDK ติดตั้งมาให้แล้ว (ไม่ต้องลงเองบนเครื่อง)
# ============================================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# คัดลอก pom.xml ก่อนเพื่อให้ Docker cache ชั้น dependency ไว้
# (ถ้าโค้ดเปลี่ยนแต่ pom.xml ไม่เปลี่ยน จะไม่ต้องโหลด dependency ใหม่ทุกครั้ง)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# คัดลอกซอร์สโค้ดที่เหลือ แล้ว build เป็น .jar (ข้าม test เพื่อความเร็ว)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================================
#  STAGE 2: Runtime — เอาแค่ .jar ที่ build เสร็จมารันจริง
#  ใช้ JRE เท่านั้น (ไม่มี Maven/JDK) เพื่อให้ image เล็กและปลอดภัยกว่า
# ============================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# สร้าง user แยกจาก root เพื่อความปลอดภัย (ไม่รันแอปด้วยสิทธิ์ root)
RUN groupadd -r sportmate && useradd -r -g sportmate sportmate

COPY --from=build /app/target/*.jar app.jar
RUN chown sportmate:sportmate app.jar

USER sportmate

EXPOSE 8080

# healthcheck ให้ Docker รู้ว่า container พร้อมรับ request จริงหรือยัง
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/login || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
