FROM openjdk:11.0.15-jre-slim

RUN mkdir -p /opt/cop && \
    echo '#!/bin/sh' > /bin/cop.sh && \
    echo 'exec java $JAVA_OPTS -jar /opt/cop/cop.jar "$@"' >> /bin/cop.sh && \
    chmod +x /bin/cop.sh

RUN apt-get update && apt-get install -y libfreetype6 fontconfig graphviz

COPY cop-app/target/cop.jar /opt/cop/cop.jar

ENV JAVA_OPTS="-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=75 \
               -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M \
               -XX:MinMetaspaceFreeRatio=10 -XX:MaxMetaspaceFreeRatio=100 \
               -XX:MaxDirectMemorySize=64M"

ENTRYPOINT ["java", "-jar", "/opt/cop/cop.jar"]
CMD ["--help"]
