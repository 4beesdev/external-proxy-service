# ==== CONFIGURE =====
# Use a openjdk 17
FROM  ubuntu:latest
# Set the working directory to /opt/photodun inside the container
WORKDIR /opt/external-proxy-service 
# Copy app files
COPY . .
# RUN mkdir 
# ==== BUILD =====

# Install needed packages
RUN apt update && apt -y install openjdk-17-jre maven git openssh-client

# Build the app
#RUN cd /opt/core && mvn clean install
RUN cd /opt/external-proxy-service && mvn clean install -DskipTests
# ==== RUN =======

# Expose the port
#EXPOSE 8443
# Start the app
#CMD [ "java", "-jar", "/opt/core/target/rori-core-1.0-SNAPSHOT.jar" ]
CMD [ "java", "-Xms1g", "-Xmx2g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/opt/external-proxy-service/heap_dump", "-jar", "/opt/external-proxy-service/target/external-proxy-service-0.0.1-SNAPSHOT.jar" ]
