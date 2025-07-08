# Use an official Node.js runtime as a parent image
FROM node:18

# Set working directory
WORKDIR /app

# Copy backend files
COPY pom.xml mvnw ./
COPY src ./src

# Install Maven and Java for backend build
RUN apt-get update && apt-get install -y maven openjdk-17-jdk

# Build backend tests
RUN mvn test -DskipTests=false

# Copy frontend files
COPY edbm-meeting-room-management-frontend/package.json edbm-meeting-room-management-frontend/package-lock.json ./
COPY edbm-meeting-room-management-frontend/__tests__ ./edbm-meeting-room-management-frontend/__tests__
COPY edbm-meeting-room-management-frontend/components ./edbm-meeting-room-management-frontend/components
COPY edbm-meeting-room-management-frontend/pages ./edbm-meeting-room-management-frontend/pages
COPY edbm-meeting-room-management-frontend/babel.config.js ./edbm-meeting-room-management-frontend/jest.config.js ./

# Install frontend dependencies
RUN cd edbm-meeting-room-management-frontend && npm install

# Run frontend tests
RUN cd edbm-meeting-room-management-frontend && npm test

CMD ["bash"]
