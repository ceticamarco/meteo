# BUILD STAGE
FROM amazoncorretto:23 AS build
LABEL author="Marco Cetica"

# Prepare working environment
WORKDIR /workspace/app
COPY pom.xml .
COPY src src

# Install latest version of Maven
RUN yum update -y
RUN yum install -y gzip tar
RUN curl -O https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
RUN tar xf *.tar.gz

# Build the jar file
RUN apache-maven-3.9.9/bin/mvn package -Dmaven.test.skip

# Run stage
FROM amazoncorretto:23 AS run

# Configure working environment
VOLUME /tmp
ARG BUILD=/workspace/app/target

# Copy jar file
COPY --from=build ${BUILD}/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]