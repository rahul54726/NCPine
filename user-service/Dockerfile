# Stage 1: Build the application INSIDE Docker (Bypasses Windows entirely)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Compiling inside the clean Docker environment
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copying the freshly built JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# 💥 THE ULTIMATE GOD MODE: Bypassing YAML and forcing Atlas Cloud!
ENTRYPOINT ["java", "-Dspring.mongodb.uri=mongodb+srv://rahul23256:HkPpPssSIpJPdgUY@cluster0.qe3y3wr.mongodb.net/user_db?retryWrites=true&w=majority", "-jar", "app.jar"]