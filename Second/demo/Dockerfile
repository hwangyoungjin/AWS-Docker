FROM openjdk:8-jdk-alpine
# application 이름으로 work directory 만들기
WORKDIR application
# jar 파일 변수로 지정
ARG JAR_FILE=build/libs/*.jar
# jar_file을 application.jar 이름으로 copy 하기
COPY ${JAR_FILE} application.jar
ENTRYPOINT ["java","-jar","application.jar"]